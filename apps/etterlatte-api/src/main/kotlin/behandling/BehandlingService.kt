package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.saksbehandling.api.typer.klientside.DetaljertBehandlingDto
import no.nav.etterlatte.saksbehandling.api.typer.klientside.Familieforhold
import no.nav.etterlatte.typer.LagretHendelser
import no.nav.etterlatte.typer.Sak
import no.nav.etterlatte.typer.Saker
import org.slf4j.LoggerFactory


data class PersonSakerResult(val person: Person, val saker: Saker)

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Grunnlagsopplysning<ObjectNode>>?
)

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val pdlKlient: PdltjenesterKlient,
    private val vedtakKlient: EtterlatteVedtak,
    private val grunnlagKlient: EtterlatteGrunnlag
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
        val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
        val vedtak = async { vedtakKlient.hentVedtak(behandlingId, accessToken) }
        val hendelser = async { behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken) }
        val sakId = behandling.await().sak
        val avdoed = grunnlagKlient.finnOpplysning<Person>(sakId, AVDOED_PDL_V1, accessToken)
        val gjenlevende = grunnlagKlient.finnOpplysning<Person>(sakId, GJENLEVENDE_FORELDER_PDL_V1, accessToken)

        logger.info("gjenlevende: " + objectMapper.writeValueAsString(gjenlevende))
        logger.info("avdoed: " + objectMapper.writeValueAsString(avdoed))

        val familieforhold = try {
            Familieforhold(avdoed, gjenlevende)
        } catch (ex: Exception) {
            logger.error("Klarte ikke opprette familieforhold?", ex)
            null
        }

        DetaljertBehandlingDto(
            id = behandling.await().id,
            sak = sakId,
            gyldighetsprøving = behandling.await().gyldighetsproeving,
            kommerSoekerTilgode = vedtak.await().kommerSoekerTilgodeResultat,
            vilkårsprøving = vedtak.await().vilkaarsResultat,
            beregning = vedtak.await().beregningsResultat,
            avkortning = vedtak.await().avkortingsResultat,
            saksbehandlerId = vedtak.await().saksbehandlerId,
            fastsatt = vedtak.await().vedtakFattet,
            datoFattet = vedtak.await().datoFattet,
            datoattestert = vedtak.await().datoattestert,
            attestant = vedtak.await().attestant,
            soeknadMottattDato = behandling.await().soeknadMottattDato,
            virkningstidspunkt = vedtak.await().virkningsDato,
            status = behandling.await().status,
            hendelser = hendelser.await().hendelser,
            familieforhold = familieforhold
        )
    }

    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag {
        logger.info("Opprett en behandling på en sak")
        return behandlingKlient.opprettBehandling(behandlingsBehov, accessToken)
    }

    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        return behandlingKlient.slettBehandlinger(sakId, accessToken)
    }

    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser {
        logger.info("Henter hendelser for behandling $behandlingId")
        return behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken)
    }

    suspend fun slettRevurderinger(sakId: Int, accessToken: String): Boolean {
        logger.error("Ikke bruk dette i prod, slett endepunktet så raskt som mulig! Slett revurderinger er ban")
        return behandlingKlient.slettRevurderinger(sakId, accessToken)
    }

}
