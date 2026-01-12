package no.nav.etterlatte.institusjonsopphold.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.institusjonsopphold.model.Institusjonsopphold
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdEkstern
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdForPersoner
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdListeEkstern
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.navConsumerId

class InstitusjonsoppholdKlient(
    private val httpKlient: HttpClient,
    private val url: String,
) {
    suspend fun hentDataForHendelse(oppholdId: Long) =
        retry<InstitusjonsoppholdEkstern> {
            httpKlient
                .get("$url/api/v1/person/institusjonsopphold/$oppholdId?Med-Institusjonsinformasjon=true") {
                    contentType(ContentType.Application.Json)
                    navConsumerId("etterlatte-institusjonsopphold")
                    header("Nav-Formaal", "ETTERLATTEYTELSER")
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> {
                    it.content
                }

                is RetryResult.Failure -> {
                    throw RuntimeException(
                        "Feil oppsto ved henting av institusjonsopphold (id=$oppholdId)",
                        it.samlaExceptions(),
                    )
                }
            }
        }

    suspend fun hentOppholdForPersoner(request: HentOppholdRequest) =
        retry {
            val oppholdListe: Map<String, InstitusjonsoppholdListeEkstern> =
                httpKlient
                    .post("$url/api/v1/personer/institusjonsopphold/soek") {
                        contentType(ContentType.Application.Json)
                        navConsumerId("etterlatte-institusjonsopphold")
                        header("Nav-Formaal", "ETTERLATTEYTELSER")
                        setBody(request.toJson())
                    }.body()
            toInternalDto(oppholdListe)
        }.let {
            when (it) {
                is RetryResult.Success -> {
                    it.content
                }

                is RetryResult.Failure -> {
                    throw RuntimeException(
                        "Feil oppsto ved henting av institusjonsopphold",
                        it.samlaExceptions(),
                    )
                }
            }
        }

    private fun toInternalDto(eksternDto: Map<String, InstitusjonsoppholdListeEkstern>): InstitusjonsoppholdForPersoner {
        val data: Map<String, List<Institusjonsopphold>> =
            eksternDto.mapValues { (_, value) ->
                value.institusjonsoppholdsliste.map {
                    Institusjonsopphold(
                        oppholdId = it.oppholdId,
                        institusjonsnavn = it.institusjonsnavn,
                        avdelingsnavn = it.avdelingsnavn,
                        organisasjonsnummer = it.organisasjonsnummer,
                        institusjonstype = it.institusjonstype,
                        kategori = it.kategori,
                        startdato = it.startdato,
                        faktiskSluttdato = it.faktiskSluttdato,
                        forventetSluttdato = it.forventetSluttdato,
                        endringstidspunkt = it.endringstidspunkt,
                    )
                }
            }
        return InstitusjonsoppholdForPersoner(data = data)
    }
}

data class HentOppholdRequest(
    val personidenter: List<String>,
)
