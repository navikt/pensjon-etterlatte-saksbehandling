package no.nav.etterlatte
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import java.util.*


interface Behandling {
    fun grunnlagEndretISak(sak: Long)
    fun vedtakHendelse(behandlingid: UUID, hendelse: String)
    fun slettSakOgBehandlinger(sakId: Long)
}
class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun grunnlagEndretISak(sak: Long) {
        runBlocking {
            behandling_app.post<String>("$url/saker/$sak/hendelse/grunnlagendret") {}
        }
    }
    override fun vedtakHendelse(behandlingid: UUID, hendelse: String) {
        runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandlingid/hendelser/vedtak/$hendelse") {}
        }
    }

    override fun slettSakOgBehandlinger(sakId: Long) {
        runBlocking {
            behandling_app.delete<String>("$url/sak/$sakId/behandlinger")
            behandling_app.delete<String>("$url/saker/$sakId/")
        }
    }

}


