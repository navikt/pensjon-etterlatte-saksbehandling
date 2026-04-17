package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SigrunModelTest {
    @Test
    fun `erNyHendelse er true naar hendelsetype er null`() {
        val hendelse =
            SkatteoppgjoerHendelse(
                gjelderPeriode = "2024",
                hendelsetype = null,
                identifikator = "123",
                sekvensnummer = 1,
                registreringstidspunkt = null,
            )

        assertTrue(hendelse.erNyHendelse())
    }

    @Test
    fun `erNyHendelse er true naar hendelsetype er NY`() {
        val hendelse =
            SkatteoppgjoerHendelse(
                gjelderPeriode = "2024",
                hendelsetype = SigrunKlient.HENDELSETYPE_NY,
                identifikator = "123",
                sekvensnummer = 1,
                registreringstidspunkt = null,
            )

        assertTrue(hendelse.erNyHendelse())
    }

    @Test
    fun `erNyHendelse er false naar hendelsetype ikke er NY`() {
        val hendelse =
            SkatteoppgjoerHendelse(
                gjelderPeriode = "2024",
                hendelsetype = "SLETTING",
                identifikator = "123",
                sekvensnummer = 1,
                registreringstidspunkt = null,
            )

        assertFalse(hendelse.erNyHendelse())
    }

    @Test
    fun `summertInntekt summerer loenn og naering`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntektSummert(loensinntekt = 120_000, naeringsinntekt = 30_000)

        assertEquals(150_000, pensjonsgivendeInntekt.summertInntekt)
    }

    @Test
    fun `summertInntekt er 0 naar begge inntekter er 0`() {
        val pensjonsgivendeInntekt = PensjonsgivendeInntektSummert(loensinntekt = 0, naeringsinntekt = 0)

        assertEquals(0, pensjonsgivendeInntekt.summertInntekt)
    }
}
