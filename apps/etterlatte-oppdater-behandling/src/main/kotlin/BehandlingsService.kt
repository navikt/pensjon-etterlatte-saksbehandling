package no.nav.etterlatte
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun leggTilVilkaarsresultat(behandling: UUID, vilkaarResultat: VilkaarResultat) {
         runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/lagrevilkaarsproeving") {
                contentType(ContentType.Application.Json)
                body = vilkaarResultat
            }
        }
    }
}

data class LeggTilVilkaarsResultatRequest(
    val vilkaarResultat: VilkaarResultat
)


