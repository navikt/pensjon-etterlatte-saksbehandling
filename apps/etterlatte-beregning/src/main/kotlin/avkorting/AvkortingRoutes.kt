package no.nav.etterlatte.avkorting

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
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagKildeDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.BrukerTokenInfo

fun Route.avkorting(avkortingService: AvkortingService, behandlingKlient: BehandlingKlient) {
    route("/api/beregning/avkorting/{$BEHANDLINGSID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter avkorting med behandlingId=$it")
                val avkorting = avkortingService.hentAvkorting(it, brukerTokenInfo)
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
                val avkorting = avkortingService.lagreAvkorting(
                    it,
                    brukerTokenInfo,
                    avkortingGrunnlag.fromDto(brukerTokenInfo)
                )
                call.respond(avkorting.toDto())
            }
        }

        post("/med/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Regulere avkorting for behandlingId=$it")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                val avkorting = avkortingService.kopierAvkorting(it, forrigeBehandlingId, brukerTokenInfo)
                call.respond(avkorting.toDto())
            }
        }
    }
}

fun Avkorting.toDto() = AvkortingDto(
    avkortingGrunnlag = avkortingGrunnlag.map { it.toDto() },
    avkortetYtelse = avkortetYtelse.map { it.toDto() }
)

fun AvkortingGrunnlag.toDto() = AvkortingGrunnlagDto(
    id = id,
    fom = periode.fom,
    tom = periode.tom,
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnAar,
    relevanteMaanederInnAar = relevanteMaanederInnAar,
    spesifikasjon = spesifikasjon,
    kilde = AvkortingGrunnlagKildeDto(kilde.tidspunkt.toString(), kilde.ident)
)

fun AvkortetYtelse.toDto() = AvkortetYtelseDto(
    fom = periode.fom.atDay(1),
    tom = periode.tom?.atEndOfMonth(),
    avkortingsbeloep = avkortingsbeloep,
    ytelseEtterAvkorting = ytelseEtterAvkorting
)

fun AvkortingGrunnlagDto.fromDto(brukerTokenInfo: BrukerTokenInfo) = AvkortingGrunnlag(
    id = id,
    periode = Periode(fom = fom, tom = tom),
    aarsinntekt = aarsinntekt,
    fratrekkInnAar = fratrekkInnAar,
    relevanteMaanederInnAar = relevanteMaanederInnAar ?: (12 - fom.monthValue + 1),
    spesifikasjon = spesifikasjon,
    kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
)