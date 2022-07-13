package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.saksbehandling.api.typer.klientside.DetaljertBehandlingDto
import no.nav.etterlatte.typer.Sak
import no.nav.etterlatte.typer.Saker
import no.nav.etterlatte.typer.LagretVedtakHendelser
import org.slf4j.LoggerFactory


data class PersonSakerResult(val person: Person, val saker: Saker)

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Grunnlagsopplysning<ObjectNode>>?
)

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val pdlKlient: PdltjenesterKlient,
    private val vedtakKlient: EtterlatteVedtak
) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String, accessToken: String): PersonSakerResult {
        logger.info("Henter person fra behandling")

        val person = pdlKlient.hentPerson(fnr, accessToken)
        val saker = behandlingKlient.hentSakerForPerson(fnr, accessToken)

        return PersonSakerResult(person, saker)
    }

    suspend fun opprettSak(fnr: String, sakType: SoeknadType, accessToken: String): Sak {
        logger.info("Oppretter sak for en person")
        return behandlingKlient.opprettSakForPerson(fnr, sakType, accessToken)
    }

    suspend fun hentSaker(accessToken: String): Saker {
        logger.info("Henter alle saker")
        return behandlingKlient.hentSaker(accessToken)
    }

    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe {
        logger.info("Henter behandlinger for sak $sakId")
        return behandlingKlient.hentBehandlingerForSak(sakId, accessToken)
    }

    suspend fun hentBehandling(behandlingId: String, accessToken: String) = coroutineScope {
        logger.info("Henter behandling")
        behandlingKlient.hentBehandling(behandlingId, accessToken).let { behandling ->
            val vedtak = async { vedtakKlient.hentVedtak(behandling.sak.toInt(), behandlingId, accessToken) }
            val hendelser = async { behandlingKlient.hentHendelserForBehandling(behandling.id.toString(), accessToken) }
            DetaljertBehandlingDto(
                id = behandling.id,
                sak = behandling.sak,
                gyldighetsprøving = behandling.gyldighetsproeving,
                kommerSoekerTilgode = vedtak.await().kommerSoekerTilgodeResultat,
                vilkårsprøving = vedtak.await().vilkaarsResultat,
                beregning = vedtak.await().beregningsResultat,
                avkortning = vedtak.await().avkortingsResultat,
                saksbehandlerId = vedtak.await().saksbehandlerId,
                fastsatt = vedtak.await().vedtakFattet,
                datoFattet = vedtak.await().datoFattet,
                datoattestert = vedtak.await().datoattestert,
                attestant = vedtak.await().attestant,
                soeknadMottattDato = behandling.soeknadMottattDato,
                virkningstidspunkt = vedtak.await().virkningsDato,
                status = behandling.status,
                vedtakhendelser = hendelser.await().hendelser,
            )
        }
    }

    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag {
        logger.info("Opprett en behandling på en sak")
        return behandlingKlient.opprettBehandling(behandlingsBehov, accessToken)
    }

    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        return behandlingKlient.slettBehandlinger(sakId, accessToken)
    }

    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretVedtakHendelser {
        logger.info("Henter hendelser for behandling $behandlingId")
        return behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken)
    }

}