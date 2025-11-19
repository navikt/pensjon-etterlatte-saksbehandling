package no.nav.etterlatte.avkorting

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.avkorting.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.avkorting.inntektsjustering.AarligInntektsjusteringService
import no.nav.etterlatte.avkorting.inntektsjustering.MottattInntektsjusteringService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagFlereInntekterDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.beregning.AvkortingOverstyrtInnvilgaMaanederDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.beregning.ForventetInntektDto
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoRequest
import no.nav.etterlatte.libs.common.beregning.MottattInntektsjusteringAvkortigRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth

fun Route.avkorting(
    avkortingService: AvkortingService,
    behandlingKlient: BehandlingKlient,
    tidligAlderspensjonService: AvkortingTidligAlderspensjonService,
    aarligInntektsjusteringService: AarligInntektsjusteringService,
    mottattInntektsjusteringService: MottattInntektsjusteringService,
    etteroppgjoerService: EtteroppgjoerService,
) {
    val logger = LoggerFactory.getLogger("AvkortingRoute")

    route("/api/beregning/avkorting/{$BEHANDLINGID_CALL_PARAMETER}") {
        /*
        Tiltenkt frontend. Henting vil utføre endringer på avkorting og behandling
        (beregning av avkorting, behandlingsstatus, etc).
         */
        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                when (val avkorting = avkortingService.hentOpprettEllerReberegnAvkorting(it, brukerTokenInfo)) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(avkorting)
                }
            }
        }

        get("manglende-inntektsaar") {
            withBehandlingId(behandlingKlient, skrivetilgang = false) { behandlingId ->
                logger.info("Henter manglende påkrevde inntektsår for behandling=$behandlingId")
                val paakrevdeInntekter =
                    avkortingService.manglendeInntektsaar(
                        behandlingId = behandlingId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                call.respond(paakrevdeInntekter)
            }
        }

        /*
        Brukes når behandling er ferdig med avkorting slik at det kan hentes uten at det gjøres nye beregninger og
        endringer på behandlingstatus.
         */
        get("ferdig") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter ferdig avkorting med behandlingId=$it")
                call.respond(avkortingService.hentFullfoertAvkorting(it, brukerTokenInfo))
            }
        }

        post("liste") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                medBody<AvkortingGrunnlagFlereInntekterDto> { avkortingGrunnlag ->
                    logger.info("Lagrer nytt avkortingsgrunnlag med lister for behandlingId=$behandlingId")
                    val avkorting =
                        avkortingService.beregnAvkortingMedNyeGrunnlag(
                            behandlingId = behandlingId,
                            nyeGrunnlag = avkortingGrunnlag.inntekter,
                            brukerTokenInfo = brukerTokenInfo,
                        )
                    call.respond(avkorting)
                }
            }
        }

        post("/med/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Kopierer avkorting for behandlingId=$it")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                val avkorting = avkortingService.kopierAvkorting(it, forrigeBehandlingId, brukerTokenInfo)
                call.respond(avkorting.toDto())
            }
        }

        post("/haandter-tidlig-alderspensjon") {
            withBehandlingId(behandlingKlient) {
                logger.info("Haandterer oppgave hvis tidlig alderspensjon (behandlingId=$it)")
                tidligAlderspensjonService.opprettOppgaveHvisTidligAlderspensjon(it, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        delete {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Sletter avkorting for behandlingId=$it")
                avkortingService.slettAvkorting(it)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/api/beregning/avkorting") {
        post("aarlig-inntektsjustering-sjekk") {
            val request = call.receive<InntektsjusteringAvkortingInfoRequest>()
            logger.info("Henter har inntekt for ${request.aar} for sakId=${request.sakId}")
            val respons = aarligInntektsjusteringService.hentSjekkAarligInntektsjustering(request)
            call.respond(respons)
        }

        post("aarlig-inntektsjustering") {
            val request = call.receive<AarligInntektsjusteringAvkortingRequest>()
            logger.info("Oppretter avkorting nytt år for behandling=${request.nyBehandling}")
            val respons =
                aarligInntektsjusteringService.kopierAarligInntektsjustering(
                    aar = request.aar,
                    behandlingId = request.nyBehandling,
                    forrigeBehandlingId = request.forrigeBehandling,
                    brukerTokenInfo = brukerTokenInfo,
                )
            call.respond(respons.toDto())
        }

        post("mottatt-inntektsjustering") {
            val request = call.receive<MottattInntektsjusteringAvkortigRequest>()
            logger.info("Oppretter avkorting etter mottatt inntektsjustering fra bruker behandling=${request.behandlingId}")
            val respons =
                mottattInntektsjusteringService.opprettAvkortingMedBrukeroppgittInntekt(request, brukerTokenInfo)
            call.respond(respons.toDto())
        }

        route("etteroppgjoer") {
            post("hent") {
                val request = call.receive<EtteroppgjoerBeregnetAvkortingRequest>()
                logger.info(
                    "Henter avkorting for siste iverksatte behandling for etteroppgjør år=${request.aar} id=${request.sisteIverksatteBehandling}",
                )
                val dto =
                    etteroppgjoerService.hentBeregnetAvkorting(
                        forbehandlingId = request.forbehandling,
                        sisteIverksatteBehandlingId = request.sisteIverksatteBehandling,
                        aar = request.aar,
                        sakId = request.sakId,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                call.respond(dto)
            }

            post("beregn-faktisk-inntekt") {
                val request = call.receive<EtteroppgjoerBeregnFaktiskInntektRequest>()
                logger.info("Beregner avkorting med faktisk inntekt for etteroppgjør med forbehandling=${request.forbehandlingId}")
                etteroppgjoerService.beregnAvkortingForbehandling(request, brukerTokenInfo)
                val resultat =
                    etteroppgjoerService.beregnOgLagreEtteroppgjoerResultat(
                        forbehandlingId = request.forbehandlingId,
                        sisteIverksatteBehandlingId = request.sisteIverksatteBehandling,
                        aar = request.aar,
                    )
                call.respond(resultat.toDto())
            }

            post("hent-beregnet-resultat") {
                val request = call.receive<EtteroppgjoerHentBeregnetResultatRequest>()
                val resultat =
                    etteroppgjoerService.hentBeregnetEtteroppgjoerResultat(
                        request.forbehandlingId,
                        request.sisteIverksatteBehandlingId,
                        request.aar,
                    )

                when (resultat) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(resultat.toDto())
                }
            }
        }

        route("behandlinger-med-aarsoppgjoer/{$SAKID_CALL_PARAMETER}") {
            get {
                val behandlinger = avkortingService.hentBehandlingerMedAarsoppgjoerForSak(sakId)

                if (behandlinger.isEmpty()) throw InternfeilException("Fant ingen behandlinger med årsoppgjør for sak $sakId")
                call.respond(behandlinger)
            }
        }
    }
}

data class AvkortingSkalHaInntektNesteAarDTO(
    val skalHaInntektNesteAar: Boolean,
)

fun ForventetInntekt.toDto() =
    ForventetInntektDto(
        id = id,
        fom = periode.fom,
        tom = if (periode.tom == null) YearMonth.of(periode.fom.year, Month.DECEMBER) else periode.tom,
        inntektTom = inntektTom,
        fratrekkInnAar = fratrekkInnAar,
        inntektUtlandTom = inntektUtlandTom,
        fratrekkInnAarUtland = fratrekkInnAarUtland,
        innvilgaMaaneder = innvilgaMaaneder,
        spesifikasjon = spesifikasjon,
        kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident),
        inntektInnvilgetPeriode =
            when (inntektInnvilgetPeriode) {
                is BenyttetInntektInnvilgetPeriode -> inntektInnvilgetPeriode.verdi
                is IngenInntektInnvilgetPeriode -> inntektTom - fratrekkInnAar + inntektUtlandTom - fratrekkInnAarUtland
            },
        overstyrtInnvilgaMaaneder =
            overstyrtInnvilgaMaanederAarsak?.let {
                AvkortingOverstyrtInnvilgaMaanederDto(
                    antall = innvilgaMaaneder,
                    aarsak = it.name,
                    begrunnelse = overstyrtInnvilgaMaanederBegrunnelse ?: "",
                )
            },
    )

fun FaktiskInntekt.toDto() =
    FaktiskInntektDto(
        id = id,
        fom = periode.fom,
        tom = periode.tom,
        loennsinntekt = loennsinntekt,
        naeringsinntekt = naeringsinntekt,
        afp = afp,
        utlandsinntekt = utlandsinntekt,
        innvilgaMaaneder = innvilgaMaaneder,
        kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident),
        inntektInnvilgetPeriode = inntektInnvilgetPeriode.verdi,
        spesifikasjon = spesifikasjon,
    )

fun AvkortetYtelse.toDto() =
    AvkortetYtelseDto(
        id = id,
        fom = periode.fom,
        tom = periode.tom,
        type = type.name,
        ytelseFoerAvkorting = ytelseFoerAvkorting,
        avkortingsbeloep = avkortingsbeloep,
        restanse = restanse?.fordeltRestanse ?: 0,
        ytelseEtterAvkorting = ytelseEtterAvkorting,
        sanksjon = sanksjon,
    )
