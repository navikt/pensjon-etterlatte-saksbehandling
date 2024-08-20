package no.nav.etterlatte.gyldigsoeknad.config

import io.ktor.client.plugins.auth.Auth
import no.nav.etterlatte.EnvKey
import no.nav.etterlatte.EnvKey.BEHANDLING_AZURE_SCOPE
import no.nav.etterlatte.EnvKey.BEHANDLING_URL
import no.nav.etterlatte.EnvKey.DOKARKIV_URL
import no.nav.etterlatte.EnvKey.PDFGEN_URL
import no.nav.etterlatte.JournalfoerInntektsjusteringService
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.DokarkivKlient
import no.nav.etterlatte.gyldigsoeknad.journalfoering.JournalfoerSoeknadService
import no.nav.etterlatte.gyldigsoeknad.pdf.PdfGeneratorKlient
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.AzureEnums.AZURE_APP_OUTBOUND_SCOPE
import no.nav.etterlatte.libs.ktor.httpClient
import no.nav.etterlatte.libs.ktor.ktor.clientCredential

class AppBuilder(
    private val env: Miljoevariabler,
) {
    val behandlingKlient: BehandlingClient by lazy {
        BehandlingClient(
            httpClient(BEHANDLING_AZURE_SCOPE),
            env.requireEnvValue(BEHANDLING_URL),
        )
    }

    val journalfoerSoeknadService: JournalfoerSoeknadService by lazy {
        JournalfoerSoeknadService(
            DokarkivKlient(
                httpClient(EnvKey.DOKARKIV_SCOPE),
                env.requireEnvValue(DOKARKIV_URL),
            ),
            PdfGeneratorKlient(httpClient(), env.requireEnvValue(PDFGEN_URL)),
        )
    }

    val journalfoerInntektsjusteringService: JournalfoerInntektsjusteringService by lazy {
        JournalfoerInntektsjusteringService(
            DokarkivKlient(
                httpClient(EnvKey.DOKARKIV_SCOPE),
                env.requireEnvValue(DOKARKIV_URL),
            ),
            // TODO skal få egen mal og path
            PdfGeneratorKlient(httpClient(), "http://ey-pdfgen/api/v1/genpdf/notat"),
        )
    }

    private fun httpClient(scope: EnvEnum) =
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
