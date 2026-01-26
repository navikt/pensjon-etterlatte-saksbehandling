package no.nav.etterlatte.institusjonsopphold.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdEkstern
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.retry
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
}
