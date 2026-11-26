package no.nav.etterlatte.behandling.forlengetoms

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.behandling.revurdering.RevurderingToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultat
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.util.UUID

class ForlengetOmsService(
    private val dao: ForlengetOmsDao,
    private val behandlingService: BehandlingService,
    private val revurderingService: RevurderingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentVurdering(behandlingId: UUID): ForlengetOmsVurdering? {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw IkkeFunnetException("BEHANDLING_IKKE_FUNNET", "Fant ikke behandling $behandlingId")
        validerForlengetOms(behandling)
        return dao.hentVurdering(behandlingId)
    }

    fun lagreVurdering(
        behandlingId: UUID,
        request: LagreForlengetOmsVurdering,
        brukerTokenInfo: BrukerTokenInfo,
    ): LagretForlengetOmsVurdering {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw IkkeFunnetException("BEHANDLING_IKKE_FUNNET", "Fant ikke behandling $behandlingId")
        validerForlengetOms(behandling)
        behandling.tilOpprettet()
        val vilkaarsvurdering =
            vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)
                ?: throw UgyldigForlengetOmsVurdering("Vilkårene må vurderes før vurderingen av aktivitet kan lagres.")
        if (behandling.status != BehandlingStatus.OPPRETTET || vilkaarsvurdering.resultat != null) {
            throw UgyldigForlengetOmsVurdering("Velg «Rediger vurdering» før vurderingen av aktivitet endres.")
        }
        if (vilkaarsvurdering.vilkaar.any { it.vurdering == null }) {
            throw UgyldigForlengetOmsVurdering("Alle vilkår må vurderes før vurderingen av aktivitet kan lagres.")
        }
        request.valider()
        val periode =
            if (request.utfall == ForlengetOmsUtfall.INNVILGELSE) {
                val periode =
                    request.periode
                        ?: throw UgyldigForlengetOmsVurdering("Innvilgelse krever en periode.")
                if (periode.fom != behandling.virkningstidspunkt?.dato) {
                    throw UgyldigForlengetOmsVurdering("Perioden må starte på virkningstidspunktet.")
                }
                revurderingService.lagreRevurderingInfo(
                    behandlingId,
                    RevurderingInfoMedBegrunnelse(periode, null),
                    brukerTokenInfo.ident(),
                )
                periode
            } else {
                revurderingService.slettForlengetOmsPeriode(behandlingId, brukerTokenInfo.ident())
                null
            }
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        val vurdering = dao.lagreVurdering(behandlingId, request.aktivitet, request.begrunnelse, request.utfall, kilde)
        val (oppdatertVilkaarsvurdering, grunnlagsversjon) =
            vilkaarsvurderingService.oppdaterTotalVurdering(
                behandlingId,
                brukerTokenInfo,
                VilkaarsvurderingResultat(
                    utfall =
                        when (request.utfall) {
                            ForlengetOmsUtfall.INNVILGELSE -> VilkaarsvurderingUtfall.OPPFYLT
                            ForlengetOmsUtfall.AVSLAG -> VilkaarsvurderingUtfall.IKKE_OPPFYLT
                        },
                    kommentar = request.begrunnelse,
                    tidspunkt = kilde.tidspunkt.toLocalDatetimeUTC(),
                    saksbehandler = kilde.ident,
                ),
            )
        return LagretForlengetOmsVurdering(
            vurdering = vurdering,
            vilkaarsvurdering = oppdatertVilkaarsvurdering.toDto(grunnlagsversjon),
            periode = periode,
        )
    }

    private fun validerForlengetOms(behandling: Behandling) {
        if (!featureToggleService.isEnabled(RevurderingToggle.FORLENGET_OMS, false)) {
            throw UgyldigForlengetOmsVurdering("Forlenget OMS er ikke aktivert.")
        }
        if (behandling !is Revurdering ||
            behandling.revurderingsaarsak != Revurderingaarsak.FORLENGET_OMS ||
            behandling.sak.sakType != SakType.OMSTILLINGSSTOENAD
        ) {
            throw UgyldigForlengetOmsVurdering("Vurdering av aktivitet gjelder bare revurdering for forlenget OMS.")
        }
    }
}

data class LagreForlengetOmsVurdering(
    val aktivitet: ForlengetOmsAktivitet,
    val begrunnelse: String,
    val utfall: ForlengetOmsUtfall,
    val periode: RevurderingInfo.ForlengetOms? = null,
) {
    fun valider() {
        if (begrunnelse.isBlank()) {
            throw UgyldigForlengetOmsVurdering("Oppgi en begrunnelse.")
        }
        when (val aktivitet = aktivitet) {
            is ForlengetOmsAktivitet.Utdanning -> {
                if (aktivitet.skole.isBlank() || aktivitet.studium.isBlank()) {
                    throw UgyldigForlengetOmsVurdering(
                        "Oppgi hvilken skole bruker skal studere ved og hva bruker skal studere.",
                    )
                }
                if (utfall == ForlengetOmsUtfall.INNVILGELSE && aktivitet.studererMinst50Prosent == JaNei.NEI) {
                    throw UgyldigForlengetOmsVurdering("Personen må studere minst 50 prosent for å få stønad.")
                }
            }

            is ForlengetOmsAktivitet.ArbeidsrettetTiltak -> {
                if (aktivitet.tiltak.isBlank()) {
                    throw UgyldigForlengetOmsVurdering("Oppgi hvilket tiltak bruker deltar i.")
                }
            }

            ForlengetOmsAktivitet.IngenAktivitet -> {
                if (utfall != ForlengetOmsUtfall.AVSLAG) {
                    throw UgyldigForlengetOmsVurdering(
                        "Forlenget OMS kan bare avslås når bruker verken tar utdanning eller deltar i tiltak.",
                    )
                }
            }
        }
        if (utfall == ForlengetOmsUtfall.INNVILGELSE && (periode == null || periode.tom < periode.fom)) {
            throw UgyldigForlengetOmsVurdering("Innvilgelse krever en gyldig fra og med- og til og med-måned.")
        }
    }
}

data class LagretForlengetOmsVurdering(
    val vurdering: ForlengetOmsVurdering,
    val vilkaarsvurdering: VilkaarsvurderingDto,
    val periode: RevurderingInfo.ForlengetOms?,
)

class UgyldigForlengetOmsVurdering(
    melding: String,
) : UgyldigForespoerselException("UGYLDIG_FORLENGET_OMS_VURDERING", melding)
