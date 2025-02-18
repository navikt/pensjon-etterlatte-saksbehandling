package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import java.time.LocalDateTime
import java.util.UUID

fun Route.etteroppgjoerRoutes() {
    route("/api/etteroppgjoer/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            val etteroppgjoerBehandling =
                EtteroppgjoerBehandling(
                    id = behandlingId,
                    status = "mottatt",
                    sak =
                        Sak(
                            id = SakId(123L),
                            ident = "30492253347",
                            sakType = SakType.OMSTILLINGSSTOENAD,
                            enhet = Enhetsnummer.ukjent,
                        ),
                    aar = 2024,
                    opprettet = LocalDateTime.now().toString(),
                    opplysninger =
                        EtteroppgjoerOpplysninger(
                            skatt =
                                OpplysnignerSkatt(
                                    aarsinntekt = 200000,
                                ),
                            ainntekt =
                                AInntekt(
                                    inntektsmaaneder =
                                        listOf(
                                            AInntektMaaned(
                                                maaned = "Januar",
                                                summertBeloep = 150000,
                                            ),
                                            AInntektMaaned(
                                                maaned = "Januar",
                                                summertBeloep = 150000,
                                            ),
                                        ),
                                ),
                        ),
                )
            call.respond(etteroppgjoerBehandling)
        }
    }
}

data class EtteroppgjoerBehandling(
    val id: UUID,
    val status: String,
    val sak: Sak,
    val aar: Int,
    val opprettet: String,
    val opplysninger: EtteroppgjoerOpplysninger,
)

data class EtteroppgjoerOpplysninger(
    val skatt: OpplysnignerSkatt,
    val ainntekt: AInntekt,
    // TODO..
)

data class OpplysnignerSkatt(
    val aarsinntekt: Int,
)

data class AInntekt(
    val inntektsmaaneder: List<AInntektMaaned>,
)

data class AInntektMaaned(
    val maaned: String,
    val summertBeloep: Int,
)
