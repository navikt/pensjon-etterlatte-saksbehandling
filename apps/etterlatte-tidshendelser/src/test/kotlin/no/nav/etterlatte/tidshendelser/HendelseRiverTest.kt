package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import javax.sql.DataSource
import kotlin.random.Random

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelseRiverTest(
    dataSource: DataSource,
) {
    private val hendelseDao = HendelseDao(dataSource)
    private val jobbTestdata = JobbTestdata(dataSource, hendelseDao)

    private val inspector = TestRapid().apply { HendelseRiver(this, hendelseDao) }

    @Test
    fun `skal lese melding og sjekke loepende ytelse`() {
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.now())
        hendelseDao.opprettHendelserForSaker(jobb.id, listOf(Random.nextLong(9999)), Steg.IDENTIFISERT_SAK)
        val hendelseId = hendelseDao.hentHendelserForJobb(jobb.id).first().id

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERGANG.name,
                mapOf(
                    ALDERSOVERGANG_STEG_KEY to "VURDERT_LOEPENDE_YTELSE",
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to hendelseId.toString(),
                    SAK_ID_KEY to 8763L,
                    HENDELSE_DATA_KEY to mapOf("loependeYtelse" to true),
                ),
            )

        inspector.apply { sendTestMessage(melding.toJson()) }

        with(hendelseDao.hentHendelserForJobb(jobb.id).first()) {
            this.steg shouldBe "VURDERT_LOEPENDE_YTELSE"
            this.info shouldNotBe null
            this.loependeYtelse shouldBe true
        }
    }

    @Test
    fun `skal lese melding og ferdigstille hendelse`() {
        val jobb = jobbTestdata.opprettJobb(JobbType.AO_BP20, YearMonth.now())
        hendelseDao.opprettHendelserForSaker(jobb.id, listOf(Random.nextLong(9999)), Steg.IDENTIFISERT_SAK)
        val hendelseId = hendelseDao.hentHendelserForJobb(jobb.id).first().id

        val melding =
            JsonMessage.newMessage(
                EventNames.ALDERSOVERGANG.name,
                mapOf(
                    ALDERSOVERGANG_STEG_KEY to "OPPGAVE_OPPRETTET",
                    ALDERSOVERGANG_TYPE_KEY to "BP20",
                    ALDERSOVERGANG_ID_KEY to hendelseId.toString(),
                    SAK_ID_KEY to 4735L,
                ),
            )

        inspector.apply { sendTestMessage(melding.toJson()) }

        with(hendelseDao.hentHendelserForJobb(jobb.id).first()) {
            this.steg shouldBe "OPPGAVE_OPPRETTET"
            this.status shouldBe HendelseStatus.FERDIG
            this.info shouldNotBe null
        }

        with(hendelseDao.hentJobb(jobb.id)) {
            this.status shouldBe JobbStatus.FERDIG
        }
    }
}
