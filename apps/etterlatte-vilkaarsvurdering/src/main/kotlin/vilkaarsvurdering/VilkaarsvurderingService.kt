package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
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
        payload: String,
        grunnlag: Opplysningsgrunnlag
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

    fun oppdaterVilkaarsvurderingPayload(behandlingId: UUID, payload: String): Vilkaarsvurdering {
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
                    val oppdatertVilkaar = oppdaterVurdering(it, vurdertVilkaar)
                    oppdatertVilkaar.copy(
                        unntaksvilkaar = it.unntaksvilkaar?.map { unntaksvilkaar ->
                            oppdaterVurdering(unntaksvilkaar, vurdertVilkaar)
                        }
                    )
                }
            )
            vilkaarsvurderingRepository.oppdater(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettVurderingPaaVilkaar(behandlingId: UUID, vilkaarType: VilkaarType): Vilkaarsvurdering {
        return hentVilkaarsvurdering(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    val oppdatertVilkaar = slettVurdering(it, vilkaarType)
                    oppdatertVilkaar.copy(
                        unntaksvilkaar = it.unntaksvilkaar?.map { unntaksvilkaar ->
                            slettVurdering(unntaksvilkaar, vilkaarType)
                        }
                    )
                }
            )
            vilkaarsvurderingRepository.oppdater(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
    }

    private fun oppdaterVurdering(vilkaar: Vilkaar, vurdertVilkaar: VurdertVilkaar) =
        if (vilkaar.type == vurdertVilkaar.vilkaarType) {
            vilkaar.copy(vurdering = vurdertVilkaar.vurdertResultat)
        } else {
            vilkaar
        }

    private fun slettVurdering(vilkaar: Vilkaar, vilkaarType: VilkaarType) =
        if (vilkaar.type == vilkaarType) {
            vilkaar.copy(vurdering = null)
        } else {
            vilkaar
        }
}