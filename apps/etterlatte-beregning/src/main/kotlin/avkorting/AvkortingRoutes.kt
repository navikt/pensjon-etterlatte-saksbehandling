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
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withBehandlingId

fun Route.avkorting(
    avkortingService: AvkortingService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/avkorting/{$BEHANDLINGID_CALL_PARAMETER}") {
        val logger = routeLogger

        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                when (val avkorting = avkortingService.hentAvkorting(it, brukerTokenInfo)) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(avkorting.toDto())
                }
            }
        }

        get("ferdig") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter ferdig avkorting med behandlingId=$it")
                call.respond(avkortingService.hentFullfoertAvkorting(it, brukerTokenInfo).toDto())
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
                call.respond(avkorting.toDto())
            }
        }

        post("/med/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Regulere avkorting for behandlingId=$it")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                val avkorting = avkortingService.kopierAvkorting(it, forrigeBehandlingId, brukerTokenInfo)
                call.respond(avkorting.toDto())
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

fun Avkorting.toDto() =
    AvkortingDto(
        // TODO erstatt single
        avkortingGrunnlag =
            aarsoppgjoer.single().inntektsavkorting.map {
                it.grunnlag.toDto(aarsoppgjoer.single().forventaInnvilgaMaaneder)
            },
        avkortetYtelse = avkortetYtelseFraVirkningstidspunkt.map { it.toDto() },
        tidligereAvkortetYtelse = avkortetYtelseForrigeVedtak.map { it.toDto() },
    )

fun AvkortingGrunnlag.toDto(forventaInnvilgaMaaneder: Int) =
    AvkortingGrunnlagDto(
        id = id,
        fom = periode.fom,
        tom = periode.tom,
        aarsinntekt = aarsinntekt,
        fratrekkInnAar = fratrekkInnAar,
        inntektUtland = inntektUtland,
        fratrekkInnAarUtland = fratrekkInnAarUtland,
        forventaInnvilgaMaaneder = forventaInnvilgaMaaneder,
        spesifikasjon = spesifikasjon,
        kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident),
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
