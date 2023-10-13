package no.nav.etterlatte.rapidsandrivers.migrering

import io.mockk.mockk
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

internal class MigreringRequestTest {
    @Test
    fun `feiler hvis kun en avdoed forelder og gjenlevende mangler`() {
        assertThrows<IllegalStateException> {
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet(""),
                soeker = Folkeregisteridentifikator.of("01448203510"),
                gjenlevendeForelder = null,
                avdoedForelder =
                    listOf(
                        AvdoedForelder(
                            ident = Folkeregisteridentifikator.of("01448203510"),
                            doedsdato = Tidspunkt.now(),
                        ),
                    ),
                virkningstidspunkt = YearMonth.now(),
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning = mockk(),
                trygdetid = mockk(),
                spraak = Spraak.NN,
            )
        }
    }
}
