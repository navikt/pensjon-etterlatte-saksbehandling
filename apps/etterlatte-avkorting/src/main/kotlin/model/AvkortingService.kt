package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.avkorting.*
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*


class AvkortingService {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    //TODO byttes til å være basert på beregning
    fun avkortingsResultat(beregningsResultat: BeregningsResultat): AvkortingsResultat {
        logger.info("Leverer en fake beregning")
        val avkortingRes = AvkortingsResultat(
            //TODO se på hvordan denne initieres
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = AvkortingsResultatType.BEREGNET,
            beregningsperioder = emptyList<Avkortingsperiode>(),
            /*
            listOf(
                Avkortingsperiode(
                    avkortingsId = "First",
                    type = Beregningstyper.GP,
                    datoFOM = LocalDateTime.of(2022, 2, 2, 0, 1),
                    datoTOM = LocalDateTime.of(2030, 1, 4, 0, 1),
                    belop = 1000
                ),
                Avkortingsperiode(
                    avkortingsId = "First",
                    type = Beregningstyper.GP,
                    datoFOM = LocalDateTime.of(2030, 2, 1, 0, 1),
                    datoTOM = LocalDateTime.of(2035, 1, 4, 0, 1),
                    belop = 1001
                )
            )
            ,
             */
            beregnetDato = LocalDateTime.now()
        )

        //TODO faktisk avkorte noe
        val beregningsperioder = beregningsResultat.beregningsperioder
        var i = 1
        for (periode in beregningsperioder) {
            avkortingRes.beregningsperioder.plus(Avkortingsperiode(
                avkortingsId = i++.toString(),
                type = Avkortingstyper.INNTEKT,
                datoFOM = periode.datoFOM,
                datoTOM = periode.datoTOM,
                belop = beregnAvkorting(periode.belop)
            ))

        }
        return avkortingRes
    }
    //TODO implementere avkortning (må ha inn inntekt)
    fun beregnAvkorting(belop: Int): Int {
        return belop - 5
    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T>? {
            return opplysning?.let {
                VilkaarOpplysning(
                    opplysning.id,
                    opplysning.opplysningType,
                    opplysning.kilde,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            }
        }

        inline fun <reified T> finnOpplysning(
            opplysninger: List<VilkaarOpplysning<ObjectNode>>,
            type: Opplysningstyper
        ): VilkaarOpplysning<T>? {
            return setOpplysningType(opplysninger.find { it.opplysningType == type })
        }
    }
}

