package no.nav.etterlatte
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*


interface Behandling {
    fun grunnlagEndretISak(sak: Long)
    fun vedtakHendelse(behandlingid: UUID, hendelse: String,
                       vedtakId: Long,
                       inntruffet: Tidspunkt,
                       saksbehandler: String?,
                       kommentar: String?,
                       valgtBegrunnelse: String?)
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
    override fun vedtakHendelse(behandlingid: UUID, hendelse: String,
                                vedtakId: Long,
                                inntruffet: Tidspunkt,
                                saksbehandler: String?,
                                kommentar: String?,
                                valgtBegrunnelse: String?) {
        runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandlingid/hendelser/vedtak/$hendelse") {
                body = VedtakHendelse(vedtakId, inntruffet, saksbehandler, kommentar, valgtBegrunnelse)
            }
        }
    }

    override fun slettSakOgBehandlinger(sakId: Long) {
        runBlocking {
            behandling_app.delete<String>("$url/sak/$sakId/behandlinger")
            behandling_app.delete<String>("$url/saker/$sakId/")
        }
    }

}


data class VedtakHendelse(
    val vedtakId: Long,
    val inntruffet: Tidspunkt,
    val saksbehandler: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?,
)