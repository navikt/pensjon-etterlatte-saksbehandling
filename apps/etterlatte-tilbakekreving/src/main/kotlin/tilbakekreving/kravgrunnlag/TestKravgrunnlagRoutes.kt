package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import java.math.BigDecimal
import java.math.BigInteger

fun Route.testKravgrunnlagRoutes(service: KravgrunnlagService) {
    route("kravgrunnlag") {

        get {
            call.respond("Hoho")
        }

        post {
            val xml = call.receive<DetaljertKravgrunnlagDto>()
            service.opprettTilbakekreving(xml)
            call.respond(xml)
        }
    }
}