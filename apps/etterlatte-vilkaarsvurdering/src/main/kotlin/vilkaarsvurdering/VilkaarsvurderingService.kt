package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
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
    private val grunnlagKlient: GrunnlagKlient
) {
    private val logger = LoggerFactory.getLogger(VilkaarsvurderingService::class.java)

    suspend fun hentEllerOpprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        val vilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)
        return if (vilkaarsvurdering != null) {
            logger.info("Vilkårsvurdering finnes for behandling $behandlingId")
            vilkaarsvurdering
        } else {
            logger.info("Ny vilkårsvurdering opprettes for behandling $behandlingId")
            opprettVilkaarsvurdering(behandlingId, accessToken)
        }
    }

    fun oppdaterTotalVurdering(behandlingId: UUID, resultat: VilkaarsvurderingResultat): Vilkaarsvurdering =
        vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(behandlingId, resultat)

    fun slettTotalVurdering(behandlingId: UUID): Vilkaarsvurdering =
        vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)

    fun oppdaterVurderingPaaVilkaar(behandlingId: UUID, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering =
        vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)

    fun slettVurderingPaaVilkaar(behandlingId: UUID, vilkaarId: UUID): Vilkaarsvurdering =
        vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)

    private suspend fun opprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering {
        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken)
        val sakType = SakType.BARNEPENSJON // TODO: SOS, Hardkodet - https://jira.adeo.no/browse/EY-1300
        val behandlingType = requireNotNull(behandling.behandlingType)
        val virkningstidspunkt = behandling.virkningstidspunkt
            ?: throw VirkningstidspunktIkkeSattException("Virkningstidspunkt ikke satt for behandling $behandlingId")

        logger.info("Oppretter vilkårsvurdering med sakType $sakType og behandlingType $behandlingType")

        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                            Vilkaarsvurdering(
                                sakId = behandling.sak,
                                behandlingId = behandlingId,
                                vilkaar = BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt),
                                virkningstidspunkt = virkningstidspunkt.dato,
                                grunnlagVersjon = grunnlag.metadata.versjon
                            )
                        )
                    BehandlingType.REVURDERING ->
                        vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                            Vilkaarsvurdering(
                                sakId = behandling.sak,
                                behandlingId = behandlingId,
                                vilkaar = mapVilkaarRevurdering(requireNotNull(behandling.revurderingsaarsak)),
                                virkningstidspunkt = virkningstidspunkt.dato,
                                grunnlagVersjon = grunnlag.metadata.versjon
                            )
                        )
                    else ->
                        throw IllegalArgumentException(
                            "Støtter ikke vilkårsvurdering for behandlingType=${behandling.behandlingType}"
                        )
                }
            SakType.OMSTILLINGSSTOENAD ->
                throw IllegalArgumentException("Støtter ikke vilkårsvurdering for sakType=$sakType")
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