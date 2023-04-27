package no.nav.etterlatte.beregning

import io.ktor.http.HttpStatusCode
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
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import java.time.YearMonth
import java.util.*

fun Route.avkorting(avkortingService: AvkortingService, behandlingKlient: BehandlingKlient) {
    route("/api/beregning/avkorting") {
        val logger = application.log

        get("/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                val avkorting = avkortingService.hentAvkorting(it)
                when (avkorting) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(avkorting.toDto())
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/grunnlag") {
            withBehandlingId(behandlingKlient) {
                logger.info("Lagre avkortinggrunnlag for behandlingId=$it")
                val avkortingGrunnlag = call.receive<AvkortingGrunnlagDto>()
                val avkorting = avkortingService.lagreAvkortingGrunnlag(it, bruker, avkortingGrunnlag.fromDto())
                call.respond(avkorting.toDto())
            }
        }
    }
}

data class AvkortingDto(
    val behandlingId: UUID,
    val avkortingGrunnlag: List<AvkortingGrunnlagDto>,
    val tidspunktForAvkorting: Tidspunkt
)

data class AvkortingGrunnlagDto(
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val gjeldendeAar: Int,
    val spesifikasjon: String
)

fun Avkorting.toDto() = AvkortingDto(
    behandlingId = behandlingId,
    avkortingGrunnlag = avkortingGrunnlag.map { it.toDto() },
    tidspunktForAvkorting = tidspunktForAvkorting
)

fun AvkortingGrunnlag.toDto() = AvkortingGrunnlagDto(
    fom = periode.fom,
    tom = periode.tom,
    aarsinntekt = aarsinntekt,
    gjeldendeAar = gjeldendeAar,
    spesifikasjon = spesifikasjon
)

fun AvkortingGrunnlagDto.fromDto() = AvkortingGrunnlag(
    periode = Periode(fom = fom, tom = null),
    aarsinntekt = aarsinntekt,
    gjeldendeAar = gjeldendeAar,
    spesifikasjon = spesifikasjon
)