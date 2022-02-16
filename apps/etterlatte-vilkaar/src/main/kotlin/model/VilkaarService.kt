package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.vilkaar.barnepensjon.brukerErUnder20
import no.nav.etterlatte.vilkaar.barnepensjon.doedsfallErRegistrert


class VilkaarService {

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>): List<VurdertVilkaar> {
        val soekerFoedselsdato =
            opplysninger.filter { it.opplysingType == Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value }
                .map { setOpplysningType<Foedselsdato>(it) }

        val avdoedDoedsdato = opplysninger.filter { it.opplysingType == Opplysningstyper.AVDOED_DOEDSFALL_V1.value }
            .map { setOpplysningType<Doedsdato>(it) }

        val soekerRelasjonForeldre =
            opplysninger.filter { it.opplysingType == Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value }
                .map { setOpplysningType<Foreldre>(it) }

        return listOf(
            brukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20.value, soekerFoedselsdato, avdoedDoedsdato),
            doedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT.value, avdoedDoedsdato, soekerRelasjonForeldre)
        )

    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<out Any>): VilkaarOpplysning<T> {
            return VilkaarOpplysning(
                opplysning.opplysingType,
                opplysning.kilde,
                objectMapper.readValue(opplysning.opplysning.toString())
            )
        }
    }
}