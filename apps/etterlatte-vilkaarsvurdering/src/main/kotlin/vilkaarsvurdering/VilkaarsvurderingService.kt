package no.nav.etterlatte.vilkaarsvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.kopier
import no.nav.etterlatte.token.BrukerTokenInfo
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
        brukerTokenInfo: BrukerTokenInfo,
        resultat: VilkaarsvurderingResultat
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).let {
            it.virkningstidspunkt?.dato?.atDay(1)
        } ?: throw IllegalStateException("Virkningstidspunkt må være satt for å sette en vurdering")
        val vilkaarsvurdering = vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(
            behandlingId = behandlingId,
            virkningstidspunkt = virkningstidspunkt,
            resultat = resultat
        )
        behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)
        vilkaarsvurdering
    }

    suspend fun slettTotalVurdering(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Vilkaarsvurdering {
        if (behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, false)) {
            val vilkaarsvurdering = vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)
            behandlingKlient.settBehandlingStatusOpprettet(behandlingId, brukerTokenInfo, true)

            return vilkaarsvurdering
        }

        throw BehandlingstilstandException
    }

    suspend fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vurdertVilkaar: VurdertVilkaar
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
        val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)
        val vilkaarTilVurdering =
            vilkaarsvurdering?.vilkaar?.first { it.hovedvilkaar.type == vurdertVilkaar.hovedvilkaar.type }
                ?: throw Exception("Fant ikke angitt vilkår (${vurdertVilkaar.vilkaarId})")

        if (vilkaarsvurdering.resultat != null) {
            throw VilkaarsvurderingTilstandException(
                "Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) på en vilkårsvurdering som har et resultat"
            )
        }

        if (vilkaarTilVurdering.vilkaarErKopiert()) {
            throw Exception("Kan ikke endre et vilkår (${vurdertVilkaar.vilkaarId}) som er kopiert")
        }

        vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
    }

    suspend fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vilkaarId: UUID
    ): Vilkaarsvurdering = tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
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
        brukerTokenInfo: BrukerTokenInfo
    ): Vilkaarsvurdering {
        logger.info("Oppretter og kopierer vilkårsvurdering for $behandlingId fra $kopierFraBehandling")
        return tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
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
                runBlocking { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo) }
            }
        }
    }

    suspend fun opprettVilkaarsvurdering(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Vilkaarsvurdering =
        tilstandssjekkFoerKjoering(behandlingId, brukerTokenInfo) {
            vilkaarsvurderingRepository.hent(behandlingId)?.let {
                throw IllegalArgumentException("Vilkårsvurdering finnes allerede for behandling $behandlingId")
            }

            val (behandling, grunnlag) = hentDataForVilkaarsvurdering(behandlingId, brukerTokenInfo)
            val virkningstidspunkt = behandling.virkningstidspunkt
                ?: throw VirkningstidspunktIkkeSattException(behandlingId)

            logger.info(
                "Oppretter vilkårsvurdering for behandling ($behandlingId) med sakType ${behandling.sakType} og " +
                    "behandlingType ${behandling.behandlingType}"
            )

            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                        Vilkaarsvurdering(
                            behandlingId = behandlingId,
                            vilkaar = vilkaarFoerstegangsbehandling(
                                grunnlag,
                                behandling.sakType
                            ),
                            virkningstidspunkt = virkningstidspunkt.dato,
                            grunnlagVersjon = grunnlag.metadata.versjon
                        )
                    )
                }

                BehandlingType.REVURDERING -> {
                    logger.info("Kopierer vilkårsvurdering for behandling $behandlingId fra forrige behandling")
                    val sisteIverksatteBehandling = behandlingKlient.hentSisteIverksatteBehandling(
                        behandling.sak,
                        brukerTokenInfo
                    )

                    val forrigeVilkaarsvurdering = vilkaarsvurderingRepository.hent(sisteIverksatteBehandling.id)
                        ?: throw NullPointerException(
                            "Fant ikke vilkårsvurdering fra behandling ${sisteIverksatteBehandling.id}"
                        )

                    val vilkaar = hentVilkaarForRevurdering(
                        behandling.sakType,
                        behandling.revurderingsaarsak!!,
                        grunnlag,
                        forrigeVilkaarsvurdering.vilkaar.kopier()
                    )

                    val ingenVilkaarTilVurdering = vilkaar.all { it.vilkaarErKopiert() }

                    val vilkaarsvurdering = vilkaarsvurderingRepository.kopierVilkaarsvurdering(
                        nyVilkaarsvurdering = Vilkaarsvurdering(
                            behandlingId = behandlingId,
                            vilkaar = vilkaar,
                            virkningstidspunkt = virkningstidspunkt.dato,
                            grunnlagVersjon = grunnlag.metadata.versjon,
                            resultat = if (ingenVilkaarTilVurdering) forrigeVilkaarsvurdering.resultat else null
                        ),
                        kopiertFraId = forrigeVilkaarsvurdering.id
                    )

                    if (ingenVilkaarTilVurdering) {
                        runBlocking {
                            behandlingKlient.settBehandlingStatusVilkaarsvurdert(
                                behandlingId,
                                brukerTokenInfo
                            )
                        }
                    }

                    vilkaarsvurdering
                }

                BehandlingType.MANUELT_OPPHOER -> throw RuntimeException(
                    "Støtter ikke vilkårsvurdering for behandlingType=${behandling.behandlingType}"
                )
            }
        }

    private fun hentVilkaarForRevurdering(
        sakType: SakType,
        revurderingsaarsak: RevurderingAarsak,
        grunnlag: Grunnlag,
        vilkaarForrigeBehandling: List<Vilkaar>
    ): List<Vilkaar> = sammenslaaVilkaar(
        vilkaarForrigeBehandling = vilkaarForrigeBehandling,
        vilkaarForRevurdering = when (sakType) {
            SakType.OMSTILLINGSSTOENAD -> OmstillingstoenadVilkaar.loependeVilkaarForRevurdering(
                grunnlag,
                revurderingsaarsak
            )
            SakType.BARNEPENSJON -> BarnepensjonVilkaar.vilkaarForRevurdering(grunnlag, revurderingsaarsak)
        }
    )

    /**
     * Slår sammen tidligere vurderte vilkår og nye vilkår. Dersom hovedvilkårtype er lik,
     * foretrekkes nye vilkår.
     */
    private fun sammenslaaVilkaar(
        vilkaarForrigeBehandling: List<Vilkaar>,
        vilkaarForRevurdering: List<Vilkaar>
    ): List<Vilkaar> {
        return (vilkaarForrigeBehandling + vilkaarForRevurdering)
            .groupBy { it.hovedvilkaar.type }
            .mapValues { (_, values) -> if (values.size > 1) values.filterNot { it.vilkaarErKopiert() } else values }
            .flatMap { it.value }
    }

    private fun vilkaarFoerstegangsbehandling(
        grunnlag: Grunnlag,
        sakType: SakType
    ): List<Vilkaar> = when (sakType) {
        SakType.BARNEPENSJON ->
            BarnepensjonVilkaar.inngangsvilkaar(grunnlag)
        SakType.OMSTILLINGSSTOENAD ->
            OmstillingstoenadVilkaar.inngangsvilkaar(grunnlag)
    }

    private suspend fun tilstandssjekkFoerKjoering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> Vilkaarsvurdering
    ): Vilkaarsvurdering {
        val kanVilkaarsvurdere = behandlingKlient.kanSetteBehandlingStatusVilkaarsvurdert(behandlingId, brukerTokenInfo)

        if (!kanVilkaarsvurdere) {
            throw BehandlingstilstandException
        }

        return block()
    }

    private suspend fun hentDataForVilkaarsvurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Pair<DetaljertBehandling, Grunnlag> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            val grunnlag = async { grunnlagKlient.hentGrunnlag(behandling.sak, brukerTokenInfo) }

            Pair(behandling, grunnlag.await())
        }
    }
}

object BehandlingstilstandException : IllegalStateException()
class VilkaarsvurderingTilstandException(message: String) : IllegalStateException(message)