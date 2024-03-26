package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktAvdoedService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseKontrollpunktAvdoedServiceTest {
    private val kontrollpunktService = DoedshendelseKontrollpunktAvdoedService()

    @Test
    fun `Skal returnere kontrollpunkt hvis avdoed ikke har doedsdato i PDL`() {
        val avdoed = mockPerson().copy(doedsdato = null)

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede har D-nummer`() {
        val avdoed =
            mockPerson().copy(
                doedsdato = OpplysningDTO(LocalDate.now(), null),
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of("69057949961"), null),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarDNummer)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede hadde utvandring`() {
        val avdoed =
            mockPerson(
                Utland(
                    innflyttingTilNorge = emptyList(),
                    utflyttingFraNorge = listOf(UtflyttingFraNorge("Sverige", LocalDate.now().minusMonths(2))),
                ),
            ).copy(
                doedsdato = OpplysningDTO(LocalDate.now(), null),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarUtvandret)
    }
}
