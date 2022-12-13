package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.util.*

class VirkningstidspunktIkkeSattException(message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient
) : VedlikeholdService {

    suspend fun hentEllerOpprettVilkaarsvurdering(behandlingId: UUID, accessToken: String): VilkaarsvurderingIntern =
        vilkaarsvurderingRepository.hent(behandlingId) ?: opprettVilkaarsvurdering(behandlingId, accessToken)

    private suspend fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        accessToken: String
    ): VilkaarsvurderingIntern {
        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(behandling.sak, accessToken)
        val sakType = SakType.BARNEPENSJON // TODO: SOS, Hardkodet - https://jira.adeo.no/browse/EY-1300

        val virkningstidspunkt = behandling.virkningstidspunkt
            ?: throw VirkningstidspunktIkkeSattException("Virkningstidspunkt ikke satt for behandling $behandlingId")

        requireNotNull(behandling.behandlingType) { // TODO gir det mening at denne er optional?!
            "BehandlingType ikke satt for behandling $behandlingId"
        }

        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandling.behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt),
                                virkningstidspunkt = virkningstidspunkt.dato,
                                grunnlagsmetadata = grunnlag.metadata
                            )
                        )
                    BehandlingType.REVURDERING ->
                        vilkaarsvurderingRepository.opprettVilkaarsvurdering(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = mapVilkaarRevurdering(requireNotNull(behandling.revurderingsaarsak)),
                                virkningstidspunkt = virkningstidspunkt.dato,
                                grunnlagsmetadata = grunnlag.metadata
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

    fun oppdaterTotalVurdering(
        behandlingId: UUID,
        resultat: VilkaarsvurderingResultat
    ): VilkaarsvurderingIntern {
        val oppdatertVilkaarsvurdering =
            vilkaarsvurderingRepository.lagreVilkaarsvurderingResultat(behandlingId, resultat)
        return oppdatertVilkaarsvurdering
    }

    fun slettTotalVurdering(behandlingId: UUID): VilkaarsvurderingIntern =
        vilkaarsvurderingRepository.slettVilkaarsvurderingResultat(behandlingId)

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar
    ): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.lagreVilkaarResultat(behandlingId, vurdertVilkaar)
    }

    fun slettVurderingPaaVilkaar(behandlingId: UUID, vilkaarId: UUID): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.slettVilkaarResultat(behandlingId, vilkaarId)
    }

    private fun mapVilkaarRevurdering(
        revurderingAarsak: RevurderingAarsak
    ): List<Vilkaar> {
        return when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> BarnepensjonVilkaar.loependevilkaar()
            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Du kan ikke ha et manuelt opphør på en revurdering"
            )
        }
    }

    override fun slettSak(sakId: Long) {
        vilkaarsvurderingRepository.slettVilkaarsvurderingerISak(sakId)
    }
}