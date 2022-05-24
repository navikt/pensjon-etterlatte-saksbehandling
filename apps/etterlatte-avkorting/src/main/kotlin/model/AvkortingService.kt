package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultatType
import no.nav.etterlatte.libs.common.avkorting.Avkortingsperiode
import no.nav.etterlatte.libs.common.avkorting.Endringskode
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
    fun avkortingsResultat(opplysninger: List<VilkaarOpplysning<ObjectNode>>): AvkortingsResultat {
        logger.info("Leverer en fake beregning")

        //TODO faktisk avkorte noe



        return AvkortingsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = AvkortingsResultatType.BEREGNET,
            beregningsperioder = listOf(
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
            ),
            beregnetDato = LocalDateTime.now()
        )
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

