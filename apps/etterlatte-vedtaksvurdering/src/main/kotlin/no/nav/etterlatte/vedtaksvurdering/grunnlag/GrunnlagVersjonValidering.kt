package no.nav.etterlatte.vedtaksvurdering.grunnlag

import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import org.slf4j.LoggerFactory

object GrunnlagVersjonValidering {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun validerVersjon(
        vilkaarsvurdering: VilkaarsvurderingDto?,
        beregningOgAvkorting: BeregningOgAvkorting?,
        trygdetider: List<TrygdetidDto>,
        behandling: DetaljertBehandling,
    ) {
        logger.info("Sjekker at grunnlagsversjon er konsekvent på tvers av appene")
        if (vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingUtfall.IKKE_OPPFYLT &&
            behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale == false
        ) {
            logger.info(
                "Behandling ${behandling.id} skal ikke ha trygdetid ved avslag da " +
                    "avdøeds trygdeavtale ikke skal vurderes(ingen avhuking). Har trygdetid ${trygdetider.isNotEmpty()}",
            )
            // TODO: Vil vi egentlig at vedtak skal slette tt her?
        } else {
            if (trygdetider.any { it.opplysningerDifferanse.differanse }) {
                throw UlikVersjonGrunnlag(
                    "Ulik versjon av grunnlag brukt i trygdetid og behandling." +
                        "Gå til trygdetidssiden(må kanskje sette vilkårsvurderingen til innvilget) og trykk deretter " +
                        "'bruk nytt grunnlag'. ",
                )
            }
        }

        if (vilkaarsvurdering?.grunnlagVersjon == null || beregningOgAvkorting == null) {
            logger.info("Vilkaar og/eller beregning er null – fortsetter ...")
        } else if (vilkaarsvurdering.grunnlagVersjon != beregningOgAvkorting.beregning.grunnlagMetadata.versjon) {
            logger.error(
                "Ulik versjon av grunnlag i vilkaarsvurdering (versjon=${vilkaarsvurdering.grunnlagVersjon})" +
                    " og beregning (versjon=${beregningOgAvkorting.beregning.grunnlagMetadata.versjon}) ",
            )

            throw UlikVersjonGrunnlag(
                "Ulik versjon av grunnlag brukt i vilkårsvurdering og beregning. " +
                    "Gå tilbake til søknadsoversikten og trykk oppdater grunnlag, deretter må totalvurderingen " +
                    "i vilkårsvurderingen gjøres på nytt.",
            )
        } else {
            logger.info("Samsvar mellom grunnlagsversjon i vilkårsvurdering og beregning – fortsetter ...")
        }
    }
}

class UlikVersjonGrunnlag(
    detail: String,
) : UgyldigForespoerselException(
        code = "ULIK_VERSJON_GRUNNLAG",
        detail = detail,
    )
