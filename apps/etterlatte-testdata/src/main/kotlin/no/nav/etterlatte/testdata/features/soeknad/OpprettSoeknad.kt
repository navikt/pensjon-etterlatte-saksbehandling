@file:Suppress("ktlint:standard:filename")

package no.nav.etterlatte.testdata.features.soeknad

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.producer
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg

object OpprettSoeknadFeature : TestDataFeature {
    override val beskrivelse: String
        get() = "Opprett søknad manuelt"
    override val path: String
        get() = "soeknad"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "soeknad/ny-soeknad.hbs",
                        mapOf(
                            "beskrivelse" to beskrivelse,
                            "path" to path,
                        ),
                    ),
                )
            }

            post {
                try {
                    val (partisjon, offset) =
                        call.receiveParameters().let {
                            producer.publiser(
                                requireNotNull(it["key"]),
                                opprettSoeknadJson(
                                    ytelse = it["ytelse"]!!,
                                    gjenlevendeFnr = it["fnrGjenlevende"]!!,
                                    avdoedFnr = it["fnrAvdoed"]!!,
                                    barnFnr = it["fnrBarn"]!!,
                                    behandlingssteg = Behandlingssteg.BEHANDLING_OPPRETTA,
                                ),
                                mapOf("NavIdent" to (brukerTokenInfo.ident().toByteArray())),
                            )
                        }
                    logger.info("Publiserer melding med partisjon: $partisjon offset: $offset")

                    call.respondRedirect("/$path/sendt?partisjon=$partisjon&offset=$offset")
                } catch (e: Exception) {
                    logger.error("En feil har oppstått! ", e)

                    call.respond(
                        MustacheContent(
                            "error.hbs",
                            mapOf("errorMessage" to e.message, "stacktrace" to e.stackTraceToString()),
                        ),
                    )
                }
            }

            get("sendt") {
                val partisjon = call.request.queryParameters["partisjon"]!!
                val offset = call.request.queryParameters["offset"]!!

                call.respond(
                    MustacheContent(
                        "soeknad/soeknad-sendt.hbs",
                        mapOf(
                            "path" to path,
                            "beskrivelse" to beskrivelse,
                            "partisjon" to partisjon,
                            "offset" to offset,
                        ),
                    ),
                )
            }
        }
}

private fun opprettSoeknadJson(
    ytelse: String,
    gjenlevendeFnr: String,
    avdoedFnr: String,
    barnFnr: String,
    behandlingssteg: Behandlingssteg,
): String {
    val soeknadType =
        if ("Omstillingsstoenad" == ytelse) {
            SoeknadType.OMSTILLINGSSTOENAD
        } else {
            SoeknadType.BARNEPENSJON
        }

    return SoeknadMapper
        .opprettJsonMessage(
            soeknadType,
            gjenlevendeFnr,
            avdoedFnr,
            listOf(barnFnr),
            behandlingssteg,
        ).toJson()
}
