package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.DetaljertForbehandlingDto
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerOpplysninger
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.FoersteVirkOgOppoerTilSak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class EtteroppgjoerForbehandlingService(
    private val dao: EtteroppgjoerForbehandlingDao,
    private val sakDao: SakLesDao,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val oppgaveService: OppgaveService,
    private val inntektskomponentService: InntektskomponentService,
    private val sigrunKlient: SigrunKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(EtteroppgjoerForbehandlingService::class.java)

    // kjøring for å opprette forbehandling for etteroppgjør
    fun startOpprettForbehandlingKjoering(inntektsaar: Int) {
        logger.info("Opprette forbehandling for etteroppgjør med mottatt skatteoppgjør for $inntektsaar")
        val etteroppgjoerListe =
            etteroppgjoerService.hentEtteroppgjoerForStatus(EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER, inntektsaar)

        for (etteroppgjoer in etteroppgjoerListe) {
            try {
                opprettEtteroppgjoerForbehandling(
                    etteroppgjoer.sakId,
                    etteroppgjoer.inntektsaar,
                    HardkodaSystembruker.etteroppgjoer,
                )
            } catch (e: Exception) {
                logger.error("Kunne ikke opprette forbehandling for sakId=${etteroppgjoer.sakId} grunnen: ${e.message}")
            }
        }
    }

    fun hentPensjonsgivendeInntekt(behandlingId: UUID): PensjonsgivendeInntektFraSkatt? = dao.hentPensjonsgivendeInntekt(behandlingId)

    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    fun lagreForbehandlingKopi(forbehandling: EtteroppgjoerForbehandling): EtteroppgjoerForbehandling {
        val forbehandlingCopy =
            forbehandling.copy(
                id = UUID.randomUUID(),
                relatertForbehandlingId = forbehandling.id,
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(), // ny dato
            )

        dao.lagreForbehandling(forbehandlingCopy)
        dao.kopierAInntekt(forbehandling.id, forbehandlingCopy.id)
        dao.kopierPensjonsgivendeInntekt(forbehandling.id, forbehandlingCopy.id)

        return forbehandlingCopy
    }

    fun hentDetaljertForbehandling(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertForbehandlingDto {
        val forbehandling = hentForbehandling(forbehandlingId)

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatt behandling")

        val request =
            EtteroppgjoerBeregnetAvkortingRequest(
                forbehandling = forbehandlingId,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                sakId = sisteIverksatteBehandling.sak.id,
            )
        logger.info("Henter avkorting for forbehandling: $request")
        val avkorting =
            runBlocking {
                beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                    request,
                    brukerTokenInfo,
                )
            }

        val pensjonsgivendeInntekt = dao.hentPensjonsgivendeInntekt(forbehandlingId)
        val aInntekt = dao.hentAInntekt(forbehandlingId)

        if (pensjonsgivendeInntekt == null || aInntekt == null) {
            throw InternfeilException(
                "Mangler ${if (pensjonsgivendeInntekt == null) "pensjonsgivendeInntekt" else "aInntekt"} for behandlingId=$forbehandlingId",
            )
        }

        // hvis relatertForbehandlingId er satt, vis faktiskInntekt fra forrige ferdigstilte forbehandling
        // slik at saksbehandler kan se hva som ble satt og kan korrigere
        val relevantBehandlingId = forbehandling.relatertForbehandlingId ?: forbehandlingId
        if (relevantBehandlingId != forbehandlingId) {
            logger.info("Henter faktiskInntekt for forbehandling $forbehandlingId fra forbehandling $relevantBehandlingId")
        }

        val faktiskInntekt =
            runBlocking {
                beregningKlient.hentAvkortingFaktiskInntekt(
                    EtteroppgjoerFaktiskInntektRequest(
                        forbehandlingId = relevantBehandlingId,
                    ),
                    brukerTokenInfo,
                )
            }
        val beregnetEtteroppgjoerResultat =
            runBlocking {
                beregningKlient.hentBeregnetEtteroppgjoerResultat(
                    EtteroppgjoerHentBeregnetResultatRequest(
                        forbehandling.aar,
                        relevantBehandlingId,
                        sisteIverksatteBehandling.id,
                    ),
                    brukerTokenInfo,
                )
            }

        return DetaljertForbehandlingDto(
            behandling = forbehandling,
            opplysninger =
                EtteroppgjoerOpplysninger(
                    skatt = pensjonsgivendeInntekt,
                    ainntekt = aInntekt,
                    tidligereAvkorting = avkorting.avkortingMedForventaInntekt,
                ),
            faktiskInntekt = faktiskInntekt,
            avkortingFaktiskInntekt = avkorting.avkortingMedFaktiskInntekt,
            sisteIverksatteBehandling = sisteIverksatteBehandling.id,
            beregnetEtteroppgjoerResultat = beregnetEtteroppgjoerResultat,
        )
    }

    fun lagreBrevreferanse(
        forbehandlingId: UUID,
        brev: Brev,
    ) {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)
        dao.lagreForbehandling(forbehandling.medBrev(brev))
    }

    fun hentEtteroppgjoerForbehandlinger(sakId: SakId): List<EtteroppgjoerForbehandling> = dao.hentForbehandlinger(sakId)

    fun opprettEtteroppgjoerForbehandling(
        sakId: SakId,
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerForbehandlingOgOppgave {
        logger.info("Oppretter forbehandling for etteroppgjør sakId=$sakId, inntektsår=$inntektsaar")
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Kunne ikke hente sak=$sakId")

        kanOppretteForbehandlingForEtteroppgjoer(sak, inntektsaar)

        val pensjonsgivendeInntekt = runBlocking { sigrunKlient.hentPensjonsgivendeInntekt(sak.ident, inntektsaar) }
        val aInntekt = runBlocking { inntektskomponentService.hentInntektFraAInntekt(sak.ident, inntektsaar) }
        val virkOgOpphoer = runBlocking { vedtakKlient.hentFoersteVirkOgOppoerTilSak(sakId, brukerTokenInfo) }
        val innvilgetPeriode = utledInnvilgetPeriode(virkOgOpphoer, inntektsaar)
        val nyForbehandling = EtteroppgjoerForbehandling.opprett(sak, innvilgetPeriode)

        dao.lagreForbehandling(nyForbehandling)
        dao.lagrePensjonsgivendeInntekt(pensjonsgivendeInntekt, nyForbehandling.id)
        dao.lagreAInntekt(aInntekt, nyForbehandling.id)

        etteroppgjoerService.oppdaterStatus(sak.id, inntektsaar, EtteroppgjoerStatus.UNDER_FORBEHANDLING)

        return EtteroppgjoerForbehandlingOgOppgave(
            etteroppgjoerForbehandling = nyForbehandling,
            oppgave =
                oppgaveService.opprettOppgave(
                    referanse = nyForbehandling.id.toString(),
                    sakId = sakId,
                    kilde = OppgaveKilde.BEHANDLING,
                    type = OppgaveType.ETTEROPPGJOER,
                    merknad = null,
                ),
        )
    }

    fun lagreOgBeregnFaktiskInntekt(
        behandlingId: UUID,
        request: BeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        val forbehandling = dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatte")

        val request =
            EtteroppgjoerBeregnFaktiskInntektRequest(
                sakId = forbehandling.sak.id,
                forbehandlingId = forbehandling.id,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                loennsinntekt = request.loennsinntekt,
                naeringsinntekt = request.naeringsinntekt,
                afp = request.afp,
                utland = request.utland,
                spesifikasjon = request.spesifikasjon,
            )

        val beregnetEtteroppgjoerResultat =
            runBlocking { beregningKlient.beregnAvkortingFaktiskInntekt(request, brukerTokenInfo) }

        dao.lagreForbehandling(forbehandling.tilBeregnet())
        return beregnetEtteroppgjoerResultat
    }

    private fun kanOppretteForbehandlingForEtteroppgjoer(
        sak: Sak,
        inntektsaar: Int,
    ) {
        val forbehandlinger = hentEtteroppgjoerForbehandlinger(sak.id)
        if (forbehandlinger.any { it.aar == inntektsaar && !it.erFerdigstilt() }) {
            throw InternfeilException("Kan ikke opprette forbehandling fordi det allerede finnes en forbehandling som ikke er ferdigstilt")
        }

        val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoer(sak.id, inntektsaar)

        if (etteroppgjoer == null) {
            logger.error("Fant ikke etteroppgjør for sak=${sak.id} og inntektsår=$inntektsaar")
            throw InternfeilException("Kan ikke opprette forbehandling fordi etteroppgjør for sak=${sak.id} ikke er opprettet")
        }

        if (sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            logger.error("Kan ikke opprette etteroppgjør forbehandling for sak=${sak.id} med sakType=${sak.sakType}")
            throw InternfeilException("Kan ikke opprette etteroppgjør for sakType=${sak.sakType}")
        }

        if (etteroppgjoer.status != EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} på grunn av feil etteroppgjørstatus=${etteroppgjoer.status}")
            throw InternfeilException(
                "Kan ikke opprette forbehandling på grunn av feil etteroppgjør status=${etteroppgjoer.status}",
            )
        }
    }

    private fun utledInnvilgetPeriode(
        virkOgOpphoer: FoersteVirkOgOppoerTilSak,
        inntektsaar: Int,
    ) = Periode(
        fom =
            if (virkOgOpphoer.foersteVirk.year == inntektsaar) {
                virkOgOpphoer.foersteVirk
            } else {
                YearMonth.of(inntektsaar, Month.JANUARY)
            },
        tom =
            if (virkOgOpphoer.opphoer != null && virkOgOpphoer!!.opphoer!!.year == inntektsaar) {
                virkOgOpphoer.opphoer
            } else {
                YearMonth.of(inntektsaar, Month.DECEMBER)
            },
    )
}

data class EtteroppgjoerForbehandlingOgOppgave(
    val etteroppgjoerForbehandling: EtteroppgjoerForbehandling,
    val oppgave: OppgaveIntern,
)

data class BeregnFaktiskInntektRequest(
    val loennsinntekt: Int,
    val afp: Int,
    val naeringsinntekt: Int,
    val utland: Int,
    val spesifikasjon: String,
)

class FantIkkeForbehandling(
    val behandlingId: UUID,
) : IkkeFunnetException(
        code = "MANGLER_FORBEHANDLING_ETTEROPPGJOER",
        detail = "Fant ikke forbehandling etteroppgjør $behandlingId",
    )
