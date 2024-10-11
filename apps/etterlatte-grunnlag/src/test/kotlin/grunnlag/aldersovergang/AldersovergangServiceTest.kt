package grunnlag.aldersovergang

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.grunnlag.GrunnlagDbExtension
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangDao
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.insert
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(GrunnlagDbExtension::class)
class AldersovergangServiceTest(
    private val dataSource: DataSource,
) {
    private val dao = AldersovergangDao(dataSource)
    private val service = AldersovergangService(dao)

    @Test
    fun `foedt for 20 aar siden er 20`() {
        val sakId = randomSakId()
        lagSakMedSoekerFoedtPaaGittDato(LocalDate.now().minusYears(20), sakId)
        assertEquals(20, service.hentAlder(sakId, PersonRolle.BARN, LocalDate.now()))
    }

    @Test
    fun `foedt for 20 aar siden i morgen er 19`() {
        val sakId = randomSakId()
        lagSakMedSoekerFoedtPaaGittDato(LocalDate.now().minusYears(20).plusDays(1), sakId)
        assertEquals(19, service.hentAlder(sakId, PersonRolle.BARN, LocalDate.now()))
    }

    @Test
    fun `Mottaker av omstillingstoenad henter måned etter fyller 67 år`() {
        val sakId = randomSakId()
        lagSakMedSoekerFoedtPaaGittDato(LocalDate.of(2000, 6, 15), sakId)
        service.aldersovergangMaaned(sakId, SakType.OMSTILLINGSSTOENAD) shouldBe YearMonth.of(2067, 7)
    }

    private fun lagSakMedSoekerFoedtPaaGittDato(
        foedselsdato: LocalDate,
        sakId: SakId,
    ) {
        dataSource.insert(
            tabellnavn = "grunnlagshendelse",
            params =
                {
                    mapOf(
                        "opplysning_type" to "SOEKER_PDL_V1",
                        "opplysning" to "{}",
                        "sak_id" to sakId.sakId,
                        "hendelsenummer" to 1,
                        "fnr" to SOEKER_FOEDSELSNUMMER.value,
                    )
                },
        )
        dataSource.insert(
            tabellnavn = "grunnlagshendelse",
            params =
                {
                    mapOf(
                        "opplysning_type" to "FOEDSELSDATO",
                        "opplysning" to foedselsdato,
                        "sak_id" to sakId.sakId,
                        "hendelsenummer" to 2,
                        "fnr" to SOEKER_FOEDSELSNUMMER.value,
                    )
                },
        )
    }
}
