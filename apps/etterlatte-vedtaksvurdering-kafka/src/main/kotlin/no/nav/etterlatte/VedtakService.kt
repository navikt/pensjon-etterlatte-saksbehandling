package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
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
    ): VedtakOgRapid

    fun opprettVedtakFattOgAttester(
        sakId: Long,
        behandlingId: UUID,
        kjoringVariant: MigreringKjoringVariant,
    ): VedtakOgRapid

    fun tilbakestillVedtak(behandlingId: UUID)

    fun tilSamordningVedtak(behandlingId: UUID): VedtakDto

    fun samordnetVedtak(vedtakId: String): VedtakDto?

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
    ): VedtakOgRapid =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$sakId/$behandlingId/automatisk").body()
        }

    override fun opprettVedtakFattOgAttester(
        sakId: Long,
        behandlingId: UUID,
        kjoringVariant: MigreringKjoringVariant,
    ): VedtakOgRapid =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$sakId/$behandlingId/automatisk/stegvis") {
                contentType(ContentType.Application.Json)
                setBody(kjoringVariant.toJson())
            }.body()
        }

    override fun tilbakestillVedtak(behandlingId: UUID) {
        runBlocking {
            vedtakKlient.patch("$url/api/vedtak/$behandlingId/tilbakestill")
        }
    }

    override fun tilSamordningVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/tilsamordning") {
                contentType(ContentType.Application.Json)
            }.body()
        }

    override fun samordnetVedtak(vedtakId: String): VedtakDto? =
        runBlocking {
            vedtakKlient.post("$url/vedtak/samordnet/$vedtakId") {
                contentType(ContentType.Application.Json)
            }.body()
        }

    override fun iverksattVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/iverksett") {
                contentType(ContentType.Application.Json)
            }.body()
        }
}
