package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.etteroppgjoer.EtteroppgjoerDao
import no.nav.etterlatte.tidshendelser.etteroppgjoer.EtteroppgjoerFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EtteroppgjoerDaoTest(
    private val dataSource: DataSource,
) {
    private val etteroppgjoerDao = EtteroppgjoerDao(dataSource)

    @Test
    fun `skal hente nyeste etteroppgjoer_konfigurasjon`() {
        (1..2).forEach {
            dataSource.connection.use { conn ->
                conn
                    .prepareStatement(
                        """
                INSERT INTO etteroppgjoer_konfigurasjon(
                    inntektsaar, antall, dato, etteroppgjoer_filter, spesifikke_saker, ekskluderte_saker, spesifikke_enheter, kjoering_id
                ) 
                VALUES (2024, 1, '2025-10-01', 'ENKEL', '{1234}', '{1235}', '{enhet}', 'kjoeringId-$it')
                """,
                    ).execute()
            }
        }

        val konfigurasjon = etteroppgjoerDao.hentNyesteKonfigurasjon()
        with(konfigurasjon) {
            spesifikkeEnheter shouldBe listOf("enhet")
            spesifikkeSaker shouldBe listOf(SakId(1234L))
            ekskluderteSaker shouldBe listOf(SakId(1235L))
            etteroppgjoerFilter shouldBe EtteroppgjoerFilter.ENKEL
            antall shouldBe 1
            dato shouldBe LocalDate.parse("2025-10-01")
            kjoeringId shouldBe "kjoeringId-2"
        }
    }
}
