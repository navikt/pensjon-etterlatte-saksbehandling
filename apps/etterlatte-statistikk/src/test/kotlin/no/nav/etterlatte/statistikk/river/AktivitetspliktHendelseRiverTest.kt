package no.nav.etterlatte.statistikk.river

import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.statistikk.service.AktivitetspliktService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AktivitetspliktHendelseRiverTest {
    private val aktivitetspliktService: AktivitetspliktService = mockk()

    private val testRapid: TestRapid =
        TestRapid().apply {
            AktivitetspliktHendelseRiver(this, aktivitetspliktService)
        }

    @Test
    fun `melding om aktivitet leses og lagres`() {
        val meldingJson =
            """
            {
              "@event_name": "AKTIVITETSPLIKT:OPPDATERT",
              "@correlation_id": "67c39566-11c6-4d3c-aeba-ece138cdd9f1",
              "teknisk_tid": "2024-06-28T09:45:07.324828Z",
              "aktivitetsplikt": {
                "sakId": 1003034,
                "avdoedDoedsmaaned": "2024-02",
                "aktivitetsgrad": [],
                "unntak": [
                  {
                    "unntak": "FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT",
                    "fom": null,
                    "tom": null
                  }
                ],
                "brukersAktivitet": [
                  {
                    "typeAktivitet": "ARBEIDSSOEKER",
                    "fom": "2024-07-01",
                    "tom": null
                  }
                ]
              },
              "@id": "fab930a6-4aee-4375-ba7e-558f2cbf072d",
              "@opprettet": "2024-06-28T09:45:07.325495",
            }
            """.trimIndent()

        val inspector = testRapid.apply { sendTestMessage(meldingJson) }.inspektør

        Assertions.assertEquals(0, inspector.size) // Sender ikke ut ny melding
        verify(exactly = 1) {
            aktivitetspliktService.oppdaterVurderingAktivitetsplikt(any(), null)
        }
    }
}
