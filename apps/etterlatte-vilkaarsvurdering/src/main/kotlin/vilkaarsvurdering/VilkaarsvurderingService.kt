package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonVilkaar
import java.util.*

class VilkaarsvurderingFinnesIkkeException(override val message: String) : RuntimeException(message)
class UgyldigSakTypeException(override val message: String) : RuntimeException(message)

class VilkaarsvurderingService(private val vilkaarsvurderingRepository: VilkaarsvurderingRepository) {

    fun hentVilkaarsvurdering(behandlingId: UUID): Vilkaarsvurdering? {
        return vilkaarsvurderingRepository.hent(behandlingId)
    }

    fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        sakType: SakType,
        behandlingType: BehandlingType,
        payload: JsonNode,
        grunnlag: Grunnlag
    ): Vilkaarsvurdering {
        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        vilkaarsvurderingRepository.lagre(
                            Vilkaarsvurdering(behandlingId, payload, barnepensjonVilkaar(grunnlag))
                        )
                    else ->
                        throw VilkaarsvurderingFinnesIkkeException(
                            "Støtter ikke vilkårsvurdering for behandlingType=$behandlingType"
                        )
                }
            SakType.OMSTILLINGSSTOENAD ->
                throw UgyldigSakTypeException("Støtter ikke vilkårsvurdering for sakType=$sakType")
        }
    }

    fun oppdaterVilkaarsvurderingPayload(behandlingId: UUID, payload: JsonNode): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let {
            vilkaarsvurderingRepository.lagre(it.copy(payload = payload))
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun oppdaterTotalVurdering(behandlingId: UUID, resultat: VilkaarsvurderingResultat): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = resultat))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettTotalVurdering(behandlingId: UUID): Vilkaarsvurdering {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = null))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun oppdaterVurderingPaaVilkaar(behandlingId: UUID, vurdertVilkaar: VurdertVilkaar): Vilkaarsvurdering {
        return hentVilkaarsvurdering(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    oppdaterVurdering(it, vurdertVilkaar)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettVurderingPaaVilkaar(behandlingId: UUID, hovedVilkaarType: VilkaarType): Vilkaarsvurdering {
        return hentVilkaarsvurdering(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    slettVurdering(it, hovedVilkaarType)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
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
                            it.copy(resultat = vurdertVilkaar.unntaksvilkaar.resultat)
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
}