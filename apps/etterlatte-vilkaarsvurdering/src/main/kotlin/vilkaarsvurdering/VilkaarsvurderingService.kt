package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.BarnepensjonVilkaar
import no.nav.etterlatte.vilkaarsvurdering.behandling.BehandlingKlient
import no.nav.etterlatte.vilkaarsvurdering.grunnlag.GrunnlagKlient
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.util.*

class VirkningstidspunktIkkeSattException(message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sendToRapid: (String, UUID) -> Unit
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
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = BarnepensjonVilkaar.inngangsvilkaar(grunnlag, virkningstidspunkt),
                                virkningstidspunkt = virkningstidspunkt,
                                grunnlagsmetadata = grunnlag.metadata
                            )
                        )
                    BehandlingType.REVURDERING ->
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = mapVilkaarRevurdering(requireNotNull(behandling.revurderingsaarsak)),
                                virkningstidspunkt = virkningstidspunkt,
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

    suspend fun oppdaterTotalVurdering(
        behandlingId: UUID,
        resultat: VilkaarsvurderingResultat,
        accessToken: String
    ): VilkaarsvurderingIntern {
        val oppdatertVilkaarsvurdering = vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = resultat))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")

        val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)

        publiserVilkaarsvurdering(
            vilkaarsvurdering = oppdatertVilkaarsvurdering,
            grunnlag = grunnlagKlient
                .hentGrunnlagMedVersjon(
                    oppdatertVilkaarsvurdering.grunnlagsmetadata.sakId,
                    oppdatertVilkaarsvurdering.grunnlagsmetadata.versjon,
                    accessToken
                ),
            behandling = behandling
        )

        return oppdatertVilkaarsvurdering
    }

    fun slettTotalVurdering(behandlingId: UUID): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = null))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun oppdaterVurderingPaaVilkaar(
        behandlingId: UUID,
        vurdertVilkaar: VurdertVilkaar
    ): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    oppdaterVurdering(it, vurdertVilkaar)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettVurderingPaaVilkaar(
        behandlingId: UUID,
        hovedVilkaarType: VilkaarType
    ): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    slettVurdering(it, hovedVilkaarType)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun publiserVilkaarsvurdering(
        vilkaarsvurdering: VilkaarsvurderingIntern,
        grunnlag: Grunnlag,
        behandling: DetaljertBehandling
    ) {
        // Bygger midlertidig opp en melding for å tilfredsstille beregning og vedtaksvurdering.
        val message = JsonMessage.newMessage(BehandlingGrunnlagEndret.eventName)
            .apply {
                this["sak"] = Sak(
                    behandling.soeker!!,
                    SakType.BARNEPENSJON.toString(),
                    behandling.sak
                )
            }
            .apply { this["sakId"] = behandling.sak }
            .apply { this["fnrSoeker"] = behandling.soeker!! }
            .apply {
                this["behandling"] =
                    Behandling(behandling.behandlingType!!, behandling.id)
            }
            .apply { this["vilkaarsvurdering"] = vilkaarsvurdering.toDomain() }
            .apply { this["virkningstidspunkt"] = vilkaarsvurdering.virkningstidspunkt.dato }
            .apply { this["grunnlag"] = grunnlag }

        sendToRapid(message.toJson(), vilkaarsvurdering.behandlingId)
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

    private fun oppdaterVurdering(vilkaar: Vilkaar, vurdertVilkaar: VurdertVilkaar): Vilkaar =
        if (vilkaar.hovedvilkaar.type == vurdertVilkaar.hovedvilkaar.type) {
            val hovedvilkaarOgUnntaksvilkaarIkkeOppfylt =
                vurdertVilkaar.hovedvilkaar.resultat == Utfall.IKKE_OPPFYLT && vurdertVilkaar.unntaksvilkaar == null
            vilkaar.copy(
                vurdering = vurdertVilkaar.vurdering,
                hovedvilkaar = vilkaar.hovedvilkaar.copy(resultat = vurdertVilkaar.hovedvilkaar.resultat),
                unntaksvilkaar = vilkaar.unntaksvilkaar?.map {
                    if (hovedvilkaarOgUnntaksvilkaarIkkeOppfylt) {
                        it.copy(resultat = Utfall.IKKE_OPPFYLT)
                    } else {
                        if (vurdertVilkaar.unntaksvilkaar?.type === it.type) {
                            it.copy(resultat = vurdertVilkaar.unntaksvilkaar!!.resultat)
                        } else {
                            it.copy(resultat = null)
                        }
                    }
                }
            )
        } else {
            vilkaar
        }

    private fun slettVurdering(vilkaar: Vilkaar, hovedVilkaarType: VilkaarType) =
        if (vilkaar.hovedvilkaar.type === hovedVilkaarType) {
            vilkaar.copy(
                vurdering = null,
                hovedvilkaar = vilkaar.hovedvilkaar.copy(resultat = null),
                unntaksvilkaar = vilkaar.unntaksvilkaar?.map {
                    it.copy(resultat = null)
                }
            )
        } else {
            vilkaar
        }

    override fun slettSak(sakId: Long) {
        vilkaarsvurderingRepository.slettVilkaarsvurderingerISak(sakId)
    }
}