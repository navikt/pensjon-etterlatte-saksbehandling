package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.typer.Sak
import no.nav.etterlatte.typer.Saker
import org.slf4j.LoggerFactory
import java.util.*


data class PersonSakerResult(val person: Person, val saker: Saker)

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Grunnlagsopplysning<ObjectNode>>?
)

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val pdlKlient: PdltjenesterKlient,
    private val grunnlagKlient: EtterlatteGrunnlag,
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

    suspend fun hentBehandling(behandlingId: String, accessToken: String): DetaljertBehandlingDto {
        logger.info("Henter behandling")
        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlagForSak(behandling.sak.toInt(), accessToken)
        val vedtak = vedtakKlient.hentVedtak(behandling.sak.toInt(), behandlingId, accessToken)
        return DetaljertBehandlingDto(
            behandling.id,
            behandling.sak,
            grunnlag,
            behandling.gyldighetsproeving,
            vedtak.vilkaarsResultat,
            null,
            false
        )

    }

    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag {
        logger.info("Opprett en behandling på en sak")
        return behandlingKlient.opprettBehandling(behandlingsBehov, accessToken)
    }

    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        return behandlingKlient.slettBehandlinger(sakId, accessToken)
    }

}

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val grunnlag: List<Grunnlagsopplysning<ObjectNode>>,
    val gyldighetsprøving: GyldighetsResultat?,
    val vilkårsprøving: VilkaarResultat?,
    val beregning: BeregningsResultat?,
    val fastsatt: Boolean = false
)