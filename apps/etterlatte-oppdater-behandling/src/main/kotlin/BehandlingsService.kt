package no.nav.etterlatte
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun leggTilGyldighetsresultat(behandling: UUID, gyldighetsResultat: GyldighetsResultat) {
        runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/lagregyldighetsproeving") {
                contentType(ContentType.Application.Json)
                body = gyldighetsResultat
            }
        }
    }

    override fun grunnlagEndretISak(sak: Long) {
        runBlocking {
            behandling_app.post<String>("$url/saker/$sak/hendelse/grunnlagendret") {}
        }
    }

}


