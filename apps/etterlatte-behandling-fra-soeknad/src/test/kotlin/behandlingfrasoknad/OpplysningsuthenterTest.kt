package behandlingfrasoknad

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class OpplysningsuthenterTest {

    @Test
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

        assertEquals("innsender", opplysninger[0].opplysningType)
        assertEquals(
            """{"fornavn":"STOR","etternavn":"SNERK","foedselsnummer":"11057523044","type":"INNSENDER"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[0].opplysning).toString()
        )

        assertEquals("samtykke", opplysninger[1].opplysningType)
        assertEquals("""{"svar":true,"spoersmaal":""}""", objectMapper.valueToTree<ObjectNode>(opplysninger[1].opplysning).toString())

        assertEquals("utbetalingsinformasjon", opplysninger[2].opplysningType)
        assertEquals(
            """{"svar":"NORSK","spoersmaal":"Ønsker du å motta utbetalingen på norsk eller utenlandsk bankkonto?","opplysning":{"kontonummer":{"svar":"9999.99.99999","spoersmaal":"Oppgi norsk kontonummer for utbetaling av barnepensjon"},"utenlandskBankNavn":null,"utenlandskBankAdresse":null,"iban":null,"swift":null,"skattetrekk":{"svar":"JA","spoersmaal":"Ønsker du at vi legger inn et skattetrekk for barnepensjonen?","opplysning":{"svar":"20%","spoersmaal":"Oppgi ønsket skattetrekk"}}}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[2].opplysning).toString()
        )

        assertEquals("soeker_personinfo", opplysninger[3].opplysningType)
        assertEquals(
            """{"fornavn":"Gustaf","etternavn":"Raymondsen","foedselsnummer":"29081276127","type":"BARN"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[3].opplysning).toString()
        )

        assertEquals("soeker_statsborgerskap", opplysninger[4].opplysningType)
        assertEquals("""{"svar":"Norge","spoersmaal":"Statsborgerskap"}""", objectMapper.valueToTree<ObjectNode>(opplysninger[4].opplysning).toString())

        assertEquals("soeker_utenlandsadresse", opplysninger[5].opplysningType)
        assertEquals(
            """{"svar":"JA","spoersmaal":"Bor barnet i et annet land enn Norge?","opplysning":{"land":{"svar":"Oman","spoersmaal":"Land"},"adresse":{"svar":"Oman 1, 9999 Oman","spoersmaal":"Adresse i utlandet"}}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[5].opplysning).toString()
        )

        assertEquals("soeker_verge", opplysninger[6].opplysningType)
        assertEquals(
            """{"svar":"JA","spoersmaal":"Er det oppnevnt en verge for barnet?","opplysning":{"fornavn":"Verge","etternavn":"Vergesen","foedselsnummer":"19078504903","type":"VERGE"}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[6].opplysning).toString()
        )

        assertFalse(opplysninger.any { it.opplysningType ==  "soeker_daglig_omsorg"})

        assertEquals("forelder_gjenlevende_personinfo", opplysninger[7].opplysningType)
        assertEquals(
            """{"fornavn":"STOR","etternavn":"SNERK","foedselsnummer":"11057523044","adresse":{"svar":"BØLERLIA 99, 0689 Oslo","spoersmaal":"Bostedsadresse"},"statsborgerskap":{"svar":"Norge","spoersmaal":"Statsborgerskap"},"kontaktinfo":{"epost":{"svar":"epost@epost.no","spoersmaal":"E-post (valgfri)"},"telefonnummer":{"svar":"969 69 696","spoersmaal":"Telefonnummer"}},"type":"GJENLEVENDE_FORELDER"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[7].opplysning).toString()
        )

        assertEquals("forelder_avdoed_personinfo", opplysninger[8].opplysningType)
        assertEquals(
            """{"fornavn":"Reidar","etternavn":"Reidarsen","foedselsnummer":"19078504903","type":"AVDOED"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[8].opplysning).toString()
        )

        assertEquals("forelder_avdoed_doedsfallinformasjon", opplysninger[9].opplysningType)
        assertEquals(
            """{"datoForDoedsfallet":{"svar":"2022-01-05","spoersmaal":"Når skjedde dødsfallet?"},"doedsaarsakSkyldesYrkesskadeEllerYrkessykdom":{"svar":"JA","spoersmaal":"Skyldes dødsfallet yrkesskade eller yrkessykdom?"}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[9].opplysning).toString()
        )

        assertEquals("forelder_avdoed_utenlandsopphold", opplysninger[10].opplysningType)
        assertEquals(
            """{"svar":"JA","spoersmaal":"Bodde eller arbeidet han eller hun i et annet land enn Norge etter fylte 16 år?","opplysning":[{"land":{"svar":"Antigua og barbuda","spoersmaal":"Land"},"fraDato":{"svar":"2000-01-01","spoersmaal":"Fra dato (valgfri)"},"tilDato":{"svar":"2002-01-01","spoersmaal":"Til dato (valgfri)"},"oppholdsType":{"svar":["BODD"],"spoersmaal":"Bodd og/eller arbeidet?"},"medlemFolketrygd":{"svar":"JA","spoersmaal":"Var han eller hun medlem av folketrygden under oppholdet?"},"pensjonsutbetaling":{"svar":"-","spoersmaal":"Oppgi eventuell pensjon han eller hun mottok fra dette landet (valgfri)"}},{"land":{"svar":"Bahamas","spoersmaal":"Land"},"fraDato":{"svar":"2002-02-02","spoersmaal":"Fra dato (valgfri)"},"tilDato":{"svar":"2003-02-02","spoersmaal":"Til dato (valgfri)"},"oppholdsType":{"svar":["ARBEIDET"],"spoersmaal":"Bodd og/eller arbeidet?"},"medlemFolketrygd":{"svar":"NEI","spoersmaal":"Var han eller hun medlem av folketrygden under oppholdet?"},"pensjonsutbetaling":{"svar":"4738","spoersmaal":"Oppgi eventuell pensjon han eller hun mottok fra dette landet (valgfri)"}},{"land":{"svar":"Canada","spoersmaal":"Land"},"fraDato":{"svar":"1998-02-02","spoersmaal":"Fra dato (valgfri)"},"tilDato":{"svar":"1999-02-02","spoersmaal":"Til dato (valgfri)"},"oppholdsType":{"svar":["BODD","ARBEIDET"],"spoersmaal":"Bodd og/eller arbeidet?"},"medlemFolketrygd":{"svar":"VET_IKKE","spoersmaal":"Var han eller hun medlem av folketrygden under oppholdet?"},"pensjonsutbetaling":{"svar":"3929","spoersmaal":"Oppgi eventuell pensjon han eller hun mottok fra dette landet (valgfri)"}}]}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[10].opplysning).toString()
        )

        assertEquals("forelder_avdoed_naeringsinntekt", opplysninger[11].opplysningType)
        assertEquals(
            """{"svar":"JA","spoersmaal":"Var han eller hun selvstendig næringsdrivende?","opplysning":{"naeringsinntektPrAarFoerDoedsfall":{"svar":"83920","spoersmaal":"Oppgi næringsinntekt fra kalenderåret før dødsfallet (valgfri)"},"naeringsinntektVedDoedsfall":{"svar":"JA","spoersmaal":"Hadde han eller hun næringsinntekt når dødsfallet skjedde?"}}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[11].opplysning).toString()
        )

        assertEquals("forelder_avdoed_militaertjeneste", opplysninger[12].opplysningType)
        assertEquals(
            """{"svar":"JA","spoersmaal":"Har han eller hun gjennomført militær eller sivil førstegangstjeneste som varte minst 30 dager?","opplysning":{"svar":"1992","spoersmaal":"Hvilke(-t) år? (valgfri)"}}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[12].opplysning).toString()
        )

        assertEquals("soesken", opplysninger[13].opplysningType)
        assertEquals(
            """{"soesken":[{"fornavn":"Josef","etternavn":"Josefsen","foedselsnummer":"19078504903","statsborgerskap":{"svar":"Papua ny-guinea","spoersmaal":"Statsborgerskap"},"utenlandsAdresse":{"svar":"NEI","spoersmaal":"Bor barnet i et annet land enn Norge?","opplysning":null},"foreldre":[{"fornavn":"Reidar","etternavn":"Reidarsen","foedselsnummer":"19078504903","type":"FORELDER"}],"verge":null,"dagligOmsorg":{"svar":"GJENLEVENDE","spoersmaal":"Har du daglig omsorg for dette barnet?"},"type":"BARN"},{"fornavn":"Reidun","etternavn":"Gustafsen","foedselsnummer":"20060976385","statsborgerskap":{"svar":"Norge","spoersmaal":"Statsborgerskap"},"utenlandsAdresse":{"svar":"NEI","spoersmaal":"Bor barnet i et annet land enn Norge?","opplysning":null},"foreldre":[{"fornavn":"STOR","etternavn":"SNERK","foedselsnummer":"11057523044","type":"FORELDER"},{"fornavn":"Reidar","etternavn":"Reidarsen","foedselsnummer":"19078504903","type":"FORELDER"}],"verge":{"svar":"NEI","spoersmaal":"Er det oppnevnt en verge for barnet?","opplysning":null},"dagligOmsorg":null,"type":"BARN"}]}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[13].opplysning).toString()
        )

        assertEquals("soeknad_mottatt_dato", opplysninger[14].opplysningType)
        assertEquals(
            """{"mottattDato":"2022-01-25T15:29:34.621087004"}""",
            objectMapper.valueToTree<ObjectNode>(opplysninger[14].opplysning).toString()
        )

    }
}