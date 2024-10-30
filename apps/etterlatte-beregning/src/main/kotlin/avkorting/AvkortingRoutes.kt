package no.nav.etterlatte.avkorting

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.AvkortingHarInntektForAarDto
import no.nav.etterlatte.libs.common.beregning.AvkortingOverstyrtInnvilgaMaanederDto
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.avkorting(
    avkortingService: AvkortingService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = routeLogger

    route("/api/beregning/avkorting/har-inntekt-for-aar") {
        post {
            val harInntektForAarDto = call.receive<AvkortingHarInntektForAarDto>()
            logger.info("Henter har inntekt for ${harInntektForAarDto.aar} for sakId=${harInntektForAarDto.sakId}")
            call.respond(avkortingService.hentHarSakInntektForAar(harInntektForAarDto))
        }
    }

    route("/api/beregning/avkorting/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                when (val avkorting = avkortingService.hentAvkorting(it, brukerTokenInfo)) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(avkorting)
                }
            }
        }

        get("ferdig") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter ferdig avkorting med behandlingId=$it")
                call.respond(avkortingService.hentFullfoertAvkorting(it, brukerTokenInfo))
            }
        }

        post {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Lagre avkorting for behandlingId=$it")
                val avkortingGrunnlag = call.receive<AvkortingGrunnlagLagreDto>()
                val avkorting =
                    avkortingService.beregnAvkortingMedNyttGrunnlag(
                        it,
                        brukerTokenInfo,
                        avkortingGrunnlag,
                    )
                call.respond(avkorting)
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
                avkortingService.opprettOppgaveHvisTidligAlderspensjon(it, brukerTokenInfo)
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
}

fun AvkortingGrunnlag.toDto() =
    AvkortingGrunnlagDto(
        id = id,
        fom = periode.fom,
        tom = periode.tom,
        inntektTom = inntektTom,
        fratrekkInnAar = fratrekkInnAar,
        inntektUtlandTom = inntektUtlandTom,
        fratrekkInnAarUtland = fratrekkInnAarUtland,
        innvilgaMaaneder = innvilgaMaaneder,
        spesifikasjon = spesifikasjon,
        kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident),
        overstyrtInnvilgaMaaneder =
            overstyrtInnvilgaMaanederAarsak?.let {
                AvkortingOverstyrtInnvilgaMaanederDto(
                    antall = innvilgaMaaneder,
                    aarsak = it.name,
                    begrunnelse = overstyrtInnvilgaMaanederBegrunnelse ?: "",
                )
            },
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
