package no.nav.etterlatte.regulering

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import java.time.LocalDate
import java.util.*

interface VedtakService {
    fun harLoependeYtelserFra(sakId: Long, dato: LocalDate): LoependeYtelseDTO
    fun upsertVedtak(behandlingId: UUID): VedtakDto
    fun fattVedtak(behandlingId: UUID): VedtakDto
    fun attesterVedtak(behandlingId: UUID): VedtakDto
}

class VedtakServiceImpl(private val vedtakKlient: HttpClient, private val url: String) : VedtakService {
    override fun harLoependeYtelserFra(sakId: Long, dato: LocalDate): LoependeYtelseDTO =
        runBlocking {
            vedtakKlient.get("$url/api/vedtak/loepende/$sakId?dato=$dato").body()
        }

    override fun upsertVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/upsert").body()
        }

    override fun fattVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/fattvedtak").body()
        }

    override fun attesterVedtak(behandlingId: UUID): VedtakDto =
        runBlocking {
            vedtakKlient.post("$url/api/vedtak/$behandlingId/attester").body()
        }
}