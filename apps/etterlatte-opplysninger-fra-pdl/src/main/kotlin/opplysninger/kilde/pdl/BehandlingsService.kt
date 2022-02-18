package no.nav.etterlatte.opplysninger.kilde.pdl
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun leggTilOpplysninger(behandling: UUID, opplysninger: List<Behandlingsopplysning<out Any>>) {
         runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/grunnlag") {
                contentType(ContentType.Application.Json)
                body = LeggTilOpplysningerRequest(opplysninger)
            }
        }
    }

}

data class LeggTilOpplysningerRequest(
    val opplysninger: List<Behandlingsopplysning<out Any>>
)

