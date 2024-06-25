package no.nav.etterlatte.behandling

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.MigreringKlient
import no.nav.etterlatte.behandling.revurdering.AutomatiskRevurderingService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.NyBehandlingRequest
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class BehandlingFactory(
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagServiceImpl,
    private val revurderingService: AutomatiskRevurderingService,
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
    suspend fun opprettSakOgBehandlingForOppgave(
        request: NyBehandlingRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling {
        sikkerLogg.info("Oppretter sak og behandling for: $request")
        logger.info("Oppretter sak og behandling for persongalleri: ${request.persongalleri}, saktype ${request.sakType}")
        val soeker = request.persongalleri.soeker

        val sak = inTransaction { sakService.finnEllerOpprettSakMedGrunnlag(soeker, request.sakType) }

        if (
            sak.enhet != request.enhet &&
            sak.enhet != Enheter.STRENGT_FORTROLIG.enhetNr &&
            sak.enhet != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr
        ) {
            request.enhet?.let {
                if (Enheter.entries.none { enhet -> enhet.enhetNr == it }) {
                    throw UgyldigEnhetException()
                }
                inTransaction {
                    sakService.oppdaterEnhetForSaker(
                        listOf(
                            SakMedEnhet(
                                enhet = it,
                                id = sak.id,
                            ),
                        ),
                    )
                }
            }
        }

        val persongalleri =
            with(request) {
                if (kilde?.foerstOpprettaIPesys() == true) {
                    persongalleri.copy(innsender = kilde!!.name)
                } else if (persongalleri.innsender == null) {
                    persongalleri.copy(innsender = brukerTokenInfo.ident())
                } else {
                    persongalleri
                }
            }

        val behandling =
            inTransaction {
                opprettBehandling(
                    sak.id,
                    persongalleri,
                    request.mottattDato,
                    request.kilde ?: Vedtaksloesning.GJENNY,
                    request = hentDataForOpprettBehandling(sak.id),
                ).also {
                    if (request.kilde == Vedtaksloesning.GJENOPPRETTA) {
                        oppgaveService
                            .hentOppgaverForSak(sak.id)
                            .find { it.type == OppgaveType.GJENOPPRETTING_ALDERSOVERGANG && !it.erAvsluttet() }
                            ?.let {
                                oppgaveService.ferdigstillOppgave(it.id, brukerTokenInfo)
                            }
                    }
                } ?: throw IllegalStateException("Kunne ikke opprette behandling")
            }.also { it.sendMeldingForHendelse() }
                .behandling

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
            mutableListOf(
                lagOpplysning(Opplysningstype.SPRAAK, kilde, request.spraak.toJsonNode()),
                lagOpplysning(
                    Opplysningstype.SOEKNAD_MOTTATT_DATO,
                    kilde,
                    SoeknadMottattDato(mottattDato).toJsonNode(),
                ),
            )
        if (request.kilde?.foerstOpprettaIPesys() == true) {
            opplysninger.add(lagOpplysning(Opplysningstype.FORELDRELOES, kilde, request.foreldreloes.toJsonNode()))
            opplysninger.add(lagOpplysning(Opplysningstype.UFOERE, kilde, request.ufoere.toJsonNode()))
        }

        grunnlagService.leggTilNyeOpplysninger(behandling.id, NyeSaksopplysninger(sak.id, opplysninger))

        if (request.kilde in listOf(Vedtaksloesning.PESYS, Vedtaksloesning.GJENOPPRETTA)) {
            coroutineScope {
                val pesysId = requireNotNull(request.pesysId) { "Manuell migrering må ha pesysid til sak som migreres" }
                migreringKlient.opprettManuellMigrering(behandlingId = behandling.id, pesysId = pesysId, sakId = sak.id)
            }
        }

        return behandling
    }

    fun opprettBehandling(
        sakId: Long,
        persongalleri: Persongalleri,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        request: DataHentetForOpprettBehandling,
        prosessType: Prosesstype = Prosesstype.MANUELL,
    ): BehandlingOgOppgave? {
        logger.info("Starter behandling i sak $sakId")

        return if (request.harIverksattBehandling()) {
            if (kilde == Vedtaksloesning.PESYS || kilde == Vedtaksloesning.GJENOPPRETTA) {
                throw ManuellMigreringHarEksisterendeIverksattBehandling()
            }
            val forrigeBehandling = request.iverkSattebehandlinger().maxBy { it.behandlingOpprettet }
            revurderingService
                .opprettAutomatiskRevurdering(
                    sakId = sakId,
                    persongalleri = persongalleri,
                    forrigeBehandling = forrigeBehandling,
                    mottattDato = mottattDato,
                    kilde = kilde,
                    revurderingAarsak = Revurderingaarsak.NY_SOEKNAD,
                ).oppdater()
                .let { BehandlingOgOppgave(it, null) }
        } else {
            val harBehandlingUnderbehandling =
                request.alleBehandlingerISak.filter { behandling ->
                    BehandlingStatus.underBehandling().find { it == behandling.status } != null
                }
            val behandling =
                opprettFoerstegangsbehandling(
                    harBehandlingUnderbehandling,
                    request.sak,
                    mottattDato,
                    kilde,
                    prosessType,
                )
                    ?: return null
            grunnlagService.leggInnNyttGrunnlag(behandling, persongalleri)
            val oppgave =
                oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
                    referanse = behandling.id.toString(),
                    sakId = request.sak.id,
                    oppgaveKilde =
                        if (kilde == Vedtaksloesning.GJENOPPRETTA) {
                            OppgaveKilde.GJENOPPRETTING
                        } else {
                            OppgaveKilde.BEHANDLING
                        },
                    merknad =
                        when (kilde) {
                            Vedtaksloesning.GJENOPPRETTA -> "Manuell gjenopprettelse av opphørt sak i Pesys"
                            else -> null
                        },
                )
            return BehandlingOgOppgave(behandling, oppgave) {
                behandlingHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                    behandling.toStatistikkBehandling(persongalleri),
                    BehandlingHendelseType.OPPRETTET,
                )
            }
        }
    }

    fun opprettOmgjoeringAvslag(
        sakId: Long,
        saksbehandler: Saksbehandler,
    ) {
        val sak = sakService.finnSak(sakId) ?: throw GenerellIkkeFunnetException()
        val behandlingerISak = behandlingDao.alleBehandlingerISak(sakId)
        val foerstegangsbehandlinger = behandlingerISak.filter { it.type == BehandlingType.FØRSTEGANGSBEHANDLING }
        if (foerstegangsbehandlinger.isEmpty()) {
            throw AvslagOmgjoering.IngenFoerstegangsbehandling()
        }
        val foerstegangsbehandlingerIkkeAvslaattAvbrutt =
            foerstegangsbehandlinger.filter { it.status !in listOf(BehandlingStatus.AVBRUTT, BehandlingStatus.AVSLAG) }
        if (foerstegangsbehandlingerIkkeAvslaattAvbrutt.isNotEmpty()) {
            throw AvslagOmgjoering.FoerstegangsbehandlingFeilStatus(
                sakId,
                foerstegangsbehandlingerIkkeAvslaattAvbrutt.first().id,
            )
        }
        if (behandlingerISak.any { it.status.aapenBehandling() }) {
            throw AvslagOmgjoering.HarAapenBehandling()
        }

        val foerstegangsbehandlingViOmgjoerer =
            foerstegangsbehandlingerIkkeAvslaattAvbrutt.maxBy { it.behandlingOpprettet }
        // TODO: skal vi kopiere med noe annet her?
        val behandling =
            checkNotNull(
                opprettFoerstegangsbehandling(
                    harBehandlingUnderbehandling = listOf(),
                    sak = sak,
                    mottattDato = foerstegangsbehandlingViOmgjoerer.mottattDato().toString(),
                    kilde = Vedtaksloesning.GJENNY,
                    prosessType = Prosesstype.MANUELL,
                ),
            ) {
                "Behandlingen vi akkurat opprettet fins ikke :("
            }
        val oppgaveForFoerstegangsbehandling =
            checkNotNull(oppgaveService.hentOppgaveUnderBehandling(behandling.id.toString())) {
                "Behandlingen vi akkurat opprettet har ikke noen oppgave :("
            }
        oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, saksbehandler.ident)
    }

    internal fun hentDataForOpprettBehandling(sakId: Long): DataHentetForOpprettBehandling {
        val sak = requireNotNull(sakService.finnSak(sakId)) { "Fant ingen sak med id=$sakId!" }
        val harBehandlingerForSak =
            behandlingDao.alleBehandlingerISak(sak.id)

        return DataHentetForOpprettBehandling(
            sak = sak,
            alleBehandlingerISak = harBehandlingerForSak,
        )
    }

    fun finnGjeldendeEnhet(
        persongalleri: Persongalleri,
        sakType: SakType,
    ) = sakService.finnGjeldendeEnhet(persongalleri.soeker, sakType)

    private fun opprettFoerstegangsbehandling(
        harBehandlingUnderbehandling: List<Behandling>,
        sak: Sak,
        mottattDato: String?,
        kilde: Vedtaksloesning,
        prosessType: Prosesstype,
    ): Behandling? {
        harBehandlingUnderbehandling.forEach {
            behandlingDao.lagreStatus(it.id, BehandlingStatus.AVBRUTT, LocalDateTime.now())
            oppgaveService.avbrytAapneOppgaverMedReferanse(it.id.toString())
        }

        return OpprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak.id,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
            kilde = kilde,
            prosesstype = prosessType,
            sendeBrev = true,
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)
        }
    }
}

