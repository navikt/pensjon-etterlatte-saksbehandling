package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.vilkaar.barnepensjon.*
import org.slf4j.LoggerFactory


class VilkaarService {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>): List<VurdertVilkaar> {
        logger.info("Map vilkaar")
        println(opplysninger)

        val soekerFoedselsdato =
            opplysninger.filter { it.opplysningsType == Opplysningstyper.SOEKER_FOEDSELSDATO_V1 }
                .map { setOpplysningType<Foedselsdato>(it) }

        val avdoedDoedsdato = opplysninger.filter { it.opplysningsType == Opplysningstyper.AVDOED_DOEDSFALL_V1 }
            .map { setOpplysningType<Doedsdato>(it) }

        val soekerRelasjonForeldre =
            opplysninger.filter { it.opplysningsType == Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1 }
                .map { setOpplysningType<Foreldre>(it) }

        val avdoedUtenlandsopphold =
            opplysninger.filter { it. opplysningsType == Opplysningstyper.AVDOED_UTENLANDSOPPHOLD_V1}
                .map { setOpplysningType<Utenlandsopphold>(it)}

        return listOf(
            vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, soekerFoedselsdato, avdoedDoedsdato),
            vilkaarDoedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT, avdoedDoedsdato, soekerRelasjonForeldre),
            vilkaarAvdoedesMedlemskap(Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP, avdoedUtenlandsopphold, avdoedDoedsdato)
        )

    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<out Any>): VilkaarOpplysning<T> {
            return VilkaarOpplysning(
                opplysning.opplysningsType,
                opplysning.kilde,
                objectMapper.readValue(opplysning.opplysning.toString())
            )
        }
    }
}