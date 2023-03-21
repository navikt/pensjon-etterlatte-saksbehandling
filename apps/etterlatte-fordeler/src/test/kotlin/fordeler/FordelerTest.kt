package no.nav.etterlatte.fordeler

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_HAR_YRKESSKADE
import no.nav.etterlatte.fordeler.FordelerKriterie.BARN_ER_FOR_GAMMELT
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.readFile
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FordelerTest {

    private val fordelerService = mockk<FordelerService>()
    private val fordelerMetricLogger = mockk<FordelerMetricLogger>()
    private val inspector = TestRapid().apply { Fordeler(this, fordelerService, fordelerMetricLogger) }

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns FordelerResultat.GyldigForBehandling
        every { fordelerService.hentSakId(any(), any()) } returns 1337L
        every { fordelerMetricLogger.logMetricFordelt() } just runs
        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals("soeknad_innsendt", inspector.message(0).get(EVENT_NAME_KEY).asText())
        assertEquals(1337L, inspector.message(0).get(GyldigSoeknadVurdert.sakIdKey).longValue())
        assertEquals("true", inspector.message(0).get(FordelerFordelt.soeknadFordeltKey).asText())

        verify { fordelerMetricLogger.logMetricFordelt() }
    }

    @Test
    fun `skal ikke fordele ugyldig soknad til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns
            FordelerResultat.UgyldigHendelse("Ikke gyldig for behandling")

        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals(0, inspector.size)
    }

    @Test
    fun `skal ikke fordele soknad som ikke oppfyller alle kriterier til behandling`() {
        every { fordelerService.sjekkGyldighetForBehandling(any()) } returns
            FordelerResultat.IkkeGyldigForBehandling(listOf(BARN_ER_FOR_GAMMELT, AVDOED_HAR_YRKESSKADE))
        every { fordelerMetricLogger.logMetricIkkeFordelt(any()) } just runs
        val inspector = inspector.apply { sendTestMessage(BARNEPENSJON_SOKNAD) }.inspektør

        assertEquals(false, inspector.message(0).get(FordelerFordelt.soeknadFordeltKey).asBoolean())
        val allMessages = (0 until inspector.size).map { inspector.message(it) }
        assertTrue {
            allMessages.all {
                it.get(FordelerFordelt.soeknadFordeltKey) == null || !it.get(FordelerFordelt.soeknadFordeltKey)
                    .asBoolean()
            }
        }

        verify(exactly = 1) {
            fordelerMetricLogger.logMetricIkkeFordelt(
                match {
                    it.ikkeOppfylteKriterier.containsAll(listOf(BARN_ER_FOR_GAMMELT, AVDOED_HAR_YRKESSKADE))
                }
            )
        }
    }

    @Test
    fun `skal ikke sjekke soknad av annen type enn barnepensjon`() {
        val inspector = inspector.apply { sendTestMessage(GJENLEVENDE_SOKNAD) }.inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    @Test
    fun `lagStatistikkMelding lager riktig statistikkmelding`() {
        val soeknadId = 1337L
        val fordeler = Fordeler(
            rapidsConnection = mockk(relaxed = true),
            fordelerService = fordelerService,
            fordelerMetricLogger = fordelerMetricLogger
        )
        val packet: JsonMessage = mockk()

        every {
            packet["@lagret_soeknad_id"].longValue()
        } returns soeknadId
        every {
            packet[CORRELATION_ID_KEY].textValue()
        } returns "korrelasjonsid"

        val statistikkMeldingGyldig = fordeler.lagStatistikkMelding(
            packet,
            FordelerResultat.GyldigForBehandling,
            SakType.BARNEPENSJON
        )
        assertJsonEquals(
            """
               {
                    "@correlation_id": "korrelasjonsid",
                    "@event_name": "FORDELER:STATISTIKK",
                    "sak_type": "BARNEPENSJON",
                    "soeknad_id": 1337,
                    "gyldig_for_behandling": true
               } 
            """,
            statistikkMeldingGyldig
        )

        val kriterier = listOf(
            FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE,
            FordelerKriterie.AVDOED_HAR_ADRESSEBESKYTTELSE
        )

        val statistikkmeldingIkkeGyldig = fordeler.lagStatistikkMelding(
            packet,
            FordelerResultat.IkkeGyldigForBehandling(kriterier),
            SakType.BARNEPENSJON
        )

        assertJsonEquals(
            """
                {
                    "@correlation_id": "korrelasjonsid",
                    "@event_name": "FORDELER:STATISTIKK",
                    "soeknad_id": 1337,
                    "sak_type": "BARNEPENSJON",
                    "gyldig_for_behandling": false,
                    "feilende_kriterier": [
                        "BARN_ER_IKKE_BOSATT_I_NORGE",
                        "AVDOED_HAR_ADRESSEBESKYTTELSE"
                    ]
                }
            """,
            statistikkmeldingIkkeGyldig
        )
    }

    @Test
    fun `skal ikke sjekke soknad av annen versjon enn versjon 2`() {
        val inspector = inspector.apply { sendTestMessage(UGYLDIG_VERSJON) }.inspektør

        assertEquals(0, inspector.size)
        verify(exactly = 0) { fordelerService.sjekkGyldighetForBehandling(any()) }
    }

    companion object {
        val BARNEPENSJON_SOKNAD = readFile("/fordeler/soknad_barnepensjon.json")
        val GJENLEVENDE_SOKNAD = readFile("/fordeler/soknad_ikke_barnepensjon.json")
        val UGYLDIG_VERSJON = readFile("/fordeler/soknad_ugyldig_versjon.json")
    }
}

private fun String.asJsonNode(): JsonNode = objectMapper.readTree(this)

// Sammenligning som ignorerer linjeskift og medlemsrekkefølge i råe JSON-strenger
private fun assertJsonEquals(expected: String?, actual: String?) {
    assertEquals(expected?.asJsonNode(), actual?.asJsonNode())
}