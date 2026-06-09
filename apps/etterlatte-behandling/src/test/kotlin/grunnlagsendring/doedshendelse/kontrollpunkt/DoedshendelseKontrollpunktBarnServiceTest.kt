package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktBarnService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DoedshendelseKontrollpunktBarnServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val kontrollpunktService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)

    private val avdoedFnr = Folkeregisteridentifikator.of("10418305857")
    private val gjenlevendeFnr = Folkeregisteridentifikator.of("09498230323")
    private val barnFnr = Folkeregisteridentifikator.of("22511075258")

    private val doedsdato = LocalDate.now()
    private val doedshendelse =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = avdoedFnr.value,
            avdoedDoedsdato = doedsdato,
            beroertFnr = barnFnr.value,
            relasjon = Relasjon.BARN,
            endringstype = Endringstype.OPPRETTET,
        )
    private val avdoed = lagPersonDto(foedselsnummer = avdoedFnr, doedsdato = doedsdato)
    private val gjenlevende = lagPersonDto(foedselsnummer = gjenlevendeFnr)

    private val barnFamilieRelasjon =
        OpplysningDTO(
            FamilieRelasjon(
                ansvarligeForeldre = emptyList(),
                foreldre = listOf(avdoedFnr, gjenlevendeFnr),
                barn = emptyList(),
            ),
            null,
        )
    private val barnet =
        lagPersonDto(
            foedselsnummer = gjenlevendeFnr,
            foedselsdato = LocalDate.now().minusYears(15),
            familieRelasjon = barnFamilieRelasjon,
        )
    private val barnOver20 =
        lagPersonDto(
            foedselsnummer = gjenlevendeFnr,
            foedselsdato = LocalDate.now().minusYears(20),
            familieRelasjon = barnFamilieRelasjon,
        )
    private val barn61Aar =
        lagPersonDto(
            foedselsnummer = gjenlevendeFnr,
            foedselsdato = LocalDate.now().minusYears(61),
            familieRelasjon = barnFamilieRelasjon,
        )
    private val sak =
        Sak(
            ident = barnFnr.value,
            sakType = SakType.BARNEPENSJON,
            id = SakId(1L),
            enhet = Enheter.defaultEnhet.enhetNr,
            adressebeskyttelse = null,
            erSkjermet = false,
        )

    @Test
    fun `Skal avbryte dersom barn er 20 aar eller eldre i dag`() {
        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barnOver20)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnForGammeltForBarnepensjon)
    }

    @Test
    fun `Skal avbryte dersom barn er godt voksent - historisk doedsdato korrigert i PDL`() {
        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barn61Aar)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnForGammeltForBarnepensjon)
    }

    @Test
    fun `Skal opprette kontrollpunkt ved samtidig doedsfall`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = gjenlevendeFnr.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende.copy(doedsdato = OpplysningDTO(doedsdato, null))

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barnet)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamtidigDoedsfall)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom vi ikke finner den andre forelderen`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = gjenlevendeFnr.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende.copy(doedsdato = OpplysningDTO(doedsdato, null))

        val barnetMedEnForelder =
            barnet.copy(
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(avdoedFnr),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barnetMedEnForelder)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet)
    }

    @Test
    fun `Skal opprette kontrollpunkt ved eksisterende barnepensjonssak`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = gjenlevendeFnr.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende

        val behandling = lagForstegangsbehandling(sak)
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns listOf(behandling)

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, sak, barnet)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnErSoektFor(sak))
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt naar ingen eksisterende behandlinger`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = gjenlevendeFnr.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende

        every { behandlingService.hentBehandlingerForSak(sak.id) } returns emptyList()

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, sak, barnet)

        kontrollpunkter shouldBe emptyList()
    }

    @Test
    fun `Skal ikke avbryte naar foedselsdato mangler - lander paa den sikre siden`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = gjenlevendeFnr.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende

        every { behandlingService.hentBehandlingerForSak(sak.id) } returns emptyList()

        val barnUtenFoedselsdato = barnet.copy(foedselsdato = null, foedselsaar = null)
        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, sak, barnUtenFoedselsdato)

        kontrollpunkter shouldBe emptyList()
    }
}

private fun lagPersonDto(
    foedselsnummer: Folkeregisteridentifikator,
    foedselsdato: LocalDate? = null,
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

private fun lagForstegangsbehandling(sak: Sak): Foerstegangsbehandling =
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
