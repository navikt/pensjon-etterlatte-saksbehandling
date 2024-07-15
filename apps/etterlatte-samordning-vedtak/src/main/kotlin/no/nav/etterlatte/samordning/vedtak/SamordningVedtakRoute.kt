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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.AuthorizationPlugin
import no.nav.etterlatte.libs.ktor.Issuer
import no.nav.etterlatte.libs.ktor.MaskinportenScopeAuthorizationPlugin
import no.nav.etterlatte.libs.ktor.hentTokenClaimsForIssuerName
import no.nav.etterlatte.libs.ktor.route.dato

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
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDto)
        }

        get {
            val fomDato =
                call.dato("fomDato")
                    ?: call.dato("virkFom")
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
                        context =
                            MaskinportenTpContext(
                                tpnr = Tjenestepensjonnummer(tpnummer),
                                organisasjonsnr = call.orgNummer,
                            ),
                    )
                } catch (e: IllegalArgumentException) {
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }

    route("api/pensjon/vedtak") {
        install(AuthorizationPlugin) {
            roles = setOf("les-oms-vedtak", config.getString("roller.pensjon-saksbehandler"))
            issuers = setOf(Issuer.AZURE.issuerName)
        }
        install(SelvbetjeningAuthorizationPlugin) {
            validator = { call, borger -> borger.value == call.fnr }
            issuer = Issuer.TOKENX.issuerName
        }

        get {
            val fomDato =
                call.dato("fomDato")
                    ?: call.dato("virkFom")
                    ?: throw ManglerFomDatoException()

            val fnr = call.fnr

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }

        get("/har-loepende-oms") {
            val paaDato = call.dato("paaDato") ?: throw ManglerPaaDatoException()
            val fnr = call.fnr

            val harLoependeOmsPaaDato =
                try {
                    samordningVedtakService.harLoependeYtelsePaaDato(
                        dato = paaDato,
                        fnr = Folkeregisteridentifikator.of(fnr),
                        sakType = SakType.OMSTILLINGSSTOENAD,
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@get call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(
                mapOf(
                    "omstillingsstoenad" to harLoependeOmsPaaDato,
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

inline val ApplicationCall.orgNummer: String
    get() {
        val claims =
            this
                .hentTokenClaimsForIssuerName(Issuer.MASKINPORTEN)
                ?.get("consumer") as Map<*, *>?
                ?: throw IllegalArgumentException("Kan ikke hente ut organisasjonsnummer")
        return (claims["ID"] as String).split(":")[1]
    }

inline val ApplicationCall.fnr: String
    get() {
        return this.request.headers["fnr"]
            ?: throw ManglerFoedselsnummerException()
    }
