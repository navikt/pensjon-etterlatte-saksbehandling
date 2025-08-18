package no.nav.etterlatte

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveNullable
import io.ktor.server.routing.RoutingContext
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.samordning.vedtak.ManglerFoedselsnummerException

suspend inline fun RoutingContext.hentFnrBody(): FoedselsnummerDTO {
    try {
        val fnrNullable = call.receiveNullable<FoedselsnummerDTO>()
        return fnrNullable ?: throw ManglerFoedselsnummerException()
    } catch (_: Exception) {
        throw ManglerFoedselsnummerException()
    }
}
