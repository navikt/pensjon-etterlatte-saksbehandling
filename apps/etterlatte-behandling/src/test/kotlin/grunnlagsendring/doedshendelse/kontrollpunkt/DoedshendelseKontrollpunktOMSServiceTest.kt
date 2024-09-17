package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktOMSService
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseKontrollpunktOMSServiceTest {
    private val pesysKlient = mockk<PesysKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val kontrollpunktService = DoedshendelseKontrollpunktOMSService(pesysKlient, behandlingService)

    private val bruker = systembruker()

    @Test
    fun `Skal opprette kontrollpunkt naar identifisert gjenlevende er over 67 aar`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()
        val gjenlevende = gjenlevende.copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(68), null))

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                hendelse = doedshendelse,
                sak = null,
                eps = gjenlevende,
                avdoed = avdoed,
                bruker = bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon)
    }

    @Test
    fun `Skal opprettet kontrollpunkt dersom identifisert gjenlevende ikke lever`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()
        val gjenlevende = gjenlevende.copy(doedsdato = OpplysningDTO(LocalDate.now(), null))

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                null,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsHarDoedsdato)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom identifisert gjenlevende har kryssende ytelse i Pesys`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now().minusMonths(2),
                    tomDate = null,
                ),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                null,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeYtelseIPesysEps)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt dersom identifisert gjenlevende har kryssende ytelse i Pesys frem i tid`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now().plusMonths(2),
                    tomDate = null,
                ),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                null,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldBe emptyList()
        verify(exactly = 0) { behandlingService.hentSisteIverksatte(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom identifisert gjenlevende har iverksatt behandling i Gjenny`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()
        every { behandlingService.hentSisteIverksatte(sak.id) } returns mockk()

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                sak,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsHarSakMedIverksattBehandlingIGjenny(sak))
        verify(exactly = 1) { behandlingService.hentSisteIverksatte(sak.id) }
        confirmVerified(behandlingService)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkter dersom det ikke er noen avvik og det ikke finnes noen sak`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                null,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldBe emptyList()
        verify(exactly = 0) { behandlingService.hentSisteIverksatte(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkter dersom det ikke er noen avvik og det finnes en sak`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns listOf()
        every { behandlingService.hentSisteIverksatte(sak.id) } returns null

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                sak,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldBe emptyList()
        verify(exactly = 1) { behandlingService.hentSisteIverksatte(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }

    companion object {
        private val doedsdato: LocalDate = LocalDate.now()
        private val doedshendelse =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = doedsdato,
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.EKTEFELLE,
                endringstype = Endringstype.OPPRETTET,
            )
        private val avdoed =
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
                doedsdato = OpplysningDTO(doedsdato, null),
            )
        private val gjenlevende =
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
            )
        private val sak =
            Sak(
                ident = doedshendelse.beroertFnr,
                sakType = SakType.OMSTILLINGSSTOENAD,
                id = 1L,
                enhet = Enhet.defaultEnhet.enhetNr,
            )
    }
}
