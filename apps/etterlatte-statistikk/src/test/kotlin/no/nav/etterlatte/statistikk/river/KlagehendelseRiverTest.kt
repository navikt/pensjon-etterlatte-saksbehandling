package no.nav.etterlatte.statistikk.river

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.klage.KLAGE_STATISTIKK_RIVER_KEY
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.service.StatistikkService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlagehendelseRiverTest {
    private val soeknadStatistikkService: StatistikkService =
        mockk {
            every { registrerStatistikkForKlagehendelse(any(), any(), any()) } returns null
        }

    private val testRapid: TestRapid =
        TestRapid().apply {
            KlagehendelseRiver(this, soeknadStatistikkService)
        }

    @Test
    fun `Skal ta i mot klagemeldinger`() {
        val message =
            JsonMessage
                .newMessage(
                    mapOf(
                        KlageHendelseType.OPPRETTET.lagParMedEventNameKey(),
                        CORRELATION_ID_KEY to UUID.randomUUID(),
                        TEKNISK_TID_KEY to LocalDateTime.now(),
                        KLAGE_STATISTIKK_RIVER_KEY to
                            StatistikkKlage(
                                UUID.randomUUID(),
                                Klage(
                                    UUID.randomUUID(),
                                    Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.defaultEnhet.enhetNr),
                                    Tidspunkt.now(),
                                    KlageStatus.OPPRETTET,
                                    kabalResultat = null,
                                    kabalStatus = null,
                                    formkrav = null,
                                    innkommendeDokument = null,
                                    resultat = null,
                                    utfall = null,
                                    aarsakTilAvbrytelse = null,
                                    initieltUtfall = null,
                                ),
                                Tidspunkt.now(),
                                null,
                            ),
                    ),
                ).toJson()
        val inspector = testRapid.apply { sendTestMessage(message) }.inspektør
        Assertions.assertEquals(0, inspector.size) // kjører ingen STATISTIKK:REGISTRERT melding da null i mock

        verify { soeknadStatistikkService.registrerStatistikkForKlagehendelse(any(), any(), any()) }
    }
}
