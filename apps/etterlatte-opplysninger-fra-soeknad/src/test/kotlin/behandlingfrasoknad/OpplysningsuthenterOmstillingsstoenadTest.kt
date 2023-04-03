package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Samtykke
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.SoekerOmstillingSoeknad
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.SoeknadstypeOpplysning
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Utbetalingsinformasjon
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import soeknad.InnsendtSoeknadTestData
import java.time.LocalDate
import java.time.Month

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsuthenterOmstillingsstoenadTest {

    @Test
    fun `alle opplysninger skal ha innsender som kilde`() {
        opplysninger.forEach {
            assertEquals("03108718357", (it.kilde as Grunnlagsopplysning.Privatperson).fnr)
        }
    }

    @Test
    fun `skal hente opplysning om avdoede`() {
        consumeSingle<AvdoedSoeknad>(Opplysningstype.AVDOED_SOEKNAD_V1)
            .apply {
                assertEquals("Bernt", fornavn)
                assertEquals("Jakobsen", etternavn)
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
        consumeSingle<SoekerOmstillingSoeknad>(Opplysningstype.SOEKER_SOEKNAD_V1)
            .apply {
                assertEquals(PersonType.GJENLEVENDE, type)
                assertEquals("Kirsten", fornavn)
                assertEquals("Jakobsen", etternavn)
                assertEquals("26058411891", foedselsnummer.value)
                assertEquals("Et sted 31", adresse)
                assertEquals("Norge", statsborgerskap)
                assertEquals("12345678", telefonnummer)
                assertEquals("Gift", sivilstatus)
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
                Assertions.assertNull(utenlandskBankNavn)
                Assertions.assertNull(utenlandskBankAdresse)
                Assertions.assertNull(iban)
                Assertions.assertNull(swift)
                Assertions.assertNull(oenskerSkattetrekk)
                Assertions.assertNull(oensketSkattetrekkProsent)
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
    fun `skal hente opplysning om spraak`() {
        consumeSingle<Spraak>(Opplysningstype.SPRAAK)
            .apply {
                assertEquals(Spraak.NB, this)
            }
    }

    @Test
    fun `skal hente opplysning om soeknadstype`() {
        consumeSingle<SoeknadstypeOpplysning>(Opplysningstype.SOEKNADSTYPE_V1).apply {
            assertEquals(SoeknadType.OMSTILLINGSSTOENAD, this.type)
        }
    }

    @Test
    fun `skal hente opplysning om persongalleri`() {
        consumeSingle<Persongalleri>(Opplysningstype.PERSONGALLERI_V1).apply {
            assertEquals(soeker, "26058411891")
            assertEquals(innsender, "03108718357")
            assertEquals(avdoed.get(0), "22128202440")
        }
    }

    private inline fun <reified T> consumeSingle(opplysningType: Opplysningstype) =
        opplysninger.filter { it.opplysningType == opplysningType }
            .also { assertEquals(1, it.size) }
            .first()
            .let {
                it.opplysning as T
            }

    companion object {
        val opplysninger = Opplysningsuthenter().lagOpplysningsListe(
            objectMapper.treeToValue(
                objectMapper.readTree(lagMelding())!!["@skjema_info"]
            ),
            SoeknadType.OMSTILLINGSSTOENAD
        )
        private fun lagMelding(): String {
            val soeknad = InnsendtSoeknadTestData.omstillingsSoeknad()
            return """
            {
              "@event_name": "GYLDIG_SOEKNAD:VURDERT",
              "behandlingId": "f525f2f7-e246-43d7-b61a-5f0757472916",
              "sakId": 1,
              "@skjema_info": ${soeknad.toJson()},
              "@lagret_soeknad_id": 360,
              "@template": "soeknad",
              "@fnr_soeker": "20110875720",
              "@hendelse_gyldig_til": "2025-01-03T13:15:31.249299158Z",
              "system_read_count": 1,
              "system_participating_services": [
                {
                  "service": "innsendt-soeknad",
                  "instance": "innsendt-soeknad-5df54b5547-xtntr",
                  "time": "2022-01-03T13:45:31.249795745"
                },
                {
                  "service": "sjekk-adressebeskyttelse",
                  "instance": "sjekk-adressebeskyttelse-66bffc6ccc-4wmsn",
                  "time": "2022-01-03T13:45:31.256701251"
                }
              ],
              "@adressebeskyttelse": "STRENGT_FORTROLIG_UTLAND"
            }
            """.trimIndent()
        }
    }
}