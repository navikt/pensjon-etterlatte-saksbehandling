package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporing
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val KS_AVDOED_FNR = Folkeregisteridentifikator.of("10418305857")
private val KS_GJENLEVENDE_FNR = Folkeregisteridentifikator.of("09498230323")
private val KS_BARN_FNR = Folkeregisteridentifikator.of("22511075258")
private val KS_SAK_ID = SakId(1L)

class DoedshendelseKontrollpunktServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val pesysKlient = mockk<PesysKlient>()
    private val oppgaveDao = mockk<OppgaveDaoMedEndringssporing>()
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = oppgaveDao,
            sakDao = mockk<SakLesDao>(),
            hendelseDao = mockk<HendelseDao>(),
            hendelser = mockk<BehandlingHendelserKafkaProducer>(),
            saksbehandlerService = mockk<SaksbehandlerService>(),
        )
    private val sakService = mockk<SakService>()
    private val grunnlagsendringshendelseDao = FakeGrunnlagsendringshendelseDao()
    private val behandlingService = mockk<BehandlingService>()
    private val kontrollpunktService =
        DoedshendelseKontrollpunktService(
            pdlTjenesterKlient = pdlTjenesterKlient,
            grunnlagsendringshendelseDao = grunnlagsendringshendelseDao,
            oppgaveService = oppgaveService,
            sakService = sakService,
            pesysKlient = pesysKlient,
            behandlingService = behandlingService,
        )
    private val doedshendelseInternalBP =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = KS_AVDOED_FNR.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = KS_BARN_FNR.value,
            relasjon = Relasjon.BARN,
            endringstype = Endringstype.OPPRETTET,
        )

    private val doedshendelseInternalOMS =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = KS_AVDOED_FNR.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = KS_GJENLEVENDE_FNR.value,
            relasjon = Relasjon.EKTEFELLE,
            endringstype = Endringstype.OPPRETTET,
        )

    private val bruker = systembruker()

    @BeforeEach
    fun oppsett() {
        grunnlagsendringshendelseDao.reset()
        every { oppgaveDao.hentOppgaverForReferanse(any()) } returns emptyList()
        coEvery { pesysKlient.hentSaker(doedshendelseInternalBP.beroertFnr, bruker) } returns emptyList()
        coEvery { pesysKlient.hentSaker(doedshendelseInternalOMS.beroertFnr, bruker) } returns emptyList()

        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalBP.avdoedFnr),
                        null,
                    ),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
            )
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr),
                        null,
                    ),
                foedselsdato = OpplysningDTO(LocalDate.now().minusYears(5), null),
                bostedsadresse =
                    listOf(
                        OpplysningDTO(
                            Adresse(AdresseType.VEGADRESSE, true, kilde = "FREG"),
                            null,
                        ),
                    ),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KS_AVDOED_FNR, KS_GJENLEVENDE_FNR),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = KS_GJENLEVENDE_FNR.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr),
                        null,
                    ),
            )
        every { sakService.finnSak(any(), any()) } returns null
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarYtelse dersom relasjon avdød og har sak med iverksatt behandling`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KS_AVDOED_FNR.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = KS_GJENLEVENDE_FNR.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sak =
            Sak(KS_AVDOED_FNR.value, SakType.OMSTILLINGSSTOENAD, KS_SAK_ID, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every {
            behandlingService.hentSisteIverksatteBehandling(
                sak.id,
            )
        } returns lagKsForstegangsbehandling(sakId = sak.id, status = BehandlingStatus.IVERKSATT)
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr),
                        null,
                    ),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarYtelse(sak))
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarIkkeYtelse dersom relasjon avdød og kun har sak ikke iverksatt behandling`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KS_AVDOED_FNR.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = KS_GJENLEVENDE_FNR.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sak =
            Sak(KS_AVDOED_FNR.value, SakType.OMSTILLINGSSTOENAD, KS_SAK_ID, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every { behandlingService.hentSisteIverksatteBehandling(sak.id) } returns null
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr),
                        null,
                    ),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarIkkeYtelse dersom relasjon avdoed og ikke har noen sak`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KS_AVDOED_FNR.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = KS_GJENLEVENDE_FNR.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        every {
            sakService.finnSaker(
                any(),
            )
        } returns emptyList()

        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr),
                        null,
                    ),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarYtelse, DuplikatGrunnlagsendringsHendelse for avdød med tidligere hendelse`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KS_AVDOED_FNR.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = KS_GJENLEVENDE_FNR.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sakIdd = KS_SAK_ID
        val sak =
            Sak(KS_AVDOED_FNR.value, SakType.OMSTILLINGSSTOENAD, sakIdd, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every {
            behandlingService.hentSisteIverksatteBehandling(
                sakIdd,
            )
        } returns lagKsForstegangsbehandling(sakId = sakIdd, status = BehandlingStatus.IVERKSATT)
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalAvdoed.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr),
                        null,
                    ),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val grunnlagshendelseID = UUID.randomUUID()
        val grunnlagsendringshendelse =
            Grunnlagsendringshendelse(
                id = grunnlagshendelseID,
                sakId = KS_SAK_ID,
                type = GrunnlagsendringsType.DOEDSFALL,
                opprettet = LocalDateTime.now(),
                hendelseGjelderRolle = Saksrolle.AVDOED,
                gjelderPerson = doedshendelseInternalAvdoed.avdoedFnr,
            )

        grunnlagsendringshendelseDao.settHendelser(listOf(grunnlagsendringshendelse))

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactlyInAnyOrder
            listOf(
                DoedshendelseKontrollpunkt.AvdoedHarYtelse(sak),
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(
                    grunnlagsendringshendelse.id,
                    oppgaveId = null,
                ),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom det eksisterer en duplikat grunnlagsendringshendelse`() {
        val sak =
            Sak(
                ident = doedshendelseInternalBP.beroertFnr,
                sakType = doedshendelseInternalBP.sakTypeForEpsEllerBarn(),
                id = KS_SAK_ID,
                enhet = Enheter.defaultEnhet.enhetNr,
                null,
                false,
            )
        val grunnlagsendringshendelseId = UUID.randomUUID()
        val grunnlagsendringshendelse =
            Grunnlagsendringshendelse(
                id = grunnlagsendringshendelseId,
                sakId = KS_SAK_ID,
                type = GrunnlagsendringsType.DOEDSFALL,
                opprettet = LocalDateTime.now(),
                hendelseGjelderRolle = Saksrolle.AVDOED,
                gjelderPerson = doedshendelseInternalBP.avdoedFnr,
            )
        every { sakService.finnSak(any(), any()) } returns sak
        grunnlagsendringshendelseDao.settHendelser(listOf(grunnlagsendringshendelse))
        every { behandlingService.hentBehandlingerForSak(KS_SAK_ID) } returns emptyList()

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalBP,
                bruker,
            )

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(
                    grunnlagsendringshendelseId = grunnlagsendringshendelse.id,
                    oppgaveId = null,
                ),
            )
    }

    @Test
    fun `Skal gi kontrollpunkt dersom gjenlevende ikke har aktiv adresse`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer =
                    OpplysningDTO(
                        Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr),
                        null,
                    ),
                foedselsdato = OpplysningDTO(LocalDate.now().minusYears(5), null),
                bostedsadresse =
                    listOf(
                        OpplysningDTO(
                            Adresse(AdresseType.VEGADRESSE, false, kilde = "FREG"),
                            null,
                        ),
                    ),
                kontaktadresse = emptyList(),
                oppholdsadresse = emptyList(),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KS_AVDOED_FNR, KS_GJENLEVENDE_FNR),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )
        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollpunkter(
                doedshendelseInternalBP,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.GjenlevendeManglerAdresse)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt hvis alle sjekker er OK`() {
        kontrollpunktService.identifiserKontrollpunkter(doedshendelseInternalBP, bruker) shouldBe emptyList()
    }

    @Test
    fun `Skal gi kontrollpunkt BarnForGammeltForBarnepensjon naar barn er 61 aar`() {
        every {
            pdlTjenesterKlient.hentPdlModellDoedshendelseForSaktype(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            lagKsPersonDto().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr), null),
                foedselsdato = OpplysningDTO(LocalDate.now().minusYears(61), null),
                bostedsadresse = listOf(OpplysningDTO(Adresse(AdresseType.VEGADRESSE, true, kilde = "FREG"), null)),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollpunkter(doedshendelseInternalBP, bruker)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnForGammeltForBarnepensjon)
    }
}

