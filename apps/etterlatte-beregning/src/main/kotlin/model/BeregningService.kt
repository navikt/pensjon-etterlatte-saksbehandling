package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import model.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.*
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


class BeregningService {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)


    fun beregnResultat(opplysninger: List<VilkaarOpplysning<ObjectNode>>, virkFOM: LocalDate): BeregningsResultat {
        logger.info("Leverer en fake beregning")


        //TODO lage beregningsperioder basert på grunnbeløpsperioder
        val beregningsperioder = finnBeregningsperioder(virkFOM)


        return BeregningsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = beregningsperioder,
            beregnetDato = LocalDateTime.now()
        )
    }

    private fun finnBeregningsperioder(virkFOM: LocalDate): List<Beregningsperiode> {
        val grunnbeloep = Grunnbeloep.hentGforPeriode(virkFOM)
        val perioder = grunnbeloep.map { Pair(it.dato, Grunnbeloep.beregnTom(it)) }


        return perioder.map {
            (Beregningsperiode(
                delytelsesId = "BP",
                type = Beregningstyper.GP,
                datoFOM = beregnFoersteFom(it.first,virkFOM).atStartOfDay(),
                datoTOM = it.second?.atStartOfDay(),
                belop = Grunnbeloep.hentGjeldendeG(it.first).grunnbeløpPerMåned
            ))
        }
    }
    private fun beregnFoersteFom (first: LocalDate, virkFOM: LocalDate ): LocalDate {
        if(first.isBefore(virkFOM)) {
            return virkFOM
        } else {
            return first
        }
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

