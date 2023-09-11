package no.nav.etterlatte.behandling

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingFactory(
    private val oppgaveService: OppgaveServiceNy,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val gyldighetsproevingService: GyldighetsproevingService,
    private val sakService: SakService,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val featureToggleService: FeatureToggleService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettBehandling(behov: NyBehandlingRequest): Behandling {
        val soeker = behov.persongalleri.soeker

        val sak = sakService.finnEllerOpprettSak(soeker, behov.sakType)

        val behandling = opprettBehandling(
            sak.id,
            behov.persongalleri,
            behov.mottattDato,
            Vedtaksloesning.GJENNY
        )

        if (behandling == null) {
            throw IllegalStateException("Kunne ikke opprette behandling")
        }

        val gyldighetsvurdering = GyldighetsResultat(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf(),
            Tidspunkt.now().toLocalDatetimeUTC()
        )

        gyldighetsproevingService.lagreGyldighetsproeving(behandling.id, gyldighetsvurdering)

        val mottattDato = LocalDateTime.parse(behov.mottattDato)
        val kilde = Grunnlagsopplysning.Privatperson(soeker, mottattDato.toTidspunkt())

        val opplysninger = listOf(
            lagOpplysning(Opplysningstype.SPRAAK, kilde, behov.spraak.toJsonNode()),
            lagOpplysning(Opplysningstype.SOEKNAD_MOTTATT_DATO, kilde, SoeknadMottattDato(mottattDato).toJsonNode())
        )

        grunnlagService.leggTilNyeOpplysninger(sak.id, NyeSaksopplysninger(opplysninger))

        return behandling
    }

    fun opprettBehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Behandling? {
        logger.info("Starter behandling i sak $sakId")
        val sak = inTransaction { sakService.finnSak(sakId) }.let {
            requireNotNull(it) { "Fant ingen sak med id=$sakId!" }
        }
        val harBehandlingerForSak = inTransaction {
            behandlingDao.alleBehandlingerISak(sak.id)
        }

        val harIverksattEllerAttestertBehandling = harBehandlingerForSak.filter { behandling ->
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
                merknad = "Oppdatert søknad",
                revurderingAarsak = RevurderingAarsak.NY_SOEKNAD
            )
        } else {
            val harBehandlingUnderbehandling = harBehandlingerForSak.filter { behandling ->
                BehandlingStatus.underBehandling().find { it == behandling.status } != null
            }
            opprettFoerstegangsbehandling(harBehandlingUnderbehandling, sak, persongalleri, mottattDato, kilde)
        }
    }

    private fun opprettFoerstegangsbehandling(
        harBehandlingUnderbehandling: List<Behandling>,
        sak: Sak,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning
    ): Behandling? {
        return inTransaction {
            harBehandlingUnderbehandling.forEach {
                behandlingDao.lagreStatus(it.id, BehandlingStatus.AVBRUTT, LocalDateTime.now())
                oppgaveService.avbrytAapneOppgaverForBehandling(it.id.toString())
            }

            OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
                persongalleri = persongalleri,
                kilde = kilde,
                merknad = opprettMerknad(sak, persongalleri)
            ).let { opprettBehandling ->
                behandlingDao.opprettBehandling(opprettBehandling)
                hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

                logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

                behandlingDao.hentBehandling(opprettBehandling.id)?.sjekkEnhet()
            }.also { behandling ->
                behandling?.let {
                    grunnlagService.leggInnNyttGrunnlag(it, persongalleri)
                    oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
                        referanse = behandling.id.toString(),
                        sakId = sak.id
                    )
                    behandlingHendelser.sendMeldingForHendelse(it, BehandlingHendelseType.OPPRETTET)
                }
            }
        }
    }

    private fun opprettMerknad(sak: Sak, persongalleri: Persongalleri): String? {
        return if (persongalleri.soesken.isEmpty()) {
            null
        } else if (sak.sakType == SakType.BARNEPENSJON) {
            "${persongalleri.soesken.size} søsken"
        } else if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            val barnUnder20 = persongalleri.soesken.count { Folkeregisteridentifikator.of(it).getAge() < 20 }

            "$barnUnder20 barn u/20år"
        } else {
            null
        }
    }
    private fun Behandling?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}