package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.GjenlevendeForelder
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Behandlingsopplysning> {

        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)!!
        val tomNode = objectMapper.createObjectNode()
        val kilde = Behandlingsopplysning.Privatperson(
            barnepensjonssoknad.innsender.foedselsnummer.value,
            barnepensjonssoknad.mottattDato.toInstant(ZoneOffset.UTC)
        )

        return listOf(
            innsender(barnepensjonssoknad),
            samtykke(barnepensjonssoknad),
            utbetalingsinformasjon(barnepensjonssoknad),
            soeker_personinfo(barnepensjonssoknad),
            soeker_statsborgerskap(barnepensjonssoknad),
            soeker_utenlandsadresse(barnepensjonssoknad),
            soeker_verge(barnepensjonssoknad),
            soeker_daglig_omsorg(barnepensjonssoknad),
            forelder_gjenlevende_personinfo(barnepensjonssoknad),
            forelder_avdoed_personinfo(barnepensjonssoknad),
            forelder_avdoed_doedsfallinformasjon(barnepensjonssoknad),
            forelder_avdoed_utenlandsopphold(barnepensjonssoknad),
            forelder_avdoed_naeringsinntekt(barnepensjonssoknad),
            forelder_avdoed_militaertjeneste(barnepensjonssoknad),
            soesken(barnepensjonssoknad),
            soeknad_mottatt_dato(barnepensjonssoknad)
        ).map { Behandlingsopplysning(UUID.randomUUID(), kilde, it.first, tomNode, it.second) }
    }

    fun innsender(barnepensjon: Barnepensjon) =
        "innsender" to objectMapper.valueToTree<ObjectNode>(barnepensjon.innsender)

    fun samtykke(barnepensjon: Barnepensjon) =
        "samtykke" to objectMapper.valueToTree<ObjectNode>(barnepensjon.harSamtykket)

    fun utbetalingsinformasjon(barnepensjon: Barnepensjon) =
        "utbetalingsinformasjon" to objectMapper.valueToTree<ObjectNode>(barnepensjon.utbetalingsInformasjon)

    fun soeker_personinfo(barnepensjon: Barnepensjon) =
        "soeker_personinfo" to objectMapper.valueToTree<ObjectNode>(
            PersonInfo(
                barnepensjon.soeker.fornavn,
                barnepensjon.soeker.etternavn,
                barnepensjon.soeker.foedselsnummer,
                PersonType.BARN
            )
        )

    fun soeker_statsborgerskap(barnepensjon: Barnepensjon) =
        "soeker_statsborgerskap" to objectMapper.valueToTree<ObjectNode>(barnepensjon.soeker.statsborgerskap)

    fun soeker_utenlandsadresse(barnepensjon: Barnepensjon) =
        "soeker_utenlandsadresse" to objectMapper.valueToTree<ObjectNode>(barnepensjon.soeker.utenlandsAdresse)

    fun soeker_verge(barnepensjon: Barnepensjon) =
        "soeker_verge" to objectMapper.valueToTree<ObjectNode>(barnepensjon.soeker.verge)

    fun soeker_daglig_omsorg(barnepensjon: Barnepensjon) =
        if (barnepensjon.soeker.dagligOmsorg != null) {
            "soeker_daglig_omsorg" to objectMapper.valueToTree(barnepensjon.soeker.dagligOmsorg)
        } else {
            "soeker_daglig_omsorg" to objectMapper.createObjectNode()
        }


    fun forelder_gjenlevende_personinfo(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.GJENLEVENDE_FORELDER) {
                return "forelder_gjenlevende_personinfo" to objectMapper.valueToTree(it as GjenlevendeForelder)
            }
        }
        return "forelder_gjenlevende_personinfo" to objectMapper.createObjectNode()
    }

    fun forelder_avdoed_personinfo(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.AVDOED) {
                val avdoed = it as Avdoed
                return "forelder_avdoed_personinfo" to objectMapper.valueToTree(
                    PersonInfo(
                        avdoed.fornavn,
                        avdoed.etternavn,
                        avdoed.foedselsnummer,
                        PersonType.AVDOED
                    )
                )
            }
        }
        return "forelder_avdoed_personinfo" to objectMapper.createObjectNode()
    }

    fun forelder_avdoed_doedsfallinformasjon(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.AVDOED) {
                val avdoed = it as Avdoed
                return "forelder_avdoed_doedsfallinformasjon" to objectMapper.valueToTree(
                    DoedsfallInformasjon(avdoed.datoForDoedsfallet, avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom)
                )
            }
        }
        return "forelder_avdoed_doedsfallinformasjon" to objectMapper.createObjectNode()
    }

    fun forelder_avdoed_utenlandsopphold(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.AVDOED) {
                val avdoed = it as Avdoed
                return "forelder_avdoed_utenlandsopphold" to objectMapper.valueToTree(
                    avdoed.utenlandsopphold
                )
            }
        }
        return "forelder_avdoed_utenlandsopphold" to objectMapper.createObjectNode()
    }

    fun forelder_avdoed_naeringsinntekt(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.AVDOED) {
                val avdoed = it as Avdoed
                return "forelder_avdoed_naeringsinntekt" to objectMapper.valueToTree(
                    avdoed.naeringsInntekt
                )
            }
        }
        return "forelder_avdoed_naeringsinntekt" to objectMapper.createObjectNode()
    }

    fun forelder_avdoed_militaertjeneste(barnepensjon: Barnepensjon): Pair<String, ObjectNode> {
        barnepensjon.foreldre.forEach {
            if (it.type == PersonType.AVDOED) {
                val avdoed = it as Avdoed
                return "forelder_avdoed_militaertjeneste" to objectMapper.valueToTree(
                    avdoed.militaertjeneste
                )
            }
        }
        return "forelder_avdoed_militaertjeneste" to objectMapper.createObjectNode()
    }

    fun soesken(barnepensjon: Barnepensjon) =
        "soesken" to objectMapper.createObjectNode()
            .set<ObjectNode>("soesken", objectMapper.valueToTree<ArrayNode>(barnepensjon.soesken))

    fun soeknad_mottatt_dato(barnepensjon: Barnepensjon) =
        "soeknad_mottatt_dato" to objectMapper.valueToTree<ObjectNode>(MottattDato(barnepensjon.mottattDato))


    data class PersonInfo(
        override val fornavn: String,
        override val etternavn: String,
        override val foedselsnummer: Foedselsnummer,
        override val type: PersonType,
    ) : Person

    data class DoedsfallInformasjon(
        val datoForDoedsfallet: Opplysning<LocalDate>,
        val doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: Opplysning<Svar>
    )

    data class MottattDato(
        val mottattDato: LocalDateTime
    )


}