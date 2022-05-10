package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.beregning.*
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*


class BeregningService {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    fun beregnResultat(opplysninger: List<VilkaarOpplysning<ObjectNode>>): BeregningsResultat {
        logger.info("Leverer en fake beregning")

        //TODO faktisk beregne noe
        //val fiktivberegning = finnOpplysning<AvdoedSoeknad>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)


        return BeregningsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = listOf(
                Beregningsperiode(
                delytelsesId = "First",
                type = Beregningstyper.GP,
                datoFOM = LocalDateTime.of(2022,2,2,0,1),
                datoTOM = LocalDateTime.of(2030,1,4,0,1),
                belop = 1000),
                Beregningsperiode(
                    delytelsesId = "First",
                    type = Beregningstyper.GP,
                    datoFOM = LocalDateTime.of(2030,2,1,0,1),
                    datoTOM = LocalDateTime.of(2035,1,4,0,1),
                    belop = 1001)
                ),
            beregnetDato = LocalDateTime.now()
            )
    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T>? {
            return opplysning?.let {
                VilkaarOpplysning(
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

