package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import java.util.*

class VirkningstidspunktIkkeSattException(message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient
) {
    suspend fun hentEllerOpprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): Vilkaarsvurdering =
        vilkaarsvurderingRepository.hent(behandlingId) ?: opprettVilkaarsvurdering(behandlingId, accessToken)

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

        val virkningstidspunkt = behandling.virkningstidspunkt
            ?: throw VirkningstidspunktIkkeSattException("Virkningstidspunkt ikke satt for behandling $behandlingId")

        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (requireNotNull(behandling.behandlingType)) {
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

    private fun mapVilkaarRevurdering(revurderingAarsak: RevurderingAarsak): List<Vilkaar> =
        when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> BarnepensjonVilkaar.loependevilkaar()
            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Du kan ikke ha et manuelt opphør på en revurdering"
            )
        }
}