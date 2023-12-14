package no.nav.etterlatte.behandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.MigreringKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingFactory(
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val gyldighetsproevingService: GyldighetsproevingService,
    private val sakService: SakService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val migreringKlient: MigreringKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /*
     * Brukes av frontend for å kunne opprette sak og behandling for en Gosys-oppgave.
     */
    suspend fun opprettSakOgBehandlingForOppgave(request: NyBehandlingRequest): Behandling {
        val soeker = request.persongalleri.soeker

        val sak = inTransaction { sakService.finnEllerOpprettSak(soeker, request.sakType) }

        val persongalleri =
            when (request.kilde) {
                Vedtaksloesning.PESYS ->
                    request.persongalleri.copy(
                        innsender = Vedtaksloesning.PESYS.name,
                    )
                else -> request.persongalleri
            }
        val behandling =
            inTransaction {
                opprettBehandling(
                    sak.id, persongalleri, request.mottattDato, request.kilde ?: Vedtaksloesning.GJENNY,
                ) ?: throw IllegalStateException("Kunne ikke opprette behandling")
            }.behandling

        val gyldighetsvurdering =
            GyldighetsResultat(
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
                emptyList(),
                Tidspunkt.now().toLocalDatetimeUTC(),
            )

        gyldighetsproevingService.lagreGyldighetsproeving(behandling.id, gyldighetsvurdering)

        val mottattDato = LocalDateTime.parse(request.mottattDato)
        val kilde = Grunnlagsopplysning.Privatperson(soeker, mottattDato.toTidspunkt())

        val opplysninger =
            listOf(
                lagOpplysning(Opplysningstype.SPRAAK, kilde, request.spraak.toJsonNode()),
                lagOpplysning(
                    Opplysningstype.SOEKNAD_MOTTATT_DATO,
                    kilde,
                    SoeknadMottattDato(mottattDato).toJsonNode(),
                ),
            )

        grunnlagService.leggTilNyeOpplysninger(behandling.id, NyeSaksopplysninger(sak.id, opplysninger))

        if (request.kilde == Vedtaksloesning.PESYS) {
            coroutineScope {
                val pesysId = requireNotNull(request.pesysId) { "Manuell migrering må ha pesysid til sak som migreres" }
                migreringKlient.opprettManuellMigrering(behandlingId = behandling.id, pesysId = pesysId)
            }
        }

        return behandling
    }

    fun opprettBehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        prosessType: Prosesstype = Prosesstype.MANUELL,
    ): BehandlingOgOppgave? {
        logger.info("Starter behandling i sak $sakId")

        val sak = requireNotNull(sakService.finnSak(sakId)) { "Fant ingen sak med id=$sakId!" }
        val harBehandlingerForSak =
            behandlingDao.alleBehandlingerISak(sak.id)

        val harIverksattEllerAttestertBehandling =
            harBehandlingerForSak.filter { behandling ->
                BehandlingStatus.iverksattEllerAttestert().find { it == behandling.status } != null
            }

        return if (harIverksattEllerAttestertBehandling.isNotEmpty()) {
            val forrigeBehandling = harIverksattEllerAttestertBehandling.maxBy { it.behandlingOpprettet }
            revurderingService.opprettAutomatiskRevurdering(
                sakId = sakId,
                persongalleri = persongalleri,
                forrigeBehandling = forrigeBehandling,
                mottattDato = mottattDato,
                kilde = kilde,
                revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
            )?.let { BehandlingOgOppgave(it, null) }
        } else {
            val harBehandlingUnderbehandling =
                harBehandlingerForSak.filter { behandling ->
                    BehandlingStatus.underBehandling().find { it == behandling.status } != null
                }
            val behandling =
                opprettFoerstegangsbehandling(harBehandlingUnderbehandling, sak, mottattDato, kilde, prosessType)
                    ?: return null
            grunnlagService.leggInnNyttGrunnlag(behandling, persongalleri)
            val oppgave =
                oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
                    referanse = behandling.id.toString(),
                    sakId = sak.id,
                )
            behandlingHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                behandling.toStatistikkBehandling(persongalleri),
                BehandlingHendelseType.OPPRETTET,
            )
            return BehandlingOgOppgave(behandling, oppgave)
        }
    }

    private fun opprettFoerstegangsbehandling(
        harBehandlingUnderbehandling: List<Behandling>,
        sak: Sak,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        prosessType: Prosesstype,
    ): Behandling? {
        harBehandlingUnderbehandling.forEach {
            behandlingDao.lagreStatus(it.id, BehandlingStatus.AVBRUTT, LocalDateTime.now())
            oppgaveService.avbrytAapneOppgaverForBehandling(it.id.toString())
        }

        return OpprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak.id,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
            kilde = kilde,
            prosesstype = prosessType,
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)?.sjekkEnhet()
        }
    }

    private fun Behandling?.sjekkEnhet() =
        this?.let { behandling ->
            listOf(behandling).filterBehandlingerForEnheter(
                Kontekst.get().AppUser,
            ).firstOrNull()
        }
}

data class BehandlingOgOppgave(val behandling: Behandling, val oppgave: OppgaveIntern?)
