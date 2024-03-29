package no.nav.etterlatte.vedtaksvurdering.grunnlag

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.vedtaksvurdering.BeregningOgAvkorting
import org.slf4j.LoggerFactory

object GrunnlagVersjonValidering {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun validerVersjon(
        vilkaarsvurdering: VilkaarsvurderingDto?,
        beregningOgAvkorting: BeregningOgAvkorting?,
        trygdetid: TrygdetidDto?,
    ) {
        logger.info("Sjekker at grunnlagsversjon er konsekvent på tvers av appene")

        if (trygdetid?.opplysningerDifferanse?.differanse == true) {
            throw UlikVersjonGrunnlag("Ulik versjon av grunnlag brukt i trygdetid og behandling")
        }
        if (vilkaarsvurdering?.grunnlagVersjon == null || beregningOgAvkorting == null) {
            logger.info("Vilkaar og/eller beregning er null – fortsetter ...")
        } else if (vilkaarsvurdering.grunnlagVersjon != beregningOgAvkorting.beregning.grunnlagMetadata.versjon) {
            logger.error(
                "Ulik versjon av grunnlag i vilkaarsvurdering (versjon=${vilkaarsvurdering.grunnlagVersjon})" +
                    " og beregning (versjon=${beregningOgAvkorting.beregning.grunnlagMetadata.versjon})",
            )

            throw UlikVersjonGrunnlag(
                "Ulik versjon av grunnlag brukt i vilkårsvurdering og beregning!",
            )
        } else {
            logger.info("Samsvar mellom grunnlagsversjon i vilkårsvurdering og beregning – fortsetter ...")
        }
    }
}

class UlikVersjonGrunnlag(detail: String) : UgyldigForespoerselException(
    code = "ULIK_VERSJON_GRUNNLAG",
    detail = detail,
)
