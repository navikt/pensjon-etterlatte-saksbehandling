package grunnlag.aldersovergang

import no.nav.etterlatte.grunnlag.GrunnlagDbExtension
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.insert
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(GrunnlagDbExtension::class)
class AldersovergangServiceTest(private val dataSource: DataSource) {
    private val dao = AldersovergangDao(dataSource)
    private val service = AldersovergangService(dao)

    @Test
    fun `foedt for 20 aar siden er 20`() {
        lagSakMedSoekerFoedtPaaGittDato(LocalDate.now().minusYears(20))
        assertEquals(20, service.hentAlder(1, PersonRolle.BARN, LocalDate.now()))
    }

    @Test
    fun `foedt for 20 aar siden i morgen er 19`() {
        lagSakMedSoekerFoedtPaaGittDato(LocalDate.now().minusYears(20).plusDays(1))
        assertEquals(19, service.hentAlder(1, PersonRolle.BARN, LocalDate.now()))
    }

    private fun lagSakMedSoekerFoedtPaaGittDato(foedselsdato: LocalDate) {
        dataSource.insert(
            tabellnavn = "grunnlagshendelse",
            params =
                mapOf(
                    "opplysning_type" to "SOEKER_PDL_V1",
                    "opplysning" to "{}",
                    "sak_id" to 1,
                    "hendelsenummer" to 1,
                    "fnr" to SOEKER_FOEDSELSNUMMER.value,
                ),
        )
        dataSource.insert(
            tabellnavn = "grunnlagshendelse",
            params =
                mapOf(
                    "opplysning_type" to "FOEDSELSDATO",
                    "opplysning" to foedselsdato,
                    "sak_id" to 1,
                    "hendelsenummer" to 2,
                    "fnr" to SOEKER_FOEDSELSNUMMER.value,
                ),
        )
    }
}
