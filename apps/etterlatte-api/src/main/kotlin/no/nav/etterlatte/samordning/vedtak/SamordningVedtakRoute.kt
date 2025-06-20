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
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.AuthorizationPlugin
import no.nav.etterlatte.MaskinportenScopeAuthorizationPlugin
import no.nav.etterlatte.hentFnrBody
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.route.dato
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.libs.ktor.token.hentTokenClaimsForIssuerName

fun haandterUgyldigIdent(fnr: String): Folkeregisteridentifikator {
    try {
        return Folkeregisteridentifikator.of(fnr)
    } catch (_: Exception) {
        throw UgyldigFoedselsnummerException()
    }
}

fun Route.samordningVedtakRoute(
    samordningVedtakService: SamordningVedtakService,
    config: Config,
    appname: String,
) {
    route("api/vedtak") {
        install(MaskinportenScopeAuthorizationPlugin) {
            scopes =
                setOf("nav:etterlatteytelser:vedtaksinformasjon.read", "nav:etterlatteytelser/vedtaksinformasjon.read")
        }

        get("{vedtakId}") {
            val vedtakId =
                krevIkkeNull(call.parameters["vedtakId"]?.toLong()) {
                    "VedtakId mangler - dette er mest sannsynlig en systemfeil"
                }

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

        post {
            val fomDato =
                call.dato("fomDato")
                    ?: call.dato("virkFom")
                    ?: throw ManglerFomDatoException()
            val fnr = hentFnrBody()
            val tpnummer =
                call.request.headers["tpnr"]
                    ?: throw ManglerTpNrException()

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        fnr = haandterUgyldigIdent(fnr.foedselsnummer),
                        context =
                            MaskinportenTpContext(
                                tpnr = Tjenestepensjonnummer(tpnummer),
                                organisasjonsnr = call.orgNummer,
                            ),
                    )
                } catch (e: IllegalArgumentException) {
                    return@post call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }
    }

    route("api/pensjon/vedtak") {
        install(AuthorizationPlugin) {
            accessPolicyRolesEllerAdGrupper =
                setOf("les-oms-vedtak", "les-oms-samordning-vedtak", config.getString("roller.pensjon-saksbehandler"))
            issuers = setOf(Issuer.AZURE.issuerName)
        }
        install(selvbetjeningAuthorizationPlugin(appname)) {
            validator = { fnr, borger -> borger.value == fnr.value }
            issuer = Issuer.TOKENX.issuerName
        }

        post {
            val fomDato =
                call.dato("fomDato")
                    ?: call.dato("virkFom")
                    ?: throw ManglerFomDatoException()

            val fnr = hentFnrBody()

            val samordningVedtakDtos =
                try {
                    samordningVedtakService.hentVedtaksliste(
                        fomDato = fomDato,
                        fnr = haandterUgyldigIdent(fnr.foedselsnummer),
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@post call.respondNullable(HttpStatusCode.BadRequest, e.message)
                }

            call.respond(samordningVedtakDtos)
        }

        post("/har-loepende-oms") {
            val paaDato = call.dato("paaDato") ?: throw ManglerPaaDatoException()
            val fnr = hentFnrBody()

            val harLoependeOmsPaaDato =
                try {
                    samordningVedtakService.harLoependeYtelsePaaDato(
                        dato = paaDato,
                        fnr = haandterUgyldigIdent(fnr.foedselsnummer),
                        sakType = SakType.OMSTILLINGSSTOENAD,
                        context = PensjonContext,
                    )
                } catch (e: IllegalArgumentException) {
                    return@post call.respondNullable(HttpStatusCode.BadRequest, e.message)
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
