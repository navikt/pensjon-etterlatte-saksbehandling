package behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
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
    fun `skal hente opplysning om avdoede`() {
        consumeSingle<AvdoedSoeknad>(Opplysningstyper.AVDOED_SOEKNAD_V1)
            .apply {
                assertEquals("fn", fornavn)
                assertEquals("en", etternavn)
                assertEquals(PersonType.AVDOED, type)
                assertEquals("22128202440", foedselsnummer.value)
                assertEquals(LocalDate.of(2022, Month.JANUARY, 1), doedsdato)
                assertEquals("Norge", statsborgerskap)
                assertEquals(JaNeiVetIkke.NEI, utenlandsopphold.harHattUtenlandsopphold)
                assertEquals(JaNeiVetIkke.NEI, doedsaarsakSkyldesYrkesskadeEllerYrkessykdom)
            }
    }

    @Test
    fun `skal hente opplysning om soeker`() {
        consumeSingle<SoekerBarnSoeknad>(Opplysningstyper.SOEKER_SOEKNAD_V1)
            .apply {
                assertEquals(PersonType.BARN, type)
                assertEquals("kirsten", fornavn)
                assertEquals("jakobsen", etternavn)
                assertEquals("12101376212", foedselsnummer.value)
                assertEquals("Norge", statsborgerskap)
                assertEquals(JaNeiVetIkke.NEI, utenlandsadresse.adresseIUtlandet)
                assertEquals("GØYAL", foreldre[0].fornavn)
                assertEquals("22128202440", foreldre[1].foedselsnummer.value)
                assertEquals(JaNeiVetIkke.NEI, verge.barnHarVerge)
                assertNull(omsorgPerson)
            }
    }

    @Test
    fun `skal hente opplysninger om gjenlevende forelder`() {
        consumeSingle<GjenlevendeForelderSoeknad>(Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1)
            .apply {
                assertEquals("GØYAL", fornavn)
                assertEquals("HØYSTAKK", etternavn)
                assertEquals("03108718357", foedselsnummer.value)
                assertEquals(PersonType.GJENLEVENDE_FORELDER, type)
                assertEquals("Sannergata 6C, 0557 Oslo", adresse)
                assertEquals("Norge", statsborgerskap)
                assertEquals("11111111", telefonnummer)
            }
    }

    @Test
    fun `skal hente opplysning om innsender`() {
        consumeSingle<InnsenderSoeknad>(Opplysningstyper.INNSENDER_SOEKNAD_v1)
            .apply {
                assertEquals(PersonType.INNSENDER, type)
                assertEquals("GØYAL", fornavn)
                assertEquals("HØYSTAKK", etternavn)
                assertEquals("03108718357", foedselsnummer.value)
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
    fun `skal hente opplysning om samtykke`() {
        consumeSingle<Samtykke>(Opplysningstyper.SAMTYKKE)
            .apply {
                assertEquals(true, harSamtykket)
            }
    }

    @Test
    fun `skal hente opplysning om mottatt dato`() {
        consumeSingle<SoeknadMottattDato>(Opplysningstyper.SOEKNAD_MOTTATT_DATO)
            .apply {
                assertEquals(LocalDateTime.parse("2022-02-14T14:37:24.573612786"), mottattDato)
            }
    }

    @Test
    fun `skal hente opplysning om soeknadstype`() {
        consumeSingle<SoeknadstypeOpplysning>(Opplysningstyper.SOEKNADSTYPE_V1).apply {
            assertEquals(SoeknadType.BARNEPENSJON, this.type)
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