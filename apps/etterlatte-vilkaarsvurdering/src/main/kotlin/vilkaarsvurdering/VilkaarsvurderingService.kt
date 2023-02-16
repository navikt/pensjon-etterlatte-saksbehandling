package no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import org.slf4j.LoggerFactory
import java.util.*

class VirkningstidspunktIkkeSattException(message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient
) {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingService::class.java)

    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? =
        vilkaarsvurderingRepository.hent(behandlingId)

    suspend fun oppdaterTotalVurdering(
        behandlingId: UUID,
        accessToken: String,
        resultat: VilkaarsvurderingResultat
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, accessToken) {
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, accessToken).let {
            it.virkningstidspunkt?.dato?.atDay(1)
        } ?: throw IllegalStateException("Virkningstidspunkt må være satt for å sette en vurdering")
        val vilkaarsvurdering = vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
            behandlingId = behandlingId,
            virkningstidspunkt = virkningstidspunkt,
            resultat = resultat
        )
        val utfall = vilkaarsvurdering.resultat?.utfall ?: throw IllegalStateException("Utfall kan ikke vaere null")
        behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, accessToken, utfall)
        vilkaarsvurdering
    }

    suspend fun slettTotalVurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        if (behandlingKlient.settBehandlingStatusOpprettet(behandlingId, accessToken, false)) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)
            behandlingKlient.settBehandlingStatusOpprettet(behandlingId, accessToken, true)

            return vilkaarsvurdering
        }

        throw BehandlingstilstandException
    }

    suspend fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        accessToken: String,
        vurdertVilkaar: VurdertVilkaar
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, accessToken) {
        if (vilkaarsvurderingRepository.hent(behandlingId)?.resultat != null) {
            throw VilkaarsvurderingTilstandException(
                "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat"
            )
        }
        vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
    }

    suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        accessToken: String,
        vilkaarId: UUID
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, accessToken) {
        if (vilkaarsvurderingRepository.hent(behandlingId)?.resultat != null) {
            throw VilkaarsvurderingTilstandException(
                "Kan ikke slette et vilkår ($vilkaarId) på en vilkårsvurdering som har et resultat"
            )
        }

        vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
    }

    suspend fun opprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, accessToken) {
            vilkaarsvurderingRepository.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag, sak) = hentDataForVilkaarsvurdering(behandlingId, accessToken)
            val virkningstidspunkt = behandling.virkningstidspunkt
                ?: throw VirkningstidspunktIkkeSattException(
                    "Virkningstidspunkt ikke satt for behandling $behandlingId"
                )

            logger.info(
                "Oppretter vilkårsvurdering for behandling ($behandlingId) med sakType ${sak.sakType} og " +
                    "behandlingType ${behandling.behandlingType}"
            )

            val vilkaar = finnVilkaarForNyVilkaarsvurdering(behandling, sak, grunnlag, virkningstidspunkt)

            vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                Vilkaarsvurdering(
                    behandlingId = behandlingId,
                    vilkaar = vilkaar,
                    virkningstidspunkt = virkningstidspunkt.dato,
                    grunnlagVersjon = grunnlag.metadata.versjon
                )
            )
        }

    private fun finnVilkaarForNyVilkaarsvurdering(
        behandling: DetaljertBehandling,
        sak: Sak,
        grunnlag: Grunnlag,
        virkningstidspunkt: Virkningstidspunkt
    ) = when (sak.sakType) {
        SakType.BARNEPENSJON ->
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING ->
                    BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt)

                BehandlingType.REVURDERING ->
                    mapVilkaarRevurdering(requireNotNull(behandling.revurderingsaarsak))

                BehandlingType.REGULERING, BehandlingType.MANUELT_OPPHOER -> throw IllegalArgumentException(
                    "Støtter ikke vilkårsvurdering for behandlingType=${behandling.behandlingType}"
                )
            }

        SakType.OMSTILLINGSSTOENAD ->
            throw IllegalArgumentException("Støtter ikke vilkårsvurdering for sakType=${sak.sakType}")
    }

    private suspend fun tilstandssjekkFoerKjoering(
        behandlingId: UUID,
        accessToken: String,
        block: suspend () -> Vilkaarsvurdering
    ): Vilkaarsvurdering {
        val kanVilkaarsvurdere = behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(behandlingId, accessToken)

        if (!kanVilkaarsvurdere) {
            throw BehandlingstilstandException
        }

        return block()
    }

    private suspend fun hentDataForVilkaarsvurdering(
        behandlingId: UUID,
        accessToken: String
    ): Triple<DetaljertBehandling, Grunnlag, Sak> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
            val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.sak, accessToken) }
            val sak = async { behandlingKlient.hentSak(behandling.sak, accessToken) }

            Triple(behandling, grunnlag.await(), sak.await())
        }
    }

    private fun mapVilkaarRevurdering(revurderingAarsak: RevurderingAarsak): List<Vilkaar> {
        logger.info("Vilkårsvurdering har revurderingsårsak $revurderingAarsak")
        return when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> BarnepensjonVilkaar.loependevilkaar()
            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Du kan ikke ha et manuelt opphør på en revurdering"
            )

            RevurderingAarsak.GRUNNBELOEPREGULERING -> throw NotImplementedError() // TODO Mads implementer
        }
    }
}

object BehandlingstilstandException : IllegalStateException()
class VilkaarsvurderingTilstandException(message: String) : IllegalStateException(message)