package no.nav.etterlatte.tidshendelser

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_STEG_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import javax.sql.DataSource

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
        hendelseDao.opprettHendelserForSaker(jobb.id, listOf(randomSakId()), Steg.IDENTIFISERT_SAK)
        val hendelseId = hendelseDao.hentHendelserForJobb(jobb.id).first().id

        val melding =
            JsonMessage.newMessage(
                EventNames.TIDSHENDELSE.name,
                mapOf(
                    TIDSHENDELSE_STEG_KEY to "VURDERT_LOEPENDE_YTELSE",
                    TIDSHENDELSE_TYPE_KEY to "BP20",
                    TIDSHENDELSE_ID_KEY to hendelseId.toString(),
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
        hendelseDao.opprettHendelserForSaker(jobb.id, listOf(randomSakId()), Steg.IDENTIFISERT_SAK)
        val hendelseId = hendelseDao.hentHendelserForJobb(jobb.id).first().id

        val melding =
            JsonMessage.newMessage(
                EventNames.TIDSHENDELSE.name,
                mapOf(
                    TIDSHENDELSE_STEG_KEY to "OPPGAVE_OPPRETTET",
                    TIDSHENDELSE_TYPE_KEY to "BP20",
                    TIDSHENDELSE_ID_KEY to hendelseId.toString(),
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
