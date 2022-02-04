package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Behandlingsopplysning<out Any>> {

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
        ).filter{it.second != null}
            .map { Behandlingsopplysning(UUID.randomUUID(), kilde, it.first, tomNode, it.second!!) } +
                listOf<Behandlingsopplysning<out Any>?>(
                    doedsdatoForAvdoed(barnepensjonssoknad)
                ).filterNotNull()
    }

    fun innsender(barnepensjon: Barnepensjon) =
        "innsender" to barnepensjon.innsender

    fun samtykke(barnepensjon: Barnepensjon) =
        "samtykke" to barnepensjon.harSamtykket

    fun utbetalingsinformasjon(barnepensjon: Barnepensjon) =
        "utbetalingsinformasjon" to barnepensjon.utbetalingsInformasjon

    fun soeker_personinfo(barnepensjon: Barnepensjon) =
        "soeker_personinfo" to
            PersonInfo(
                barnepensjon.soeker.fornavn,
                barnepensjon.soeker.etternavn,
                barnepensjon.soeker.foedselsnummer,
                PersonType.BARN
            )


    fun soeker_statsborgerskap(barnepensjon: Barnepensjon) =
        "soeker_statsborgerskap" to barnepensjon.soeker.statsborgerskap

    fun soeker_utenlandsadresse(barnepensjon: Barnepensjon) =
        "soeker_utenlandsadresse" to barnepensjon.soeker.utenlandsAdresse

    fun soeker_verge(barnepensjon: Barnepensjon) =
        "soeker_verge" to barnepensjon.soeker.verge

    fun soeker_daglig_omsorg(barnepensjon: Barnepensjon) =
        "soeker_daglig_omsorg" to barnepensjon.soeker.dagligOmsorg



    fun forelder_gjenlevende_personinfo(barnepensjon: Barnepensjon) =
        "forelder_gjenlevende_personinfo" to barnepensjon.foreldre.find { it.type == PersonType.GJENLEVENDE_FORELDER }

    fun forelder_avdoed_personinfo(barnepensjon: Barnepensjon): Pair<String, PersonInfo?> {
        return "forelder_avdoed_personinfo" to barnepensjon.foreldre
            .find { it.type == PersonType.AVDOED }
            ?.let {
                PersonInfo(
                    it.fornavn,
                    it.etternavn,
                    it.foedselsnummer,
                    PersonType.AVDOED
                )
            }
    }

    fun forelder_avdoed_doedsfallinformasjon(barnepensjon: Barnepensjon): Pair<String, DoedsfallInformasjon?> {
        return "forelder_avdoed_doedsfallinformasjon" to  barnepensjon.foreldre.find { it.type == PersonType.AVDOED}
            ?.let{
                val avdoed = it as Avdoed
                DoedsfallInformasjon(avdoed.datoForDoedsfallet, avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom)
            }
    }

    fun doedsdatoForAvdoed(barnepensjon: Barnepensjon): Behandlingsopplysning<Doedsdato>? {
        return barnepensjon.foreldre.find { it.type == PersonType.AVDOED}
            ?.let{
                val avdoed = it as Avdoed
                Behandlingsopplysning(UUID.randomUUID(),Behandlingsopplysning.Privatperson(
                    barnepensjon.innsender.foedselsnummer.value,
                    barnepensjon.mottattDato.toInstant(ZoneOffset.UTC)
                ),"doedsfall:v1", objectMapper.createObjectNode() ,
                    Doedsdato(
                        avdoed.datoForDoedsfallet.svar,
                        avdoed.foedselsnummer.value
                    )
                )
            }
    }

    fun forelder_avdoed_utenlandsopphold(barnepensjon: Barnepensjon): Pair<String, BetingetOpplysning<Svar, List<Utenlandsopphold>>?> {
        return "forelder_avdoed_utenlandsopphold" to  barnepensjon.foreldre
            .find { it.type == PersonType.AVDOED }
            ?.let { (it as Avdoed).utenlandsopphold }
    }

    fun forelder_avdoed_naeringsinntekt(barnepensjon: Barnepensjon): Pair<String, BetingetOpplysning<Svar, Naeringsinntekt?>?> {
        return "forelder_avdoed_naeringsinntekt" to barnepensjon.foreldre.find { it.type == PersonType.AVDOED}
            ?.let{ (it as Avdoed).naeringsInntekt }
    }

    fun forelder_avdoed_militaertjeneste(barnepensjon: Barnepensjon): Pair<String,  BetingetOpplysning<Svar, Opplysning<AarstallForMilitaerTjeneste>?>?> {
        return "forelder_avdoed_militaertjeneste" to barnepensjon.foreldre.find { (it.type == PersonType.AVDOED)}
            ?.let { (it as Avdoed).militaertjeneste }
    }

    fun soesken(barnepensjon: Barnepensjon) =
        "soesken" to objectMapper.createObjectNode().set<ObjectNode>("soesken", objectMapper.valueToTree<ArrayNode>(barnepensjon.soesken))

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