package no.nav.etterlatte.brukerdialog.soeknad.client

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
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.inntektsjustering.MottattInntektsjustering
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import java.time.LocalDateTime
import java.util.UUID

class FeiletVedOpprettBehandling(
    sakId: SakId,
) : UgyldigForespoerselException(
        code = "FEILET_OPPRETTELSE_AV_BEHANDLING",
        detail = "Feilet ved opprettelse av behandling for sakid $sakId",
    )

@Deprecated(
    "Slå sammen med BehandlingService. Husk at de har ulik feilhåndtering!",
    replaceWith =
        ReplaceWith(
            "BehandlingService",
            "no.nav.etterlatte.behandling.BehandlingService",
        ),
)
class BehandlingClient(
    private val sakOgBehandlingApp: HttpClient,
    private val behandlingAppForventSukess: HttpClient,
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

    fun behandleInntektsjustering(request: MottattInntektsjustering) {
        runBlocking {
            behandlingAppForventSukess
                .post("$url/inntektsjustering/behandle") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
        }
    }

    fun finnEllerOpprettSak(
        fnr: String,
        saktype: SakType,
    ): Sak =
        runBlocking {
            behandlingAppForventSukess
                .post("$url/personer/saker/$saktype") {
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(fnr))
                }.body()
        }

    fun hentSakMedBehandlinger(sakId: SakId): SakMedBehandlinger =
        runBlocking {
            behandlingAppForventSukess.get("$url/saker/${sakId.sakId}/behandlinger").body()
        }

    fun opprettOppgave(
        sakId: SakId,
        oppgave: NyOppgaveDto,
    ): UUID =
        runBlocking {
            behandlingAppForventSukess
                .post("$url/oppgaver/sak/${sakId.sakId}/opprett") {
                    contentType(ContentType.Application.Json)
                    setBody(oppgave)
                }.body<OppgaveIntern>()
                .id
        }

    fun finnOppgaverForReferanse(referanse: String): List<OppgaveIntern> =
        runBlocking {
            behandlingAppForventSukess
                .get("$url/oppgaver/referanse/$referanse") {
                    contentType(ContentType.Application.Json)
                }.body()
        }
}
