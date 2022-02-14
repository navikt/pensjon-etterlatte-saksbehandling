package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.vilkaar.barnepensjon.brukerErUnder20


class VilkaarService {

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>) {
        val soekerFoedselsdato =
            opplysninger.filter { it.opplysingType == Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value }
                .map { setOpplysningType<Foedselsdato>(it) }
        val avdoedDoedsfall = opplysninger.filter { it.opplysingType == Opplysningstyper.AVDOED_DOEDSFALL_V1.value }
            .map { setOpplysningType<Doedsdato>(it) }

        brukerErUnder20(soekerFoedselsdato)



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