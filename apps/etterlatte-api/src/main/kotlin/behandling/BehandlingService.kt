package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.saksbehandling.api.typer.klientside.DetaljertBehandlingDto
import no.nav.etterlatte.saksbehandling.api.typer.klientside.Familieforhold
import no.nav.etterlatte.typer.LagretHendelser
import no.nav.etterlatte.typer.Saker
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.*

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val vedtakKlient: EtterlatteVedtak,
    private val grunnlagKlient: EtterlatteGrunnlag,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient
) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

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
        val avdoed = async { grunnlagKlient.finnPersonOpplysning(sakId, AVDOED_PDL_V1, accessToken) }
        val gjenlevende = async { grunnlagKlient.finnPersonOpplysning(sakId, GJENLEVENDE_FORELDER_PDL_V1, accessToken) }
        val soeker = async { grunnlagKlient.finnPersonOpplysning(sakId, SOEKER_PDL_V1, accessToken) }
        val beregning = async { beregningKlient.hentBeregning(UUID.fromString(behandlingId), accessToken) }
        val vilkaarsvurdering = async {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(
                UUID.fromString(behandlingId),
                accessToken
            )
        }
        DetaljertBehandlingDto(
            id = behandling.await().id,
            sak = sakId,
            gyldighetsprøving = behandling.await().gyldighetsproeving,
            kommerBarnetTilgode = behandling.await().kommerBarnetTilgode,
            vilkårsprøving = vilkaarsvurdering.await(),
            beregning = beregning.await(),
            saksbehandlerId = vedtak.await()?.saksbehandlerId,
            fastsatt = vedtak.await()?.vedtakFattet,
            datoFattet = vedtak.await()?.datoFattet,
            datoattestert = vedtak.await()?.datoattestert,
            attestant = vedtak.await()?.attestant,
            soeknadMottattDato = behandling.await().soeknadMottattDato,
            virkningstidspunkt = behandling.await().virkningstidspunkt,
            status = behandling.await().status,
            hendelser = hendelser.await().hendelser,
            familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
            behandlingType = behandling.await().behandlingType,
            søker = soeker.await()?.opplysning
        )
    }

    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser {
        logger.info("Henter hendelser for behandling $behandlingId")
        return behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken)
    }

    suspend fun opprettManueltOpphoer(
        manueltOpphoerRequest: ManueltOpphoerRequest,
        accessToken: String
    ): ManueltOpphoerResponse {
        logger.info("Oppretter manuelt opphør for sak ${manueltOpphoerRequest.sak}")
        return behandlingKlient.opprettManueltOpphoer(manueltOpphoerRequest, accessToken)
            .getOrThrow()
    }

    suspend fun fastsettVirkningstidspunkt(
        behandlingId: String,
        dato: YearMonth,
        accessToken: String
    ): VirkningstidspunktResponse {
        logger.info("Fastsetter virkningstidspunkt for behandling $behandlingId")
        return behandlingKlient.fastsettVirkningstidspunkt(behandlingId, dato, accessToken)
    }
}