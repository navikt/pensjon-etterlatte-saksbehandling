package no.nav.etterlatte.gyldigsoeknad.config

import io.ktor.client.plugins.auth.Auth
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.clientCredential
import pdf.PdfGeneratorKlient

class AppBuilder(private val env: Miljoevariabler) {
    val behandlingKlient: BehandlingClient by lazy {
        BehandlingClient(
            httpClient(env.requireEnvValue("BEHANDLING_AZURE_SCOPE")),
            env.requireEnvValue("BEHANDLING_URL"),
        )
    }

    val journalfoerSoeknadService: JournalfoerSoeknadService by lazy {
        JournalfoerSoeknadService(
            DokarkivKlient(
                httpClient(env.requireEnvValue("DOKARKIV_URL")),
                env.requireEnvValue("DOKARKIV_SCOPE"),
            ),
            PdfGeneratorKlient(httpClient(), env.requireEnvValue("PDFGEN_URL")),
        )
    }

    private fun httpClient(scope: String? = null) =
        httpClient(
            auth = {
                if (scope != null) {
                    it.install(Auth) {
                        clientCredential {
                            config =
                                env.props.toMutableMap()
                                    .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get(scope))) }
                        }
                    }
                }
            },
        )
}
