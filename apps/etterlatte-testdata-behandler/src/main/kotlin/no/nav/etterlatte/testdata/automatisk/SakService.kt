package no.nav.etterlatte.testdata.automatisk

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.behandling.VirkningstidspunktRequest
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.SaksbehandlerEndringDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.sak.UtlandstilknytningRequest
import java.time.YearMonth
import java.util.UUID

class SakService(private val klient: DownstreamResourceClient, private val url: String, private val clientId: String) {
    suspend fun hentSak(sakId: Long): Sak =
        klient.get(Resource(clientId, "$url/saker/$sakId"), Systembruker.testdata).mapBoth(
            success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
            failure = { throw it },
        )

    suspend fun settKommerBarnetTilGode(behandling: UUID) {
        klient.post(
            Resource(clientId, "$url/api/behandling/$behandling/kommerbarnettilgode"),
            Systembruker.testdata,
            postBody =
                JaNeiMedBegrunnelse(
                    JaNei.JA,
                    "Automatisk behandla testsak",
                ),
        )
            .mapBoth(
                success = {},
                failure = { throw it },
            )
    }

    suspend fun lagreGyldighetsproeving(behandling: UUID) {
        klient.post(
            Resource(clientId, "$url/api/behandling/$behandling/gyldigfremsatt"),
            Systembruker.testdata,
            JaNeiMedBegrunnelse(JaNei.JA, "Automatisk behandla testsak"),
        ).mapBoth(
            success = {},
            failure = { throw it },
        )
    }

    suspend fun lagreUtlandstilknytning(behandling: UUID) {
        klient.post(
            Resource(clientId, "$url/api/behandling/$behandling/utlandstilknytning"),
            Systembruker.testdata,
            UtlandstilknytningRequest(
                utlandstilknytningType = UtlandstilknytningType.NASJONAL,
                begrunnelse = "Automatisk behandla testsak",
            ),
        ).mapBoth(
            success = {},
            failure = { throw it },
        )
    }

    suspend fun lagreVirkningstidspunkt(behandling: UUID) {
        klient.post(
            Resource(clientId, "$url/api/behandling/$behandling/virkningstidspunkt"),
            Systembruker.testdata,
            VirkningstidspunktRequest(
                _dato = YearMonth.now().toString(),
                begrunnelse = "Automatisk behandla testsak",
                kravdato = null,
            ),
        ).mapBoth(
            success = {},
            failure = { throw it },
        )
    }

    suspend fun tildelSaksbehandler(
        navn: String,
        sakId: Long,
    ) {
        val oppgaver: List<OppgaveIntern> =
            klient.get(Resource(clientId, "$url/oppgaver/sak/$sakId/oppgaver"), Systembruker.testdata)
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throw it },
                )

        oppgaver.forEach {
            klient.post(
                Resource(clientId, "$url/api/oppgaver/${it.id}/tildel-saksbehandler"),
                Systembruker.testdata,
                SaksbehandlerEndringDto(navn),
            )
        }
    }
}
