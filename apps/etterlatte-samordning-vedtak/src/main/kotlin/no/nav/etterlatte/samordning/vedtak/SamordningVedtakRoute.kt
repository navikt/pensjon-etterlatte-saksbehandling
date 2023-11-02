package no.nav.etterlatte.samordning.vedtak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.MaskinportenScopeAuthorizationPlugin
import no.nav.etterlatte.libs.ktor.hentTokenClaims
import java.time.LocalDate

fun Route.samordningVedtakRoute(samordningVedtakService: SamordningVedtakService) {
    route("api/vedtak") {
        install(MaskinportenScopeAuthorizationPlugin) {
            scopes = setOf("nav:etterlatteytelser:vedtaksinformasjon.read")
        }

        get("{vedtakId}") {
            val vedtakId = requireNotNull(call.parameters["vedtakId"]).toLong()

            val tpnummer =
                call.request.headers["tpnr"]
                    ?: throw ManglerTpNrException()

            val samordningVedtakDto =
                try {
                    samordningVedtakService.hentVedtak(
                        vedtakId = vedtakId,
                        MaskinportenTpContext(
                            tpnr = Tjenestepensjonnummer(tpnummer),
                            organisasjonsnr = call.orgNummer,
                        ),
                    )
                } catch (e: VedtakFeilSakstypeException) {
                    call.respond(HttpStatusCode.Unauthorized, "Ikke tilgang til sakstype")
                } catch (e: TjenestepensjonManglendeTilgangException) {
                    call.respond(HttpStatusCode.Unauthorized, e.message)
                } catch (e: TjenestepensjonUgyldigForesporselException) {
                    call.respond(HttpStatusCode.BadRequest, e.message)
                } catch (e: TjenestepensjonIkkeFunnetException) {
                    call.respond(HttpStatusCode.NotFound, e.message)
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDto)
        }

        get {
            val virkFom =
                call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "virkFom ikke angitt")

            val fnr =
                call.request.headers["fnr"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val tpnummer =
                call.request.headers["tpnr"]
                    ?: throw ManglerTpNrException()

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        virkFom = virkFom,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        MaskinportenTpContext(
                            tpnr = Tjenestepensjonnummer(tpnummer),
                            organisasjonsnr = call.orgNummer,
                        ),
                    )
                } catch (e: TjenestepensjonManglendeTilgangException) {
                    call.respond(HttpStatusCode.Unauthorized, e.message)
                } catch (e: TjenestepensjonUgyldigForesporselException) {
                    call.respond(HttpStatusCode.BadRequest, e.message)
                } catch (e: TjenestepensjonIkkeFunnetException) {
                    call.respond(HttpStatusCode.NotFound, e.message)
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }

    route("api/pensjon/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("les-oms-vedtak")
        }

        get {
            val virkFom =
                call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "virkFom ikke angitt")

            val fnr =
                call.request.headers["fnr"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "fnr ikke angitt")

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        virkFom = virkFom,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        PensjonContext,
                    )
                } catch (e: TjenestepensjonManglendeTilgangException) {
                    call.respond(HttpStatusCode.Unauthorized, e.message)
                } catch (e: TjenestepensjonUgyldigForesporselException) {
                    call.respond(HttpStatusCode.BadRequest, e.message)
                } catch (e: TjenestepensjonIkkeFunnetException) {
                    call.respond(HttpStatusCode.NotFound, e.message)
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }
}

inline val ApplicationCall.orgNummer: String
    get() {
        val claims =
            this.hentTokenClaims("maskinporten")
                ?.get("consumer") as Map<*, *>?
                ?: throw IllegalArgumentException("Kan ikke hente ut organisasjonsnummer")
        return (claims["ID"] as String).split(":")[1]
    }
