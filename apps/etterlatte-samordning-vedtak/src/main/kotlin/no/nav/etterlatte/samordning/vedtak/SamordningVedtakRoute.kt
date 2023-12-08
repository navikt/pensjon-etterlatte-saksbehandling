package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
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

fun Route.samordningVedtakRoute(
    samordningVedtakService: SamordningVedtakService,
    config: Config,
) {
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
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDto)
        }

        get {
            val fomDato =
                call.parameters["fomDato"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: throw ManglerFomDatoException()

            val fnr = call.fnr

            val tpnummer =
                call.request.headers["tpnr"]
                    ?: throw ManglerTpNrException()

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        MaskinportenTpContext(
                            tpnr = Tjenestepensjonnummer(tpnummer),
                            organisasjonsnr = call.orgNummer,
                        ),
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }

    route("api/pensjon/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("les-oms-vedtak", config.getString("roller.pensjon-saksbehandler"))
            issuers = setOf("azure")
        }
        install(SelvbetjeningAuthorizationPlugin) {
            validator = { call, borger -> borger.value == call.fnr }
            issuer = "tokenx"
        }

        get {
            val fomDato =
                call.parameters["fomDato"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: call.parameters["virkFom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: throw ManglerFomDatoException()

            val fnr = call.fnr

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }

        get("/ping") {
            call.respond(
                getMeta(),
            )
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

inline val ApplicationCall.fnr: String
    get() {
        return this.request.headers["fnr"]
            ?: throw ManglerFoedselsnummerException()
    }
