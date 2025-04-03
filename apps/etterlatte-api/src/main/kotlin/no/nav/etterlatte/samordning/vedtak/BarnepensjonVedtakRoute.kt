package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.ktor.route.dato
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.logger

fun Route.barnepensjonVedtakRoute(
    samordningVedtakService: SamordningVedtakService,
    config: Config,
) {
    route("api/barnepensjon/har-loepende-bp") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper =
                setOf("les-bp-vedtak", "les-bp-samordning-vedtak", config.getString("roller.pensjon-saksbehandler"))
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        get {
            val paaDato = call.dato("paaDato") ?: throw ManglerFomDatoException()
            val fnr = call.fnr
            logger.info("GETTILPOST: sjekker om løpende barnepensjon på gammel løsning")

            val harLoependeBarnepensjonYtelsePaaDato =
                try {
                    samordningVedtakService.harLoependeYtelsePaaDato(
                        dato = paaDato,
                        fnr = haandterUgyldigIdent(fnr),
                        sakType = SakType.BARNEPENSJON,
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(
                mapOf(
                    "barnepensjon" to harLoependeBarnepensjonYtelsePaaDato,
                ),
            )
        }

        post {
            val paaDato = call.dato("paaDato") ?: throw ManglerFomDatoException()
            val fnr = call.receiveNullable<FoedselsnummerDTO>() ?: throw ManglerFoedselsnummerException()

            val harLoependeBarnepensjonYtelsePaaDato =
                try {
                    samordningVedtakService.harLoependeYtelsePaaDato(
                        dato = paaDato,
                        fnr = haandterUgyldigIdent(fnr.foedselsnummer),
                        sakType = SakType.BARNEPENSJON,
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@post call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(
                mapOf(
                    "barnepensjon" to harLoependeBarnepensjonYtelsePaaDato,
                ),
            )
        }

        get("/ping") {
            call.respond(
                getMeta(),
            )
        }
    }
}
