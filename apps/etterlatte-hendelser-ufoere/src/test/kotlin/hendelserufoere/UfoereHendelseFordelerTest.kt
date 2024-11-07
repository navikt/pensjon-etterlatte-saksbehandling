package no.nav.etterlatte.hendelserufoere

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UfoereHendelseFordelerTest {
    private val kafkaProduser: KafkaProdusent<String, JsonMessage> = mockk()
    private lateinit var ufoereHendelseFordeler: UfoereHendelseFordeler

    @BeforeEach
    fun setup() {
        coEvery { kafkaProduser.publiser(any(), any()) } returns mockk(relaxed = true)

        ufoereHendelseFordeler = UfoereHendelseFordeler(kafkaProduser)
    }

    @Test
    fun `skal håndtere ufoerehendelse der bruker er mellom og 18 og 21 på virkningstidspunkt`() {
        val attenAarIMaaneder = 12 * 18

        val ufoereHendelse: UfoereHendelse =
            UfoereHendelse().apply {
                personidentifikator = "12312312312"
                ytelse = "ufoere"
                virkningstidspunkt = "2021-01-01"
                alderVedVirkningstidspunkt = attenAarIMaaneder
                hendelsestype = "ufoere"
            }

        ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
    }

    @Test
    fun `skal ignorere ufoerehendelse der bruker ikke er mellom og 18 og 21 på virkningstidspunkt`() {
        val tolvAarIMaaneder = 12 * 12

        val ufoereHendelse: UfoereHendelse =
            UfoereHendelse().apply {
                personidentifikator = "12312312312"
                ytelse = "ufoere"
                virkningstidspunkt = "2021-01-01"
                alderVedVirkningstidspunkt = tolvAarIMaaneder
                hendelsestype = "ufoere"
            }

        ufoereHendelseFordeler.haandterHendelse(ufoereHendelse)
    }
}
