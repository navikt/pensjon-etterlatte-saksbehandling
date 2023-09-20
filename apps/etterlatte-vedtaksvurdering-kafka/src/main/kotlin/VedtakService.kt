package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import java.time.LocalDate
import java.util.UUID

interface VedtakService {
    fun harLoependeYtelserFra(
        sakId: Long,
        dato: LocalDate,
    ): LoependeYtelseDTO

    fun opprettVedtakFattOgAttester(
        sakId: Long,
        behandlingId: UUID,
    ): VedtakDto

    fun tilbakestillVedtak(behandlingId: UUID)

    fun iverksattVedtak(behandlingId: UUID): VedtakDto
}

class VedtakServiceImpl(private val vedtakKlient: HttpClient, private val url: String) : VedtakService {
    override fun harLoependeYtelserFra(
        sakId: Long,
        dato: LocalDate,
    ): LoependeYtelseDTO =
        runBlocking {
            vedtakKlient.get("$url/api/vedtak/loepende/$sakId?dato=$dato").body()
        }

    override fun opprettVedtakFattOgAttester(
        sakId: Long,
        behandlingId: UUID,
    ): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$sakId/$behandlingId/automatisk").body()
        }

    override fun tilbakestillVedtak(behandlingId: UUID) {
        runBlocking {
            vedtakKlient.patch("$url/api/vedtak/$behandlingId/tilbakestill")
        }
    }

    override fun iverksattVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/iverksett") {
                contentType(ContentType.Application.Json)
            }.body()
        }
}
