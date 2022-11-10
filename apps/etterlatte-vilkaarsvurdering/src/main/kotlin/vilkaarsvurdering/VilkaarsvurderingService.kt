package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.Sak
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VurdertVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonFoerstegangsbehandlingVilkaar
import no.nav.etterlatte.vilkaarsvurdering.barnepensjon.barnepensjonRevurderingSoekerDoedVilkaar
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
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
        grunnlag: Grunnlag,
        revurderingAarsak: RevurderingAarsak?
    ): VilkaarsvurderingIntern {
        return when (sakType) {
            SakType.BARNEPENSJON ->
                when (behandlingType) {
                    BehandlingType.FØRSTEGANGSBEHANDLING ->
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = barnepensjonFoerstegangsbehandlingVilkaar(
                                    grunnlag,
                                    virkningstidspunkt.let {
                                        // TODO fjern dette når virkningstidspunkt kommer inn
                                        Virkningstidspunkt(
                                            dato = YearMonth.of(it.year, it.month),
                                            kilde = Grunnlagsopplysning.Saksbehandler("todo", Instant.now())
                                        )
                                    }
                                ),
                                virkningstidspunkt = virkningstidspunkt,
                                grunnlagsmetadata = grunnlag.metadata
                            )
                        )

                    BehandlingType.REVURDERING ->
                        vilkaarsvurderingRepository.lagre(
                            VilkaarsvurderingIntern(
                                behandlingId = behandlingId,
                                vilkaar = mapVilkaarRevurdering(requireNotNull(revurderingAarsak)),
                                virkningstidspunkt = virkningstidspunkt,
                                grunnlagsmetadata = grunnlag.metadata
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
            .apply { this["fnrSoeker"] = behandling.soeker!! }
            .apply {
                this["behandling"] =
                    Behandling(behandling.behandlingType!!, behandling.id)
            }
            .apply { this["vilkaarsvurdering"] = vilkaarsvurdering.toDomain() }
            .apply { this["virkningstidspunkt"] = YearMonth.from(vilkaarsvurdering.virkningstidspunkt) }
            .apply { this["grunnlag"] = grunnlag }

        sendToRapid(message.toJson(), vilkaarsvurdering.behandlingId)
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