package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Samtykke
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadstypeOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utbetalingsinformasjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsuthenterBarnepensjonTest {

    companion object {
        val opplysninger = Opplysningsuthenter().lagOpplysningsListe(
            objectMapper.treeToValue(
                objectMapper.readTree(
                    OpplysningsuthenterBarnepensjonTest::class.java.getResource("/melding.json")!!.readText()
                )!!["@skjema_info"]
            ),
            SoeknadType.BARNEPENSJON
        )
    }

    @Test
    fun `alle opplysninger skal ha innsender som kilde`() {
        val kilde = Grunnlagsopplysning.Privatperson(
            "03108718357",
            LocalDateTime.parse("2022-02-14T14:37:24.573612786").toTidspunkt()
        )
        opplysninger.forEach {
            assertEquals(kilde.fnr, (it.kilde as Grunnlagsopplysning.Privatperson).fnr)
            assertEquals(kilde.mottatDato, (it.kilde as Grunnlagsopplysning.Privatperson).mottatDato)
        }
    }

    @Test
    fun `skal hente opplysning om avdoede`() {
        consumeSingle<AvdoedSoeknad>(Opplysningstype.AVDOED_SOEKNAD_V1)
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
        consumeSingle<SoekerBarnSoeknad>(Opplysningstype.SOEKER_SOEKNAD_V1)
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
        consumeSingle<GjenlevendeForelderSoeknad>(Opplysningstype.GJENLEVENDE_FORELDER_SOEKNAD_V1)
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
        consumeSingle<InnsenderSoeknad>(Opplysningstype.INNSENDER_SOEKNAD_V1)
            .apply {
                assertEquals(PersonType.INNSENDER, type)
                assertEquals("GØYAL", fornavn)
                assertEquals("HØYSTAKK", etternavn)
                assertEquals("03108718357", foedselsnummer.value)
            }
    }

    @Test
    fun `skal hente opplysning om utbetalingsinformasjon`() {
        consumeSingle<Utbetalingsinformasjon>(Opplysningstype.UTBETALINGSINFORMASJON_V1)
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
        consumeSingle<Samtykke>(Opplysningstype.SAMTYKKE)
            .apply {
                assertEquals(true, harSamtykket)
            }
    }

    @Test
    fun `skal hente opplysning om språk`() {
        consumeSingle<Spraak>(Opplysningstype.SPRAAK)
            .apply {
                assertEquals(Spraak.NB, this)
            }
    }

    @Test
    fun `skal hente opplysning om mottatt dato`() {
        consumeSingle<SoeknadMottattDato>(Opplysningstype.SOEKNAD_MOTTATT_DATO)
            .apply {
                assertEquals(LocalDateTime.parse("2022-02-14T14:37:24.573612786"), mottattDato)
            }
    }

    @Test
    fun `skal hente opplysning om soeknadstype`() {
        consumeSingle<SoeknadstypeOpplysning>(Opplysningstype.SOEKNADSTYPE_V1).apply {
            assertEquals(SoeknadType.BARNEPENSJON, this.type)
        }
    }

    private inline fun <reified T> consumeSingle(opplysningType: Opplysningstype) =
        opplysninger.filter { it.opplysningType == opplysningType }
            .also { assertEquals(1, it.size) }
            .first()
            .let {
                it.opplysning as T
            }
}