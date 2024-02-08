package grunnlagsendring

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.BeroertePersonerVedDoedsfallService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.personOpplysning
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BeroertePersonerVedDoedsfallServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val service = BeroertePersonerVedDoedsfallService(pdlTjenesterKlient = pdlTjenesterKlient)

    @Test
    fun `Skal returnere barna som kan ha rett paa barnepensjon ved doedsfall`() {
        val avdoed =
            mockPerson().copy(
                doedsdato = OpplysningDTO(LocalDate.of(2022, 8, 17), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2000, 1, 1)),
                        personOpplysning(foedselsdato = LocalDate.of(2002, 8, 30)),
                        personOpplysning(foedselsdato = LocalDate.of(2002, 9, 15)),
                        personOpplysning(foedselsdato = LocalDate.of(2005, 9, 15), doedsdato = LocalDate.of(2020, 8, 17)),
                        personOpplysning(foedselsdato = null).copy(foedselsnummer = Folkeregisteridentifikator.of("26061363474")),
                        personOpplysning(foedselsdato = LocalDate.of(2020, 9, 15)),
                    ),
            )
        every { pdlTjenesterKlient.hentPdlModell(avdoed.foedselsnummer.verdi.value, any(), any()) } returns avdoed

        val beroerteBarn = service.hentBeroertePersoner(avdoed.foedselsnummer.verdi.value)

        beroerteBarn.size shouldBe 3
    }

    @Test
    fun `Skal kaste feil hvis personen ikke er doed`() {
        val avdoed = mockPerson()
        every { pdlTjenesterKlient.hentPdlModell(avdoed.foedselsnummer.verdi.value, any(), any()) } returns avdoed

        shouldThrow<AssertionError> {
            service.hentBeroertePersoner(avdoed.foedselsnummer.verdi.value)
        }
    }
}
