import io.kotest.matchers.shouldBe
import no.nav.etterlatte.TidshendelsePacket
import no.nav.etterlatte.TidshendelseService
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class TidshendelsePacketTest {
    @Test
    fun `minimal melding kan tolkes`() {
        val jsonMessage =
            JsonMessage(
                mapOf(
                    "dato" to "2018-02-25",
                    TIDSHENDELSE_TYPE_KEY to TidshendelseService.TidshendelserJobbType.AO_BP21,
                ).toJson(),
                MessageProblems(""),
            )
        interestedIn(jsonMessage)

        val tidshendelsePacket = TidshendelsePacket(jsonMessage)

        tidshendelsePacket.behandlingId shouldBe null
        tidshendelsePacket.behandlingsmaaned shouldBe YearMonth.of(2018, 2)
        tidshendelsePacket.dryrun shouldBe false
        tidshendelsePacket.harLoependeYtelse shouldBe false
        tidshendelsePacket.harMigrertYrkesskadeFordel shouldBe false
        tidshendelsePacket.harRettUtenTidsbegrensning shouldBe false
        tidshendelsePacket.hendelseId shouldBe ""
        tidshendelsePacket.jobbtype shouldBe TidshendelseService.TidshendelserJobbType.AO_BP21
        tidshendelsePacket.sakId shouldBe 0L
    }

    @Test
    fun `maksimal melding kan tolkes`() {
        val behandlingId = UUID.randomUUID()
        val jsonMessage =
            JsonMessage(
                mapOf(
                    "dato" to "2018-01-08",
                    "ao_type" to TidshendelseService.TidshendelserJobbType.AO_BP20,
                    "dry_run" to true,
                    "hendelse_data" to
                        mapOf(
                            "loependeYtelse" to true,
                            "loependeYtelse_behandlingId" to behandlingId.toString(),
                        ),
                    "yrkesskadefordel_pre_20240101" to true,
                    "oms_rett_uten_tidsbegrensning" to true,
                    "sakId" to 8448L,
                    "ao_id" to "min_id",
                ).toJson(),
                MessageProblems(""),
            )
        interestedIn(jsonMessage)

        val tidshendelsePacket = TidshendelsePacket(jsonMessage)

        tidshendelsePacket.behandlingId shouldBe behandlingId
        tidshendelsePacket.behandlingsmaaned shouldBe YearMonth.of(2018, 1)
        tidshendelsePacket.dryrun shouldBe true
        tidshendelsePacket.harLoependeYtelse shouldBe true
        tidshendelsePacket.harMigrertYrkesskadeFordel shouldBe true
        tidshendelsePacket.harRettUtenTidsbegrensning shouldBe true
        tidshendelsePacket.hendelseId shouldBe "min_id"
        tidshendelsePacket.jobbtype shouldBe TidshendelseService.TidshendelserJobbType.AO_BP20
        tidshendelsePacket.sakId shouldBe 8448L
    }

    private fun interestedIn(jsonMessage: JsonMessage) {
        jsonMessage.interestedIn(
            "sakId",
            "dato",
            "dry_run",
            "hendelseId",
            "jobbtype",
            HENDELSE_DATA_KEY,
            TIDSHENDELSE_TYPE_KEY,
            TIDSHENDELSE_ID_KEY,
            "yrkesskadefordel_pre_20240101",
            "oms_rett_uten_tidsbegrensning",
        )
    }
}
