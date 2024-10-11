package no.nav.etterlatte.gyldigsoeknad.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingsBehov
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakMedBehandlinger
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import java.time.LocalDateTime
import java.util.UUID

class FeiletVedOpprettBehandling(
    sakId: SakId,
) : UgyldigForespoerselException(
        code = "FEILET_OPPRETTELSE_AV_BEHANDLING",
        detail = "Feilet ved opprettelse av behandling for sakid $sakId",
    )

class BehandlingClient(
    private val sakOgBehandlingApp: HttpClient,
    private val url: String,
) {
    fun opprettBehandling(
        sakId: SakId,
        mottattDato: LocalDateTime,
        persongalleri: Persongalleri,
    ): UUID =
        runBlocking {
            val response =
                sakOgBehandlingApp
                    .post("$url/behandlinger/opprettbehandling") {
                        contentType(ContentType.Application.Json)
                        setBody(BehandlingsBehov(sakId, persongalleri, mottattDato.toString()))
                    }
            if (!response.status.isSuccess()) {
                throw FeiletVedOpprettBehandling(sakId)
            }
            UUID.fromString(response.body())
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

    fun hentSakMedBehandlinger(sakId: SakId): SakMedBehandlinger =
        runBlocking {
            sakOgBehandlingApp.get("$url/saker/${sakId.sakId}/behandlinger").body()
        }

    fun opprettOppgave(
        sakId: SakId,
        oppgave: NyOppgaveDto,
    ): UUID =
        runBlocking {
            sakOgBehandlingApp
                .post("$url/oppgaver/sak/${sakId.sakId}/opprett") {
                    contentType(ContentType.Application.Json)
                    setBody(oppgave)
                }.body<OppgaveIntern>()
                .id
        }
}
