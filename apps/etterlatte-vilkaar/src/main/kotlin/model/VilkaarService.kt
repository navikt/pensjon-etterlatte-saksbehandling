package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.barnepensjon.vilkaarAvdoedesMedlemskap
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foreldre
import no.nav.etterlatte.vilkaar.barnepensjon.*
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException


class VilkaarService {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>): List<VurdertVilkaar> {
        logger.info("Map vilkaar")
        println(opplysninger)

        val avdoedSoeknad =
            setOpplysningType<AvdoedSoeknad>(opplysninger.find { it.opplysningsType == Opplysningstyper.AVDOED_SOEKNAD_V1 })

        val soekerPdl =
            setOpplysningType<Person>(opplysninger.find { it.opplysningsType == Opplysningstyper.SOEKER_PDL_V1 })

        val avdoedPdl =
            setOpplysningType<Person>(opplysninger.find { it.opplysningsType == Opplysningstyper.AVDOED_PDL_V1 })

        val gjenlevendePdl =
            setOpplysningType<Person>(opplysninger.find { it.opplysningsType == Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 })

        return listOf(
            vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, soekerPdl, avdoedPdl),
            vilkaarDoedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT, avdoedPdl, soekerPdl ),
            vilkaarAvdoedesMedlemskap(
                Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
                avdoedSoeknad,
                avdoedPdl
            ),
            vilkaarBarnetsMedlemskap(
                Vilkaartyper.BARNETS_MEDLEMSKAP,
                soekerPdl,
                gjenlevendePdl,
                avdoedPdl,
            )
        )
    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T> {
            return if (opplysning != null) {
                VilkaarOpplysning(
                    opplysning.opplysningsType,
                    opplysning.kilde,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}