package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktOMSService
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val OMS_AVDOED_FNR = Folkeregisteridentifikator.of("10418305857")
private val OMS_GJENLEVENDE_FNR = Folkeregisteridentifikator.of("09498230323")

class DoedshendelseKontrollpunktOMSServiceTest {
    private val pesysKlient = mockk<PesysKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val kontrollpunktService = DoedshendelseKontrollpunktOMSService(pesysKlient, behandlingService)

    private val bruker = systembruker()

    private val doedsdato: LocalDate = LocalDate.now()
    private val doedshendelse =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = OMS_AVDOED_FNR.value,
            avdoedDoedsdato = doedsdato,
            beroertFnr = OMS_GJENLEVENDE_FNR.value,
            relasjon = Relasjon.EKTEFELLE,
            endringstype = Endringstype.OPPRETTET,
        )
    private val avdoed =
        lagOmsPersonDto(foedselsnummer = OMS_AVDOED_FNR, doedsdato = doedsdato)
    private val gjenlevende =
        lagOmsPersonDto(foedselsnummer = OMS_GJENLEVENDE_FNR)
    private val sak =
        Sak(
            ident = doedshendelse.beroertFnr,
            sakType = SakType.OMSTILLINGSSTOENAD,
            id = SakId(1L),
            enhet = Enheter.defaultEnhet.enhetNr,
            adressebeskyttelse = null,
            erSkjermet = false,
        )

    @Test
    fun `Skal opprette kontrollpunkt naar identifisert gjenlevende er over 67 aar`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()
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
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()
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
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()
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

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeUfoeretrygdIPesysEps)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt dersom ytelse i pesys ikke har en fra og med dato`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = null,
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
        verify(exactly = 0) { behandlingService.hentSisteIverksatteBehandling(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
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
        verify(exactly = 0) { behandlingService.hentSisteIverksatteBehandling(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom identifisert gjenlevende har behandling i Gjenny`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns
            listOf(lagOmsForstegangsbehandling(sak))

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                sak,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsHarSoektOMS(sak))
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sak.id) }
        confirmVerified(behandlingService)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkter dersom det ikke er noen avvik og det ikke finnes noen sak`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                null,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldBe emptyList()
        verify(exactly = 0) { behandlingService.hentSisteIverksatteBehandling(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkter dersom det ikke er noen avvik og det finnes en sak`() {
        coEvery { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) } returns emptyList()
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns emptyList()

        val kontrollpunkter =
            kontrollpunktService.identifiser(
                doedshendelse,
                sak,
                gjenlevende,
                avdoed,
                bruker,
            )

        kontrollpunkter shouldBe emptyList()
        verify(exactly = 1) { behandlingService.hentBehandlingerForSak(sak.id) }
        coVerify(exactly = 1) { pesysKlient.hentSaker(doedshendelse.beroertFnr, bruker) }
        confirmVerified(behandlingService, pesysKlient)
    }
}

private fun lagOmsPersonDto(
    foedselsnummer: Folkeregisteridentifikator,
    foedselsdato: LocalDate? = LocalDate.now().minusYears(45),
    doedsdato: LocalDate? = null,
    familieRelasjon: OpplysningDTO<FamilieRelasjon>? = null,
): PersonDoedshendelseDto =
    PersonDoedshendelseDto(
        foedselsnummer = OpplysningDTO(foedselsnummer, null),
        foedselsdato = foedselsdato?.let { OpplysningDTO(it, null) },
        foedselsaar = null,
        doedsdato = doedsdato?.let { OpplysningDTO(it, null) },
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        sivilstand = null,
        utland = null,
        familieRelasjon = familieRelasjon,
        avdoedesBarn = null,
        avdoedesBarnUtenIdent = null,
    )

private fun lagOmsForstegangsbehandling(sak: Sak): Foerstegangsbehandling =
    Foerstegangsbehandling(
        id = UUID.randomUUID(),
        sak = sak,
        behandlingOpprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        status = BehandlingStatus.IVERKSATT,
        kommerBarnetTilgode = null,
        virkningstidspunkt = null,
        utlandstilknytning = null,
        boddEllerArbeidetUtlandet = null,
        soeknadMottattDato = null,
        gyldighetsproeving = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        sendeBrev = true,
    )
