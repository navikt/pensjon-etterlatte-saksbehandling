package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonFoerstegangsbehandlingVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonRevurderingSoekerDoedVilkaar
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.time.LocalDate
import java.util.*

class VilkaarsvurderingFinnesIkkeException(override val message: String) : RuntimeException(message)
class UgyldigSakTypeException(override val message: String) : RuntimeException(message)

class VilkaarsvurderingService(
    private val vilkaarsvurderingRepository: VilkaarsvurderingRepository,
    private val sendToRapid: (String, UUID) -> Unit
) {

    fun hentVilkaarsvurdering(behandlingId: UUID): VilkaarsvurderingIntern? {
        return vilkaarsvurderingRepository.hent(behandlingId)
    }

    private fun mapVilkaarRevurdering(
        revurderingAarsak: RevurderingAarsak
    ): List<Vilkaar> {
        return when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> barnepensjonRevurderingSoekerDoedVilkaar()
            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Du kan ikke ha et manuelt opphør på en revurdering"
            )
        }
    }

    fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        sakType: SakType,
        behandlingType: BehandlingType,
        virkningstidspunkt: LocalDate,
        payload: JsonNode,
        grunnlag: Grunnlag,
        revurderingAarsak: RevurderingAarsak?
    ): VilkaarsvurderingIntern {
        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId,
                                payload,
                                barnepensjonFoerstegangsbehandlingVilkaar(grunnlag),
                                virkningstidspunkt
                            )
                        )

                    BehandlingType.REVURDERING ->
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId,
                                payload,
                                mapVilkaarRevurdering(requireNotNull(revurderingAarsak)),
                                virkningstidspunkt
                            )
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

    fun oppdaterVilkaarsvurderingPayload(
        behandlingId: UUID,
        payload: JsonNode,
        virkningstidspunkt: LocalDate
    ): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let {
            vilkaarsvurderingRepository.lagre(it.copy(payload = payload, virkningstidspunkt = virkningstidspunkt))
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun oppdaterTotalVurdering(behandlingId: UUID, resultat: VilkaarsvurderingResultat): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = resultat))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettTotalVurdering(behandlingId: UUID): VilkaarsvurderingIntern {
        return vilkaarsvurderingRepository.hent(behandlingId)?.let { vilkaarsvurdering ->
            vilkaarsvurderingRepository.lagre(vilkaarsvurdering.copy(resultat = null))
        } ?: throw RuntimeException("Fant ikke vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun oppdaterVurderingPaaVilkaar(behandlingId: UUID, vurdertVilkaar: VurdertVilkaar): VilkaarsvurderingIntern {
        return hentVilkaarsvurdering(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    oppdaterVurdering(it, vurdertVilkaar)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun slettVurderingPaaVilkaar(behandlingId: UUID, hovedVilkaarType: VilkaarType): VilkaarsvurderingIntern {
        return hentVilkaarsvurdering(behandlingId)?.let { vilkaarsvurdering ->
            val oppdatertVilkaarsvurdering = vilkaarsvurdering.copy(
                vilkaar = vilkaarsvurdering.vilkaar.map {
                    slettVurdering(it, hovedVilkaarType)
                }
            )
            vilkaarsvurderingRepository.lagre(oppdatertVilkaarsvurdering)
        } ?: throw VilkaarsvurderingFinnesIkkeException("Fant ingen vilkårsvurdering for behandlingId=$behandlingId")
    }

    fun publiserVilkaarsvurdering(vilkaarsvurdering: VilkaarsvurderingIntern) {
        val oppdatertPayload = JsonMessage(vilkaarsvurdering.payload.toJson(), MessageProblems("{}"), null)
            .apply { this["vilkaarsvurdering"] = vilkaarsvurdering.toDomain() }

        sendToRapid(oppdatertPayload.toJson(), vilkaarsvurdering.behandlingId)
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
}