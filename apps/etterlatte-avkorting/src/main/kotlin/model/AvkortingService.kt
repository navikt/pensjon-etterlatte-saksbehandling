package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.avkorting.*
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*


class AvkortingService {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    fun avkortingsResultat(beregningsResultat: BeregningsResultat): AvkortingsResultat {
        logger.info("Leverer en fake beregning")
        val avkortingRes = AvkortingsResultat(
            //TODO se på hvordan denne initieres
            id = UUID.randomUUID(),
            type = beregningsResultat.type,
            endringskode = Endringskode.NY,
            resultat = AvkortingsResultatType.BEREGNET,
            beregningsperioder = avkortPerioder(beregningsResultat.beregningsperioder),
            beregnetDato = LocalDateTime.now()
        )
        return avkortingRes
    }
    //TODO implementere avkortning (må ha inn inntekt)
    fun beregnAvkorting(belop: Int): Int {
        return belop - 5
    }

    fun avkortPerioder(beregningsperioder: List<Beregningsperiode>): List<Avkortingsperiode> {
        var i = 1
        var avkortingsperioder: List<Avkortingsperiode> = emptyList()
        for (periode in beregningsperioder) {
            avkortingsperioder = avkortingsperioder + Avkortingsperiode(
                avkortingsId = i++.toString(),
                type = Avkortingstyper.INNTEKT,
                datoFOM = periode.datoFOM,
                datoTOM = periode.datoTOM,
                belop = beregnAvkorting(periode.belop)
            )
        }
        return avkortingsperioder
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

