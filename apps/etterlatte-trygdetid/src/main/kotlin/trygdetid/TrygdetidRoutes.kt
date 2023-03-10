package no.nav.etterlatte.trygdetid

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

fun Route.trygdetid(trygdetidService: TrygdetidService) {
    route("/api/trygdetid") {
        val logger = application.log

        get {
            logger.info("Henter trygdetid")
            val trygdetid = trygdetidService.hentTrygdetid()
            call.respond(TrygdetidDto.toDto(trygdetid))
        }

        post("/grunnlag") {
            logger.info("Lagre trygdetidgrunnlag")
            val trygdetidDto = call.receive<TrygdetidGrunnlagDto>()
            trygdetidService.lagreTrygdetidGrunnlag(trygdetidDto.fromDto())
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class TrygdetidDto(
    val grunnlag: List<TrygdetidGrunnlagDto>
) {
    companion object {
        fun toDto(trygdetid: Trygdetid): TrygdetidDto {
            return TrygdetidDto(
                grunnlag = trygdetid.grunnlag.map { TrygdetidGrunnlagDto.toDto(it) }
            )
        }
    }
}

data class TrygdetidGrunnlagDto(
    val bosted: String,
    val periodeFra: String,
    val periodeTil: String
) {
    fun fromDto(): TrygdetidGrunnlag {
        return TrygdetidGrunnlag(
            bosted,
            periodeFra,
            periodeTil
        )
    }
    companion object {
        fun toDto(trygdetidGrunnlag: TrygdetidGrunnlag): TrygdetidGrunnlagDto {
            return TrygdetidGrunnlagDto(
                trygdetidGrunnlag.bosted,
                trygdetidGrunnlag.periodeFra,
                trygdetidGrunnlag.periodeTil
            )
        }
    }
}