data class BehandlingOgOppgave(
    val behandling: Behandling,
    val oppgave: OppgaveIntern?,
    val sendMeldingForHendelse: () -> Unit = {},
)

class ManuellMigreringHarEksisterendeIverksattBehandling :
    UgyldigForespoerselException(
        code = "MANUELL_MIGRERING_EKSISTERENDE_IVERKSATT",
        detail = "Det eksisterer allerede en sak med en iverksatt behandling for angitt søker",
    )

class UgyldigEnhetException :
    UgyldigForespoerselException(
        code = "UGYLDIG-ENHET",
        detail = "Enhet brukt i form er matcher ingen gyldig enhet",
    )

sealed class AvslagOmgjoering {
    class IngenFoerstegangsbehandling :
        UgyldigForespoerselException(
            "INGEN_FOERSTEGANGSBEHANDLING",
            "Fant ingen førstegangsbehandling i saken der førstegangsbehandling skulle omgjøres",
        )

    class FoerstegangsbehandlingFeilStatus(
        sakId: Long,
        behandlingId: UUID,
    ) : UgyldigForespoerselException(
            "FOERSTEGANGSBEHANDLING_UGYLDIG_STATUS",
            "Kan ikke omgjøre førstegangsbehandling i sak $sakId, siden $behandlingId er en " +
                "førstegangsbehandling i saken som ikke er avslått eller avbrutt",
        )

    class HarAapenBehandling :
        UgyldigForespoerselException(
            "HAR_AAPEN_BEHANDLING",
            "For å omgjøre en førstegangsbehandling må alle andre behandlinger i saken være lukket.",
        )
}

fun Vedtaksloesning.foerstOpprettaIPesys() = this == Vedtaksloesning.PESYS || this == Vedtaksloesning.GJENOPPRETTA

data class DataHentetForOpprettBehandling(
    val sak: Sak,
    val alleBehandlingerISak: List<Behandling>,
) {
    fun harIverksattBehandling() =
        alleBehandlingerISak.any { behandling ->
            BehandlingStatus.IVERKSATT == behandling.status
        }

    fun iverkSattebehandlinger() = alleBehandlingerISak.filter { BehandlingStatus.IVERKSATT == it.status }
}
