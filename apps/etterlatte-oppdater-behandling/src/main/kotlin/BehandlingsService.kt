package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.util.*

interface Behandling : VedlikeholdService {
    fun grunnlagEndretISak(sak: Long)
    fun vedtakHendelse(
        behandlingid: UUID,
        hendelse: String,
        vedtakId: Long,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        valgtBegrunnelse: String?
    )

    fun sendDoedshendelse(doedshendelse: Doedshendelse)
    fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse)
    fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse)
}

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String
) : Behandling {
    override fun grunnlagEndretISak(sak: Long) {
        runBlocking {
            behandling_app.post("$url/saker/$sak/hendelse/grunnlagendret") {}
        }
    }

    override fun vedtakHendelse(
        behandlingid: UUID,
        hendelse: String,
        vedtakId: Long,
        inntruffet: Tidspunkt,
        saksbehandler: String?,
        kommentar: String?,
        valgtBegrunnelse: String?
    ) {
        runBlocking {
            behandling_app.post("$url/behandlinger/$behandlingid/hendelser/vedtak/$hendelse") {
                contentType(ContentType.Application.Json)
                setBody(VedtakHendelse(vedtakId, inntruffet, saksbehandler, kommentar, valgtBegrunnelse))
            }
        }
    }

    override fun slettSak(sakId: Long) {
        runBlocking {
            behandling_app.delete("$url/sak/$sakId/behandlinger")
            behandling_app.delete("$url/saker/$sakId/")
        }
    }

    override fun sendDoedshendelse(doedshendelse: Doedshendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/doedshendelse") {
                contentType(ContentType.Application.Json)
                setBody(doedshendelse)
            }
        }
    }

    override fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/utflyttingshendelse") {
                contentType(ContentType.Application.Json)
                setBody(utflyttingsHendelse)
            }
        }
    }

    override fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                contentType(ContentType.Application.Json)
                setBody(forelderBarnRelasjon)
            }
        }
    }
}

data class VedtakHendelse(
    val vedtakId: Long,
    val inntruffet: Tidspunkt,
    val saksbehandler: String?,
    val kommentar: String?,
    val valgtBegrunnelse: String?
)