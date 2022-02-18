package behandlingfrasoknad

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class OpplysningsuthenterTest {

    @Test
    @Disabled
    fun lagSkjemaInfoOpplysningsListe() {
        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fullMessage3.json")!!.readText())!!
        val opplysningsuthenter = Opplysningsuthenter()
        val opplysninger =
            opplysningsuthenter.lagOpplysningsListe(objectMapper.treeToValue(hendelseJson["@skjema_info"])!!)

        assertEquals((opplysninger[0].kilde as Behandlingsopplysning.Privatperson).fnr, "11057523044")
        assertEquals(
            (opplysninger[0].kilde as Behandlingsopplysning.Privatperson).mottatDato.epochSecond,
            LocalDateTime.parse("2022-01-25T15:29:34.621087004").atOffset(
                ZoneOffset.UTC
            ).toInstant().epochSecond
        )

        assertEquals("innsender_personinfo:v1", opplysninger[0].opplysningType)
        assertEquals(
            """{"fornavn":"STOR","etternavn":"SNERK","foedselsnummer":"11057523044","type":"INNSENDER"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[0].opplysning).toString()
        )

        assertEquals("samtykke", opplysninger[1].opplysningType)
        assertEquals(
            """{"harSamtykket":true}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[1].opplysning).toString()
        )

        assertEquals("utbetalingsinformasjon:v1", opplysninger[2].opplysningType)
        assertEquals(
            """{"bankkontoType":"NORSK","kontonummer":"9999.99.99999","utenlandskBankNavn":null,"utenlandskBankAdresse":null,"iban":null,"swift":null,"oenskerSkattetrekk":"JA","oensketSkattetrekkProsent":"20%"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[2].opplysning).toString()
        )

        assertEquals("soeker_personinfo:v1", opplysninger[3].opplysningType)
        assertEquals(
            """{"fornavn":"Gustaf","etternavn":"Raymondsen","foedselsnummer":"29081276127","type":"BARN"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[3].opplysning).toString()
        )

        assertEquals("soeker_statsborgerskap:v1", opplysninger[4].opplysningType)
        assertEquals(
            """{"statsborgerskap":"Norge","foedselsnummer":"29081276127"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[4].opplysning).toString()
        )

        assertEquals("soeker_utenlandsadresse:v1", opplysninger[5].opplysningType)
        assertEquals(
            """{"adresseIUtlandet":"JA","land":"Oman","adresse":"Oman 1, 9999 Oman","foedselsnummer":"29081276127"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[5].opplysning).toString()
        )

        assertEquals("soeker_verge:v1", opplysninger[6].opplysningType)
        assertEquals(
            """{"barnHarVerge":"JA","fornavn":"Verge","etternavn":"Vergesen","foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[6].opplysning).toString()
        )

        assertFalse(opplysninger.any { it.opplysningType == "soeker_daglig_omsorg:v1" })

        assertEquals("forelder_gjenlevende_personinfo:v1", opplysninger[8].opplysningType)
        assertEquals(
            """{"fornavn":"STOR","etternavn":"SNERK","foedselsnummer":"11057523044","type":"GJENLEVENDE_FORELDER"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[8].opplysning).toString()
        )

        assertEquals("avdoed_personinfo:v1", opplysninger[9].opplysningType)
        assertEquals(
            """{"fornavn":"Reidar","etternavn":"Reidarsen","foedselsnummer":"19078504903","type":"AVDOED"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[9].opplysning).toString()
        )

        assertEquals("avdoed_doedsfall:v1", opplysninger[10].opplysningType)
        assertEquals(
            """{"doedsdato":"2022-01-05","foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[10].opplysning).toString()
        )

        assertEquals("avdoed_doedsaarsak:v1", opplysninger[11].opplysningType)
        assertEquals(
            """{"doedsaarsakSkyldesYrkesskadeEllerYrkessykdom":"JA","foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[11].opplysning).toString()
        )

        assertEquals("avdoed_utenlandsopphold:v1", opplysninger[12].opplysningType)
        assertEquals(
            """{"harHattUtenlandsopphold":"JA","opphold":[{"land":"Antigua og barbuda","fraDato":"2000-01-01","tilDato":"2002-01-01","oppholdsType":["BODD"],"medlemFolketrygd":"JA","pensjonsutbetaling":"-"},{"land":"Bahamas","fraDato":"2002-02-02","tilDato":"2003-02-02","oppholdsType":["ARBEIDET"],"medlemFolketrygd":"NEI","pensjonsutbetaling":"4738"},{"land":"Canada","fraDato":"1998-02-02","tilDato":"1999-02-02","oppholdsType":["BODD","ARBEIDET"],"medlemFolketrygd":"VET_IKKE","pensjonsutbetaling":"3929"}],"foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[12].opplysning).toString()
        )

        assertEquals("avdoed_naeringsinntekt:v1", opplysninger[13].opplysningType)
        assertEquals(
            """{"selvstendigNaeringsdrivende":"JA","haddeNaeringsinntektVedDoedsfall":"JA","naeringsinntektAarFoerDoedsfall":"83920","foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[13].opplysning).toString()
        )

        assertEquals("avdoed_militaertjeneste:v1", opplysninger[14].opplysningType)
        assertEquals(
            """{"harHattMilitaertjeneste":"JA","aarstallForMilitaerTjeneste":"1992","foedselsnummer":"19078504903"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[14].opplysning).toString()
        )

        assertEquals("soeker_soesken:v1", opplysninger[15].opplysningType)
        assertEquals(
            """{"soesken":[{"fornavn":"Josef","etternavn":"Josefsen","foedselsnummer":"19078504903","type":"BARN"},{"fornavn":"Reidun","etternavn":"Gustafsen","foedselsnummer":"20060976385","type":"BARN"}]}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[15].opplysning).toString()
        )

        assertEquals("soeknad_mottatt_dato", opplysninger[16].opplysningType)
        assertEquals(
            """{"mottattDato":"2022-01-25T15:29:34.621087004"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[16].opplysning).toString()
        )

    }
}