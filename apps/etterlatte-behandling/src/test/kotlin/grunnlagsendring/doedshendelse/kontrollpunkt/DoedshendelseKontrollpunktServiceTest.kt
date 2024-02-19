package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseKontrollpunktServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val pesysKlient = mockk<PesysKlient>()
    private val kontrollpunktService = DoedshendelseKontrollpunktService(pdlTjenesterKlient, pesysKlient)
    private val doedshendelse =
        Doedshendelse.nyHendelse(
            avdoedFnr = "12345678901",
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = "12345678901",
            relasjon = Relasjon.BARN,
        )

    @Test
    fun `Skal returnere kontrollpunkt hvis avdoed ikke har doedsdato i PDL`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.avdoedFnr) } returns emptyList()
        every { pdlTjenesterKlient.hentPdlModell(doedshendelse.avdoedFnr, PersonRolle.AVDOED, any()) } returns
            mockk {
                every { doedsdato } returns null
            }

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelse)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den beroerte har ufoeretrygd`() {
        every { pdlTjenesterKlient.hentPdlModell(doedshendelse.avdoedFnr, PersonRolle.AVDOED, any()) } returns
            mockk {
                every { doedsdato } returns OpplysningDTO(doedshendelse.avdoedDoedsdato, null)
            }
        coEvery { pesysKlient.hentSaker(doedshendelse.avdoedFnr) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now(),
                    tomDate = null,
                ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelse)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeYtelseIPesys)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt hvis alle sjekker er OK`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.avdoedFnr) } returns emptyList()
        every { pdlTjenesterKlient.hentPdlModell(doedshendelse.avdoedFnr, PersonRolle.AVDOED, any()) } returns
            mockk {
                every { doedsdato } returns OpplysningDTO(doedshendelse.avdoedDoedsdato, null)
            }

        kontrollpunktService.identifiserKontrollerpunkter(doedshendelse) shouldBe emptyList()
    }
}
