package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.NAV_CONSUMER_ID
import java.time.LocalDate
import java.time.LocalDateTime

class InstitusjonsoppholdKlient(val institusjonsoppholdHttpKlient: HttpClient, val proxyUrl: String) {
    fun hentDataForHendelse(oppholdId: Long) =
        runBlocking {
            institusjonsoppholdHttpKlient.get(proxyUrl.plus("/inst2/$oppholdId?Med-Institusjonsinformasjon=true")) {
                contentType(ContentType.Application.Json)
                header(NAV_CONSUMER_ID, "etterlatte-institusjonsopphold")
            }.body<Institusjonsopphold>()
        }
}

data class Institusjonsopphold(
    val oppholdId: Long,
    val tssEksternId: String,
    val institusjonsnavn: String? = null,
    val avdelingsnavn: String? = null,
    val organisasjonsnummer: String? = null,
    val institusjonstype: String? = null,
    val varighet: String? = null,
    val kategori: String? = null,
    val startdato: LocalDate,
    val faktiskSluttdato: LocalDate? = null,
    val forventetSluttdato: LocalDate? = null,
    val kilde: String? = null,
    val overfoert: Boolean? = null,
    val registrertAv: String? = null,
    val endretAv: String? = null,
    val endringstidspunkt: LocalDateTime? = null,
)
