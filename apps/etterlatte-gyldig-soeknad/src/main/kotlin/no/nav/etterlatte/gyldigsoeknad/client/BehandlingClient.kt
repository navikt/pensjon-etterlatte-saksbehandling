package no.nav.etterlatte.gyldigsoeknad.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import java.time.LocalDateTime
import java.util.UUID

class BehandlingClient(
    private val sakOgBehandlingApp: HttpClient,
    private val url: String,
) {
    fun opprettBehandling(
        sak: Long,
        mottattDato: LocalDateTime,
        persongalleri: Persongalleri,
    ): UUID =
        runBlocking {
            sakOgBehandlingApp
                .post("$url/behandlinger/opprettbehandling") {
                    contentType(ContentType.Application.Json)
                    setBody(BehandlingsBehov(sak, persongalleri, mottattDato.toString()))
                }.also { require(it.status.isSuccess()) }
                .body<String>()
        }.let {
            UUID.fromString(it)
        }

    fun finnEllerOpprettSak(
        fnr: String,
        saktype: SakType,
    ): Sak =
        runBlocking {
            sakOgBehandlingApp
                .post("$url/personer/saker/$saktype") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.body()
        }

    fun lagreGyldighetsVurdering(
        behandlingId: UUID,
        gyldighet: GyldighetsResultat,
    ) = runBlocking {
        sakOgBehandlingApp
            .post("$url/behandlinger/$behandlingId/gyldigfremsatt") {
                contentType(ContentType.Application.Json)
                setBody(gyldighet)
            }.body<String>()
    }

    fun opprettOppgave(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        oppgave: NyOppgaveDto,
    ) = runBlocking {
        sakOgBehandlingApp
            .post("$url/oppgaver/sak/$sakId/opprett") {
                contentType(ContentType.Application.Json)
                setBody(oppgave)
            }.body<String>()
    }
}
