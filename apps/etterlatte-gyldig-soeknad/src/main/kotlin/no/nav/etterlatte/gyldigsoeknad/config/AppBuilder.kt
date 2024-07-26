package no.nav.etterlatte.gyldigsoeknad.config

import io.ktor.client.plugins.auth.Auth
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.gyldigsoeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_OUTBOUND_SCOPE
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.clientCredential

class AppBuilder(
    private val env: Miljoevariabler,
) {
    val behandlingKlient: BehandlingClient by lazy {
        BehandlingClient(
            httpClient("BEHANDLING_AZURE_SCOPE"),
            env.requireEnvValue("BEHANDLING_URL"),
        )
    }

    val journalfoerSoeknadService: JournalfoerSoeknadService by lazy {
        JournalfoerSoeknadService(
            DokarkivKlient(
                httpClient("DOKARKIV_SCOPE"),
                env.requireEnvValue("DOKARKIV_URL"),
            ),
            PdfGeneratorKlient(httpClient(), env.requireEnvValue("PDFGEN_URL")),
        )
    }

    private fun httpClient(scope: String) =
        httpClient(
            auth = {
                it.install(Auth) {
                    clientCredential {
                        config = env.append(AZURE_APP_OUTBOUND_SCOPE) { requireNotNull(it[scope]) }
                    }
                }
            },
        )
}
