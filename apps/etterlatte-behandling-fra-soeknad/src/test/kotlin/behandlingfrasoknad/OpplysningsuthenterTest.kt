package behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsuthenterTest {


    companion object {
        val opplysninger = Opplysningsuthenter().lagOpplysningsListe(
            objectMapper.treeToValue(
                objectMapper.readTree(
                    javaClass.getResource("/fullMessage2.json")!!.readText()
                )!!["@skjema_info"]
            )!!
        )
    }

    @Test
    fun `alle opplysninger skal ha innsender som kilde`() {
        val kilde = Behandlingsopplysning.Privatperson(
            "03108718357", LocalDateTime.parse("2022-02-14T14:37:24.573612786").toInstant(
                ZoneOffset.UTC
            )
        )
        opplysninger.forEach {
            assertEquals(kilde.fnr, (it.kilde as Behandlingsopplysning.Privatperson).fnr)
            assertEquals(kilde.mottatDato, (it.kilde as Behandlingsopplysning.Privatperson).mottatDato)
        }
    }

    @Test
    fun `skal hente opplysning om innsenders personinfo`() {
        consumeSingle<PersonInfo>(Opplysningstyper.INNSENDER_PERSONINFO_V1)
            .apply {
                assertEquals("GØYAL", fornavn)
                assertEquals("HØYSTAKK", etternavn)
                assertEquals("03108718357", foedselsnummer.value)
                assertEquals(PersonType.INNSENDER, type)
            }
    }

    @Test
    fun `skal hente opplysning om søkers personinfo`() {
        consumeSingle<PersonInfo>(Opplysningstyper.SOEKER_PERSONINFO_V1)
            .apply {
                assertEquals("kirsten", fornavn)
                assertEquals("jakobsen", etternavn)
                assertEquals("12101376212", foedselsnummer.value)
                assertEquals(PersonType.BARN, type)
            }
    }

    @Test
    fun `skal hente opplysning om gjenlevende forelders personinfo`() {
        consumeSingle<PersonInfo>(Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1)
            .apply {
                assertEquals("GØYAL", fornavn)
                assertEquals("HØYSTAKK", etternavn)
                assertEquals("03108718357", foedselsnummer.value)
                assertEquals(PersonType.GJENLEVENDE_FORELDER, type)
            }
    }

    @Test
    fun `skal hente opplysning om samtykke`() {
        consumeSingle<Samtykke>(Opplysningstyper.SAMTYKKE)
            .apply {
                assertEquals(true, harSamtykket)
            }
    }

    @Test
    fun `skal hente opplysning om utbetalingsinformasjon`() {
        consumeSingle<Utbetalingsinformasjon>(Opplysningstyper.UTBETALINGSINFORMASJON_V1)
            .apply {
                assertEquals(BankkontoType.NORSK, bankkontoType)
                assertEquals("6848.64.44444", kontonummer)
                assertNull(utenlandskBankNavn)
                assertNull(utenlandskBankAdresse)
                assertNull(iban)
                assertNull(swift)
                assertNull(oenskerSkattetrekk)
                assertNull(oensketSkattetrekkProsent)
            }
    }

    @Test
    fun `skal hente opplysning om søkers statsborkerskap`() {
        consumeSingle<Statsborgerskap>(Opplysningstyper.SOEKER_STATSBORGERSKAP_V1)
            .apply {
                assertEquals("Norge", statsborgerskap)
                assertEquals("12101376212", foedselsnummer)
            }
    }

    @Test
    @Disabled
    fun `skal hente opplysning om søkers adresse`() {
        consumeSingle<Utenlandsadresse>(Opplysningstyper.SOEKER_UTENLANDSADRESSE_V1)
            .apply {
                println(this)
            }
    }

    @Test
    fun `skal hente opplysning om søkers verge`() {
        consumeSingle<Verge>(Opplysningstyper.SOEKER_VERGE_V1 )
            .apply {
                assertEquals("Nei", barnHarVerge?.innhold)
            }
    }

    @Test
    fun `skal hente opplysning om daglig omsorg`() {
        consumeSingle<DagligOmsorg>(Opplysningstyper.SOEKER_DAGLIG_OMSORG_V1)
            .apply {
                assertNull(omsorgPerson)
            }
    }

    @Test
    fun `skal hente opplysning om avdøde`() {
        consumeSingle<PersonInfo>(Opplysningstyper.AVDOED_PERSONINFO_V1)
            .apply {
                assertEquals("fn", fornavn)
                assertEquals("en", etternavn)
                assertEquals(PersonType.AVDOED, type)
                assertEquals("22128202440", foedselsnummer.value)
            }
    }

    @Test
    fun `skal hente opplysning om avdødes dødsfall`() {
        consumeSingle<Doedsdato>(Opplysningstyper.AVDOED_DOEDSFALL_V1)
            .apply {
                assertEquals(LocalDate.of(2022, Month.JANUARY, 1), doedsdato)
                assertEquals("22128202440", foedselsnummer)
            }
    }

    @Test
    @Disabled
    fun `skal hente opplysning om dødsårsak`() {
        consumeSingle<Doedsaarsak>(Opplysningstyper.AVDOED_DOEDSAARSAK_V1)
            .apply {
                println(this)
            }
    }

    @Test
    fun `skal hente opplysning om avdødes utenlandsopphold`() {
        consumeSingle<Utenlandsopphold>(Opplysningstyper.AVDOED_UTENLANDSOPPHOLD_V1)
            .apply {
                assertEquals("Nei", harHattUtenlandsopphold)
            }
    }

    @Test
    @Disabled
    fun `skal hente opplysning om avdødes næringsinntekt`() {
        consumeSingle<Naeringsinntekt>(Opplysningstyper.AVDOED_NAERINGSINNTEKT_V1)
            .apply {
                println(this)
            }
    }

    @Test
    @Disabled
    fun `skal hente opplysning om avdødes militærtjeneste`() {
        consumeSingle<Militaertjeneste>(Opplysningstyper.AVDOED_MILITAERTJENESTE_V1)
            .apply {
                println(this)
            }
    }

    @Test
    fun `skal hente opplysning om søsken`() {
        consumeSingle<Soesken>(Opplysningstyper.SOEKER_RELASJON_SOESKEN_V1)
            .apply {
                assertTrue(soesken!!.isEmpty())
            }
    }

    @Test
    fun `skal hente opplysning om mottatt dato`() {
        consumeSingle<SoeknadMottattDato>(Opplysningstyper.SOEKNAD_MOTTATT_DATO)
            .apply {
                assertEquals(LocalDateTime.parse("2022-02-14T14:37:24.573612786"), mottattDato)
            }
    }


    inline fun <reified T> consumeSingle(opplysningType: Opplysningstyper) =
        opplysninger.filter { it.opplysningType == opplysningType }
            .also { assertEquals(1, it.size) }
            .first()
            .let {
                it.opplysning as T
            }
}