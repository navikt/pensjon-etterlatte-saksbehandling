package no.nav.etterlatte.regulering

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import java.time.LocalDate

interface VedtakService {
    fun harLoependeYtelserFra(sakId: Long, dato: LocalDate): LoependeYtelseDTO
}

class VedtakServiceImpl(private val vedtakKlient: HttpClient, private val url: String) : VedtakService {
    override fun harLoependeYtelserFra(sakId: Long, dato: LocalDate): LoependeYtelseDTO =
        runBlocking {
            vedtakKlient.get("$url/api/vedtak/loepende/$sakId?dato=$dato").body()
        }
}