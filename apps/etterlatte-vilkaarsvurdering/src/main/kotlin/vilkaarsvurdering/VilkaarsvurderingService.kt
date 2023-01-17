package no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import org.slf4j.LoggerFactory
import java.util.*

class VirkningstidspunktIkkeSattException(message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sessionFactory: SessionFactory
) {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingService::class.java)

    suspend fun hentEllerOpprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)

        return if (vilkaarsvurdering != null) {
            logger.info("Vilkårsvurdering finnes for behandling $behandlingId")
            vilkaarsvurdering
        } else {
            tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
                logger.info("Ny vilkårsvurdering opprettes for behandling $behandlingId")
                opprettVilkaarsvurdering(behandlingId, accessToken)
            }
        }
    }

    suspend fun oppdaterTotalVurdering(
        behandlingId: UUID,
        accessToken: String,
        resultat: VilkaarsvurderingResultat
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
        val vilkaarsvurdering = sessionFactory.withTransactionalSession { tx ->
            val vilkaarsvurdering =
                vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(behandlingId, resultat, tx)
            val utfall = vilkaarsvurdering.resultat?.utfall ?: throw IllegalStateException("Utfall kan ikke vaere null")
            runBlocking {
                if (!behandlingKlient.commitVilkaarsvurdering(behandlingId, accessToken, utfall)) {
                    logger.error(
                        "Endring av behandlingsstatus feilet under oppdatering av vilkaarsvurdering " +
                            "for behandling: $behandlingId"
                    )
                    throw RuntimeException("Endring av behandlingsstatus feilet under oppdatering av vilkaarsvurdering")
                }
            }

            vilkaarsvurdering
        }

        vilkaarsvurdering
    }

    suspend fun slettTotalVurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        if (behandlingKlient.opprett(behandlingId, accessToken, false)) {
            val vilkaarsvurdering = sessionFactory.withTransactionalSession { tx ->
                val vilkaarsvurdering = vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId, tx)
                runBlocking {
                    if (!behandlingKlient.opprett(behandlingId, accessToken, true)) {
                        logger.error(
                            "Endring av behandlingsstatus feilet under sletting av vilkaarsvurdering " +
                                "for behandling: $behandlingId"
                        )
                        throw RuntimeException(
                            "Endring av behandlingsstatus feilet under oppdatering av vilkaarsvurdering"
                        )
                    }
                }

                vilkaarsvurdering
            }

            return vilkaarsvurdering
        }

        throw BehandlingstilstandException
    }

    suspend fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        accessToken: String,
        vurdertVilkaar: VurdertVilkaar
    ): Vilkaarsvurdering =
        tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
        }

    suspend fun slettVurderingPaaVilkaar(behandlingId: UUID, accessToken: String, vilkaarId: UUID): Vilkaarsvurdering =
        tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
        }

    private suspend fun opprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        val (behandling, grunnlag, sak) = hentDataForVilkaarsvurdering(behandlingId, accessToken)

        val sakType = sak.sakType
        val behandlingType = requireNotNull(behandling.behandlingType)
        val virkningstidspunkt = behandling.virkningstidspunkt
            ?: throw VirkningstidspunktIkkeSattException("Virkningstidspunkt ikke satt for behandling $behandlingId")

        logger.info("Oppretter vilkårsvurdering med sakType $sakType og behandlingType $behandlingType")

        val vilkaar = when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt)

                    BehandlingType.REVURDERING ->
                        mapVilkaarRevurdering(requireNotNull(behandling.revurderingsaarsak))

                    else -> throw IllegalArgumentException(
                        "Støtter ikke vilkårsvurdering for behandlingType=${behandling.behandlingType}"
                    )
                }

            SakType.OMSTILLINGSSTOENAD ->
                throw IllegalArgumentException("Støtter ikke vilkårsvurdering for sakType=$sakType")
        }

        return vilkaarsvurderingRepository.opprettVilkaarsvurdering(
            Vilkaarsvurdering(
                behandlingId = behandlingId,
                vilkaar = vilkaar,
                virkningstidspunkt = virkningstidspunkt.dato,
                grunnlagVersjon = grunnlag.metadata.versjon
            )
        )
    }

    private suspend fun tilstandssjekkFoerKjoerning(
        behandlingId: UUID,
        accessToken: String,
        block: suspend () -> Vilkaarsvurdering
    ): Vilkaarsvurdering {
        val kanVilkaarsvurdere = behandlingKlient.testVilkaarsvurderingState(behandlingId, accessToken)

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
        }
    }
}

object BehandlingstilstandException : IllegalStateException()