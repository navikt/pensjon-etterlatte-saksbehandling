package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus

/**
 * Brukes kun for lokal testing
 */
fun Route.kravgrunnlagRoutes(service: KravgrunnlagService) {
    route("kravgrunnlag") {
        post {
            val dto = call.receive<DetaljertKravgrunnlagDto>()
            val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(dto)
            service.haandterKravgrunnlag(kravgrunnlag)
            call.respond(dto)
        }
    }
    route("status") {
        post {
            val dto = call.receive<KravOgVedtakstatus>()
            val status = KravgrunnlagMapper.toKravOgVedtakstatus(dto)
            service.haandterKravOgVedtakStatus(status)
            call.respond(dto)
        }
    }
}
