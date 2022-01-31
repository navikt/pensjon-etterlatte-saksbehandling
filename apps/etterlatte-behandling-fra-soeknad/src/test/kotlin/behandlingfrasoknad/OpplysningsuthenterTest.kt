package behandlingfrasoknad

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
        assertEquals("samtykke", opplysninger[1].opplysningType)
        assertEquals("utbetalingsinformasjon", opplysninger[2].opplysningType)
        assertEquals("soeker_personinfo", opplysninger[3].opplysningType)
        assertEquals("soeker_statsborgerskap", opplysninger[4].opplysningType)
        assertEquals("soeker_utenlandsadresse", opplysninger[5].opplysningType)
        assertEquals("soeker_verge", opplysninger[6].opplysningType)
        assertEquals("soeker_daglig_omsorg", opplysninger[7].opplysningType)
        assertEquals("forelder_gjenlevende_personinfo", opplysninger[8].opplysningType)
        assertEquals("forelder_avdoed_personinfo", opplysninger[9].opplysningType)
        assertEquals("forelder_avdoed_doedsfallinformasjon", opplysninger[10].opplysningType)
        assertEquals("forelder_avdoed_utenlandsopphold", opplysninger[11].opplysningType)
        assertEquals("forelder_avdoed_naeringsinntekt", opplysninger[12].opplysningType)
        assertEquals("forelder_avdoed_militaertjeneste", opplysninger[13].opplysningType)
        assertEquals("soesken", opplysninger[14].opplysningType)
        assertEquals("soeknad_mottatt_dato", opplysninger[15].opplysningType)

        // bør inneholdet i objektene over testes på noen måte?

    }
}