package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.FoedselsNummerMedGraderingDTO
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

class BehandlingKlient(
    private val httpClient: HttpClient,
    private val url: String,
) {
    suspend fun hentSak(
        fnr: String,
        sakType: SakType,
        gradering: AdressebeskyttelseGradering?,
    ): Long {
        return httpClient.post("$url/personer/saker/$sakType") {
            contentType(ContentType.Application.Json)
            setBody(FoedselsNummerMedGraderingDTO(fnr, gradering))
        }.body<ObjectNode>()["id"].longValue()
    }

    suspend fun opprettOppgave(sakId: Long): Long {
        return httpClient.post("$url/oppgaver/sak/$sakId/opprett") {
            contentType(ContentType.Application.Json)
            setBody(
                NyOppgaveDto(
                    OppgaveKilde.EKSTERN,
                    OppgaveType.MANUELL_JOURNALFOERING,
                    "Noe feilet. Opprett søknad via Gosys-oppgave",
                ),
            )
        }.body<ObjectNode>()["id"].longValue()
    }
}
