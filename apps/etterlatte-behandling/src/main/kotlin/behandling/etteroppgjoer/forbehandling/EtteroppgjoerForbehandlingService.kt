package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.FoersteVirkOgOppoerTilSak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
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

    fun ferdigstillForbehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Ferdigstiller forbehandling for behandling=$behandlingId")
        val forbehandling = dao.hentForbehandling(behandlingId)

        dao.lagreForbehandling(forbehandling!!.tilFerdigstilt())
        etteroppgjoerService.oppdaterEtteroppgjoerStatus(
            forbehandling.sak.id,
            forbehandling.aar,
            EtteroppgjoerStatus.FERDIGSTILT_FORBEHANDLING,
        )
        oppgaveService.ferdigStillOppgaveUnderBehandling(
            forbehandling.id.toString(),
            OppgaveType.ETTEROPPGJOER,
            brukerTokenInfo,
        )
    }

    fun lagreForbehandling(forbehandling: EtteroppgjoerForbehandling) = dao.lagreForbehandling(forbehandling)

    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        dao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    fun hentDetaljertForbehandling(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertForbehandlingDto {
        val forbehandling = hentForbehandling(forbehandlingId)
        val sisteIverksatteBehandling =
            behandlingService.hentBehandling(forbehandling.sisteIverksatteBehandlingId)
                ?: throw InternfeilException(
                    "Fant ikke relatert behandling=${forbehandling.sisteIverksatteBehandlingId} for forbehandling=$forbehandlingId",
                )

        val avkorting = hentAvkortingForForbehandling(forbehandling, sisteIverksatteBehandling, brukerTokenInfo)

        val pensjonsgivendeInntekt = dao.hentPensjonsgivendeInntekt(forbehandlingId)
        val aInntekt = dao.hentAInntekt(forbehandlingId)

        if (pensjonsgivendeInntekt == null || aInntekt == null) {
            throw InternfeilException(
                "Mangler ${if (pensjonsgivendeInntekt == null) "pensjonsgivendeInntekt" else "aInntekt"} for behandlingId=$forbehandlingId",
            )
        }

        val beregnetEtteroppgjoerResultat =
            runBlocking {
                beregningKlient.hentBeregnetEtteroppgjoerResultat(
                    EtteroppgjoerHentBeregnetResultatRequest(
                        forbehandling.aar,
                        forbehandlingId,
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
            beregnetEtteroppgjoerResultat = beregnetEtteroppgjoerResultat,
            faktiskInntekt = avkorting.avkortingMedFaktiskInntekt?.avkortingGrunnlag?.firstOrNull() as? FaktiskInntektDto,
        )
    }

    fun lagreBrevreferanse(
        forbehandlingId: UUID,
        brev: Brev,
    ) {
        val forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)
        dao.lagreForbehandling(forbehandling.medBrev(brev))
    }

    fun hentPensjonsgivendeInntekt(behandlingId: UUID): PensjonsgivendeInntektFraSkatt? = dao.hentPensjonsgivendeInntekt(behandlingId)

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

        val nyForbehandling = opprettOgLagreNyForbehandling(sak, innvilgetPeriode)

        dao.lagrePensjonsgivendeInntekt(pensjonsgivendeInntekt, nyForbehandling.id)
        dao.lagreAInntekt(aInntekt, nyForbehandling.id)

        etteroppgjoerService.oppdaterEtteroppgjoerStatus(sak.id, inntektsaar, EtteroppgjoerStatus.UNDER_FORBEHANDLING)

        return EtteroppgjoerForbehandlingOgOppgave(
            etteroppgjoerForbehandling = nyForbehandling,
            oppgave =
                oppgaveService.opprettOppgave(
                    referanse = nyForbehandling.id.toString(),
                    sakId = sakId,
                    kilde = OppgaveKilde.BEHANDLING,
                    type = OppgaveType.ETTEROPPGJOER,
                    merknad = "Etteroppgjør for $inntektsaar",
                ),
        )
    }

    fun lagreOgBeregnFaktiskInntekt(
        forbehandlingId: UUID,
        request: BeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        var forbehandling = dao.hentForbehandling(forbehandlingId) ?: throw FantIkkeForbehandling(forbehandlingId)

        // hvis ferdigstilt, ikke overskriv men opprett ny kopi forbehandling
        if (forbehandling.erFerdigstilt()) {
            logger.info("Oppretter ny kopi av forbehandling for behandlingId=$forbehandlingId")
            forbehandling = kopierOgLagreNyForbehandling(forbehandling)
        }

        val beregningRequest =
            EtteroppgjoerBeregnFaktiskInntektRequest(
                sakId = forbehandling.sak.id,
                forbehandlingId = forbehandling.id,
                sisteIverksatteBehandling = forbehandling.sisteIverksatteBehandlingId,
                aar = forbehandling.aar,
                loennsinntekt = request.loennsinntekt,
                naeringsinntekt = request.naeringsinntekt,
                afp = request.afp,
                utlandsinntekt = request.utlandsinntekt,
                spesifikasjon = request.spesifikasjon,
            )

        val beregnetEtteroppgjoerResultat =
            runBlocking { beregningKlient.beregnAvkortingFaktiskInntekt(beregningRequest, brukerTokenInfo) }

        behandlingService.settBeregnetForRevurderingTilForbehandling(forbehandling)

        dao.lagreForbehandling(forbehandling.tilBeregnet())
        return beregnetEtteroppgjoerResultat
    }

    fun lagreInformasjonFraBruker(
        forbehandlingId: UUID,
        harMottattNyInformasjon: JaNei,
        endringErTilUgunstForBruker: JaNei?,
        beskrivelseAvUgunst: String?,
    ) {
        dao.oppdaterInformasjonFraBruker(
            forbehandlingId = forbehandlingId,
            harMottattNyInformasjon = harMottattNyInformasjon,
            endringErTilUgunstForBruker = endringErTilUgunstForBruker?.takeIf { harMottattNyInformasjon == JaNei.JA },
            beskrivelseAvUgunst =
                beskrivelseAvUgunst?.takeIf {
                    harMottattNyInformasjon == JaNei.JA &&
                        endringErTilUgunstForBruker == JaNei.JA
                },
        )
    }

    private fun kanOppretteForbehandlingForEtteroppgjoer(
        sak: Sak,
        inntektsaar: Int,
    ) {
        if (sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} med sakType=${sak.sakType}")
            throw InternfeilException("Kan ikke opprette forbehandling for sakType=${sak.sakType}")
        }

        if (behandlingService.hentSisteIverksatte(sak.id) == null) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id}, sak mangler iverksatt behandling")
            throw InternfeilException(
                "Kan ikke opprette forbehandling, mangler sist iverksatte behandling for sak=${sak.id}",
            )
        }

        val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
        if (etteroppgjoer == null) {
            logger.error("Fant ikke etteroppgjør for sak=${sak.id} og inntektsår=$inntektsaar")
            throw InternfeilException("Kan ikke opprette forbehandling fordi sak=${sak.id} ikke har et etteroppgjør")
        }

        // TODO: Denne sjekken må være strengere når vi får koblet opp mot skatt.
        if (etteroppgjoer.status !in
            listOf(
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
            )
        ) {
            logger.error("Kan ikke opprette forbehandling for sak=${sak.id} på grunn av feil etteroppgjørstatus=${etteroppgjoer.status}")
            throw InternfeilException(
                "Kan ikke opprette forbehandling på grunn av feil etteroppgjør status=${etteroppgjoer.status}",
            )
        }

        val forbehandlinger = hentEtteroppgjoerForbehandlinger(sak.id)
        if (forbehandlinger.any { it.aar == inntektsaar && !it.erFerdigstilt() }) {
            throw InternfeilException(
                "Kan ikke opprette forbehandling fordi det allerede eksisterer en forbehandling som ikke er ferdigstilt",
            )
        }

        // TODO: flere sjekker?
    }

    private fun opprettOgLagreNyForbehandling(
        sak: Sak,
        innvilgetPeriode: Periode,
    ): EtteroppgjoerForbehandling {
        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(sak.id)
        krevIkkeNull(sisteIverksatteBehandling) {
            "Fant ikke sisteIverksatteBehandling for Sak=${sak.id} kan derfor ikke opprette forbehandling"
        }

        return EtteroppgjoerForbehandling.opprett(sak, innvilgetPeriode, sisteIverksatteBehandling.id).also {
            dao.lagreForbehandling(it)
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

    private fun hentAvkortingForForbehandling(
        forbehandling: EtteroppgjoerForbehandling,
        sisteIverksatteBehandling: Behandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting {
        val request =
            EtteroppgjoerBeregnetAvkortingRequest(
                forbehandling = forbehandling.id,
                sisteIverksatteBehandling = sisteIverksatteBehandling.id,
                aar = forbehandling.aar,
                sakId = sisteIverksatteBehandling.sak.id,
            )
        logger.info("Henter avkorting for forbehandling: $request")
        return runBlocking {
            beregningKlient.hentAvkortingForForbehandlingEtteroppgjoer(
                request,
                brukerTokenInfo,
            )
        }
    }

    private fun kopierOgLagreNyForbehandling(forbehandling: EtteroppgjoerForbehandling): EtteroppgjoerForbehandling {
        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(forbehandling.sak.id)
                ?: throw InternfeilException("Fant ikke siste iverksatte")

        val forbehandlingCopy =
            forbehandling.copy(
                id = UUID.randomUUID(),
                status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(), // ny dato
                kopiertFra = forbehandling.id,
                sisteIverksatteBehandlingId = sisteIverksatteBehandling.id,
            )

        dao.lagreForbehandling(forbehandlingCopy)
        dao.kopierAInntekt(forbehandling.id, forbehandlingCopy.id)
        dao.kopierPensjonsgivendeInntekt(forbehandling.id, forbehandlingCopy.id)
        dao.oppdaterRelatertBehandling(forbehandling.id, forbehandlingCopy.id)

        return forbehandlingCopy
    }
}

data class EtteroppgjoerForbehandlingOgOppgave(
    val etteroppgjoerForbehandling: EtteroppgjoerForbehandling,
    val oppgave: OppgaveIntern,
)

data class BeregnFaktiskInntektRequest(
    val loennsinntekt: Int,
    val afp: Int,
    val naeringsinntekt: Int,
    val utlandsinntekt: Int,
    val spesifikasjon: String,
)

data class InformasjonFraBrukerRequest(
    val harMottattNyInformasjon: JaNei,
    val endringErTilUgunstForBruker: JaNei?,
    val beskrivelseAvUgunst: String?,
)

class FantIkkeForbehandling(
    val behandlingId: UUID,
) : IkkeFunnetException(
        code = "MANGLER_FORBEHANDLING_ETTEROPPGJOER",
        detail = "Fant ikke forbehandling etteroppgjør $behandlingId",
    )
