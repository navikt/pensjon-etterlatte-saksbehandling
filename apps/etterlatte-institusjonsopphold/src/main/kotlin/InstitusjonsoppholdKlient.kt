package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.X_CORRELATION_ID
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import java.time.LocalDate
import java.time.LocalDateTime

class InstitusjonsoppholdKlient(val institusjonsoppholdHttpKlient: HttpClient, val proxyUrl: String) {

    fun hentDataForHendelse(oppholdId: Long) =
        runBlocking {
            institusjonsoppholdHttpKlient.get(proxyUrl.plus("/inst2/$oppholdId")) {
                contentType(ContentType.Application.Json)
                header(X_CORRELATION_ID, getXCorrelationId())
                header("Nav_Call_Id", getXCorrelationId())
                header("Nav-Consumer-Id", "etterlatte-institusjonsopphold")
            }.body<Institusjonsopphold>()
        }
}

data class Institusjonsopphold(
    val oppholdId: Long,
    val tssEksternId: String,
    val institusjonsnavn: String,
    val avdelingsnavn: String,
    val organisasjonsnummer: String,
    val institusjonstype: String,
    val varighet: String,
    val kategori: String,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate,
    val forventetSluttdato: LocalDate,
    val kilde: String,
    val overfoert: Boolean? = null,
    val registrertAv: String? = null,
    val endretAv: String,
    val endringstidspunkt: LocalDateTime? = null
)