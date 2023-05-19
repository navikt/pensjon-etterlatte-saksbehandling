package no.nav.etterlatte.beregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Bruker
import java.util.*

fun Route.avkorting(avkortingService: AvkortingService, behandlingKlient: BehandlingKlient) {
    route("/api/beregning/avkorting/{$BEHANDLINGSID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                val avkorting = avkortingService.hentAvkorting(it)
                when (avkorting) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(avkorting.toDto())
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient) {
                logger.info("Lagre avkorting for behandlingId=$it")
                val avkortingGrunnlag = call.receive<AvkortingGrunnlagDto>()
                val avkorting = avkortingService.lagreAvkorting(it, bruker, avkortingGrunnlag.fromDto(bruker))
                call.respond(avkorting.toDto())
            }
        }

        post("/med/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Regulere avkorting for behandlingId=$it")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                val avkorting = avkortingService.regulerAvkorting(it, forrigeBehandlingId, bruker)
                call.respond(avkorting.toDto())
            }
        }
    }
}

private fun ApplicationCall.uuid(param: String) = this.parameters[param]?.let {
    UUID.fromString(it)
} ?: throw NullPointerException(
    "$param er ikke i path params"
)

fun Avkorting.toDto() = AvkortingDto(
    behandlingId = behandlingId,
    avkortingGrunnlag = avkortingGrunnlag.map { it.toDto() },
    avkortetYtelse = avkortetYtelse.map { it.toDto() }
)

fun AvkortingGrunnlag.toDto() = AvkortingGrunnlagDto(
    fom = periode.fom,
    tom = periode.tom,
    aarsinntekt = aarsinntekt,
    spesifikasjon = spesifikasjon,
    kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident)
)

fun AvkortetYtelse.toDto() = AvkortetYtelseDto(
    fom = periode.fom.atDay(1),
    tom = periode.tom?.atEndOfMonth(),
    avkortingsbeloep = avkortingsbeloep,
    ytelseEtterAvkorting = ytelseEtterAvkorting
)

fun AvkortingGrunnlagDto.fromDto(bruker: Bruker) = AvkortingGrunnlag(
    periode = Periode(fom = fom, tom = null),
    aarsinntekt = aarsinntekt,
    spesifikasjon = spesifikasjon,
    kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now()),
    beregnetAvkorting = emptyList()
)