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
    fun `kan hente alder`() {
        dataSource.insert(
            tabellnavn = "grunnlagshendelse",
            params =
                mapOf(
                    "opplysning_type" to "SOEKER_PDL_V1",
                    "opplysning" to "1999-06-04",
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
                    "opplysning" to LocalDate.now().minusYears(21),
                    "sak_id" to 1,
                    "hendelsenummer" to 2,
                    "fnr" to SOEKER_FOEDSELSNUMMER.value,
                ),
        )

        assertEquals(21, service.hentAlder(1, PersonRolle.BARN))
    }
}