private fun lagKsPersonDto(
    foedselsnummer: Folkeregisteridentifikator = KS_AVDOED_FNR,
    doedsdato: LocalDate? = null,
): PersonDoedshendelseDto =
    PersonDoedshendelseDto(
        foedselsnummer = OpplysningDTO(foedselsnummer, null),
        foedselsdato = OpplysningDTO(LocalDate.now().minusYears(45), null),
        foedselsaar = null,
        doedsdato = doedsdato?.let { OpplysningDTO(it, null) },
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        sivilstand = null,
        utland = null,
        familieRelasjon = null,
        avdoedesBarn = null,
        avdoedesBarnUtenIdent = null,
    )

private fun lagKsForstegangsbehandling(
    sakId: SakId,
    status: BehandlingStatus = BehandlingStatus.IVERKSATT,
): Foerstegangsbehandling =
    Foerstegangsbehandling(
        id = UUID.randomUUID(),
        sak =
            Sak(
                ident = KS_AVDOED_FNR.value,
                sakType = SakType.BARNEPENSJON,
                id = sakId,
                enhet = Enheter.defaultEnhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            ),
        behandlingOpprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        status = status,
        kommerBarnetTilgode = null,
        virkningstidspunkt = null,
        utlandstilknytning = null,
        boddEllerArbeidetUtlandet = null,
        soeknadMottattDato = null,
        gyldighetsproeving = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        sendeBrev = true,
    )

/**
 * Fake DAO som omgår MockK sitt problem med value class-parametere (SakId → mangled JVM-navn).
 * Bruk [settHendelser] for å styre hva som returneres fra hentGrunnlagsendringshendelserMedStatuserISak.
 */
private class FakeGrunnlagsendringshendelseDao : GrunnlagsendringshendelseDao(NoopConnectionAutoclosing) {
    private var hendelser: List<Grunnlagsendringshendelse> = emptyList()

    fun settHendelser(h: List<Grunnlagsendringshendelse>) {
        hendelser = h
    }

    fun reset() {
        hendelser = emptyList()
    }

    override fun hentGrunnlagsendringshendelserMedStatuserISak(
        sakId: SakId,
        statuser: List<GrunnlagsendringStatus>,
    ): List<Grunnlagsendringshendelse> = hendelser
}

private object NoopConnectionAutoclosing : ConnectionAutoclosing() {
    override fun <T> hentConnection(block: (java.sql.Connection) -> T): T = error("Not implemented in tests")

    override fun <T> hentKotliquerySession(block: (kotliquery.Session) -> T): T = error("Not implemented in tests")
}
