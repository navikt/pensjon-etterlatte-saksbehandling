package no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.kopier
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.klienter.GrunnlagKlient
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.vilkaar.OmstillingstoenadVilkaar
import org.slf4j.LoggerFactory
import java.util.*

class VirkningstidspunktIkkeSattException(behandlingId: UUID) :
    RuntimeException("Virkningstidspunkt ikke satt for behandling $behandlingId")

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
        bruker: Bruker,
        resultat: VilkaarsvurderingResultat
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, bruker) {
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, bruker).let {
            it.virkningstidspunkt?.dato?.atDay(1)
        } ?: throw IllegalStateException("Virkningstidspunkt må være satt for å sette en vurdering")
        val vilkaarsvurdering = vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
            behandlingId = behandlingId,
            virkningstidspunkt = virkningstidspunkt,
            resultat = resultat
        )
        behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
        vilkaarsvurdering
    }

    suspend fun slettTotalVurdering(behandlingId: UUID, bruker: Bruker): Vilkaarsvurdering {
        if (behandlingKlient.settBehandlingStatusOpprettet(behandlingId, bruker, false)) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)
            behandlingKlient.settBehandlingStatusOpprettet(behandlingId, bruker, true)

            return vilkaarsvurdering
        }

        throw BehandlingstilstandException
    }

    suspend fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        bruker: Bruker,
        vurdertVilkaar: VurdertVilkaar
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, bruker) {
        if (vilkaarsvurderingRepository.hent(behandlingId)?.resultat != null) {
            throw VilkaarsvurderingTilstandException(
                "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat"
            )
        }
        vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
    }

    suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        bruker: Bruker,
        vilkaarId: UUID
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, bruker) {
        if (vilkaarsvurderingRepository.hent(behandlingId)?.resultat != null) {
            throw VilkaarsvurderingTilstandException(
                "Kan ikke slette et vilkår ($vilkaarId) på en vilkårsvurdering som har et resultat"
            )
        }

        vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
    }

    suspend fun kopierVilkaarsvurdering(
        behandlingId: UUID,
        kopierFraBehandling: UUID,
        bruker: Bruker
    ): Vilkaarsvurdering {
        logger.info("Oppretter og kopierer vilkårsvurdering for $behandlingId fra $kopierFraBehandling")
        return tilstandssjekkFoerKjoering(behandlingId, bruker) {
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, bruker)
            val tidligereVilkaarsvurdering = vilkaarsvurderingRepository.hent(kopierFraBehandling)
                ?: throw NullPointerException("Fant ikke vilkårsvurdering fra behandling $kopierFraBehandling")

            val virkningstidspunkt =
                behandling.virkningstidspunkt ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            vilkaarsvurderingRepository.kopierVilkaarsvurdering(
                nyVilkaarsvurdering = Vilkaarsvurdering(
                    behandlingId = behandlingId,
                    grunnlagVersjon = grunnlag.metadata.versjon,
                    virkningstidspunkt = virkningstidspunkt.dato,
                    vilkaar = tidligereVilkaarsvurdering.vilkaar.kopier(),
                    resultat = tidligereVilkaarsvurdering.resultat
                ),
                kopiertFraId = tidligereVilkaarsvurdering.id
            ).also {
                runBlocking { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker) }
            }
        }
    }

    suspend fun opprettVilkaarsvurdering(behandlingId: UUID, bruker: Bruker): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, bruker) {
            vilkaarsvurderingRepository.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, bruker)

            val virkningstidspunkt = behandling.virkningstidspunkt
                ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            logger.info(
                "Oppretter vilkårsvurdering for behandling ($behandlingId) med sakType ${behandling.sakType} og " +
                    "behandlingType ${behandling.behandlingType}"
            )

            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    val vilkaar = finnVilkaarForNyVilkaarsvurdering(
                        grunnlag,
                        virkningstidspunkt,
                        behandling.behandlingType,
                        behandling.sakType
                    )
                    vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                        Vilkaarsvurdering(
                            behandlingId = behandlingId,
                            vilkaar = vilkaar,
                            virkningstidspunkt = virkningstidspunkt.dato,
                            grunnlagVersjon = grunnlag.metadata.versjon
                        )
                    )
                }

                BehandlingType.REVURDERING -> {
                    logger.info("Kopierer vilkårsvurdering for behandling $behandlingId fra forrige behandling")
                    val forrigeBehandling = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                    kopierVilkaarsvurdering(behandlingId, forrigeBehandling.id, bruker)
                }

                BehandlingType.MANUELT_OPPHOER -> throw RuntimeException(
                    "Støtter ikke vilkårsvurdering for behandlingType=${behandling.behandlingType}"
                )
            }
        }

    private fun finnVilkaarForNyVilkaarsvurdering(
        grunnlag: Grunnlag,
        virkningstidspunkt: Virkningstidspunkt,
        behandlingType: BehandlingType,
        sakType: SakType
    ): List<Vilkaar> = when (sakType) {
        SakType.BARNEPENSJON ->
            when (behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING,
                BehandlingType.REVURDERING -> BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt)

                BehandlingType.MANUELT_OPPHOER -> throw IllegalArgumentException(
                    "Støtter ikke vilkårsvurdering for behandlingType=$behandlingType"
                )
            }

        SakType.OMSTILLINGSSTOENAD ->
            when (behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING ->
                    OmstillingstoenadVilkaar.inngangsvilkaar()

                BehandlingType.REVURDERING,
                BehandlingType.MANUELT_OPPHOER -> throw IllegalArgumentException(
                    "Støtter ikke vilkårsvurdering for behandlingType=$behandlingType"
                )
            }
    }

    private suspend fun tilstandssjekkFoerKjoering(
        behandlingId: UUID,
        bruker: Bruker,
        block: suspend () -> Vilkaarsvurdering
    ): Vilkaarsvurdering {
        val kanVilkaarsvurdere = behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(behandlingId, bruker)

        if (!kanVilkaarsvurdere) {
            throw BehandlingstilstandException
        }

        return block()
    }

    private suspend fun hentDataForVilkaarsvurdering(
        behandlingId: UUID,
        bruker: Bruker
    ): Pair<DetaljertBehandling, Grunnlag> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.sak, bruker) }

            Pair(behandling, grunnlag.await())
        }
    }
}

object BehandlingstilstandException : IllegalStateException()
class VilkaarsvurderingTilstandException(message: String) : IllegalStateException(message)