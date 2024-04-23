package no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.behandling.VirkningstidspunktRequest
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

class SakService(private val klient: HttpClient, private val url: String) {
    suspend fun hentSak(sakId: Long) = klient.get("$url/saker/$sakId")

    suspend fun settKommerBarnetTilGode(behandling: UUID) {
        klient.post("$url/api/behandling/$behandling/kommerbarnettilgode") {
            contentType(ContentType.Application.Json)
            setBody(
                KommerBarnetTilgode(
                    JaNei.JA,
                    "Automatisk behandla testsak",
                    Grunnlagsopplysning.Pesys.create(),
                    behandlingId = behandling,
                ),
            )
        }
    }

    suspend fun lagreGyldighetsproeving(behandling: UUID) {
        klient.post("$url/api/behandling/$behandling/gyldigfremsatt") {
            contentType(ContentType.Application.Json)
            setBody(
                JaNeiMedBegrunnelse(JaNei.JA, "Automatisk behandla testsak"),
            )
        }
    }

    suspend fun lagreVirkningstidspunkt(behandling: UUID) {
        klient.post("$url/api/behandling/$behandling/virkningstidspunkt") {
            contentType(ContentType.Application.Json)
            setBody(
                VirkningstidspunktRequest(
                    _dato = Tidspunkt.now().toString(),
                    begrunnelse = "Automatisk behandla testsak",
                    kravdato = LocalDate.now(),
                ),
            )
        }
    }

    suspend fun tildelSaksbehandler(
        navn: String,
        sakId: Long,
    ) {
        val oppgaver = klient.get("$url/api/oppgaver/sak/$sakId").body<List<OppgaveIntern>>()

        oppgaver.forEach {
            klient.post("$url/api/oppgaver/${it.id}/tildel-saksbehandler") {
                contentType(ContentType.Application.Json)
                setBody(SaksbehandlerEndringDto(navn))
            }
        }
    }
}
