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
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Bruker
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
                val avkorting = avkortingService.lagreAvkortingGrunnlag(it, bruker, avkortingGrunnlag.fromDto(bruker))
                call.respond(avkorting.toDto())
            }
        }
    }
}

data class AvkortingDto(
    val behandlingId: UUID,
    val avkortingGrunnlag: List<AvkortingGrunnlagDto>
)

data class AvkortingGrunnlagDto(
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val gjeldendeAar: Int,
    val spesifikasjon: String,
    val kilde: AvkortingGrunnlagKildeDto?
)

data class AvkortingGrunnlagKildeDto(
    val tidspunkt: String,
    val ident: String
)

fun Avkorting.toDto() = AvkortingDto(
    behandlingId = behandlingId,
    avkortingGrunnlag = avkortingGrunnlag.map { it.toDto() }
)

fun AvkortingGrunnlag.toDto() = AvkortingGrunnlagDto(
    fom = periode.fom,
    tom = periode.tom,
    aarsinntekt = aarsinntekt,
    gjeldendeAar = gjeldendeAar,
    spesifikasjon = spesifikasjon,
    kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident)
)

fun AvkortingGrunnlagDto.fromDto(bruker: Bruker) = AvkortingGrunnlag(
    periode = Periode(fom = fom, tom = null),
    aarsinntekt = aarsinntekt,
    gjeldendeAar = gjeldendeAar,
    spesifikasjon = spesifikasjon,
    kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now()),
    beregnetAvkorting = emptyList()
)