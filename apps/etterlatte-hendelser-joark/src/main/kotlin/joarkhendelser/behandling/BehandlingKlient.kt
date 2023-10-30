package joarkhendelser.behandling

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

class BehandlingKlient(
    private val httpClient: HttpClient,
    private val url: String,
) {
    suspend fun hentEllerOpprettSak(
        fnr: String,
        sakType: SakType,
    ): Long {
        return httpClient.post("$url/personer/saker/$sakType") {
            contentType(ContentType.Application.Json)
            setBody(FoedselsNummerMedGraderingDTO(fnr, gradering = null))
        }.body<ObjectNode>()["id"].longValue()
    }

    suspend fun opprettOppgave(sakId: Long): Long {
        return httpClient.post("$url/oppgaver/sak/$sakId/oppgaver") {
            contentType(ContentType.Application.Json)
            setBody(
                NyOppgaveDto(
                    OppgaveKilde.HENDELSE,
                    OppgaveType.MANUELL_JOURNALFOERING,
                    null,
                ),
            )
        }.body<ObjectNode>()["id"].longValue()
    }
}
