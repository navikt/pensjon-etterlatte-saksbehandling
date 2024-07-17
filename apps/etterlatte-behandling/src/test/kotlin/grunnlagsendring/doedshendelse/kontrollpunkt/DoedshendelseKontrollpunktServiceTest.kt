package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.LITE_BARN
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoServiceTest.Companion.bruker
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.ktor.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DoedshendelseKontrollpunktServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val pesysKlient = mockk<PesysKlient>()
    private val oppgaveService = mockk<OppgaveService>()
    private val sakService = mockk<SakService>()
    private val grunnlagsendringshendelseDao = mockk<GrunnlagsendringshendelseDao>()
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
            avdoedFnr = KONTANT_FOT.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = LITE_BARN.value,
            relasjon = Relasjon.BARN,
            endringstype = Endringstype.OPPRETTET,
        )

    private val doedshendelseInternalOMS =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = KONTANT_FOT.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = JOVIAL_LAMA.value,
            relasjon = Relasjon.EKTEFELLE,
            endringstype = Endringstype.OPPRETTET,
        )

    @BeforeEach
    fun oppsett() {
        coEvery { pesysKlient.hentSaker(doedshendelseInternalBP.beroertFnr, any()) } returns emptyList()
        coEvery { pesysKlient.hentSaker(doedshendelseInternalOMS.beroertFnr, any()) } returns emptyList()
        coEvery {
            pesysKlient.erTilstoetendeBehandlet(
                doedshendelseInternalOMS.beroertFnr,
                any(),
                simpleSaksbehandler(),
            )
        } returns false
        coEvery {
            pesysKlient.erTilstoetendeBehandlet(
                doedshendelseInternalBP.beroertFnr,
                any(),
                simpleSaksbehandler(),
            )
        } returns false

        every { behandlingService.hentSisteIverksatte(any()) } returns null
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
            )
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr), null),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KONTANT_FOT, JOVIAL_LAMA),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr), null),
            )
        every { sakService.finnSak(any(), any()) } returns null
        every {
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                any(),
                any(),
            )
        } returns emptyList()
        every { oppgaveService.hentOppgaverForReferanse(any()) } returns emptyList()
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarYtelse dersom relasjon avdød og har sak med iverksatt behandling`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sak = Sak(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD, 1L, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every {
            behandlingService.hentSisteIverksatte(
                sak.id,
            )
        } returns foerstegangsbehandling(sakId = sak.id, status = BehandlingStatus.IVERKSATT)
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarYtelse(sak))
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarIkkeYtelse dersom relasjon avdød og kun har sak ikke iverksatt behandling`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sak = Sak(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD, 1L, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every { behandlingService.hentSisteIverksatte(sak.id) } returns null
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarIkkeYtelse dersom relasjon avdoed og ikke har noen sak`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        every {
            sakService.finnSaker(
                any(),
            )
        } returns emptyList()

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarIkkeYtelse)
    }

    @Test
    fun `Skal gi kontrollpunkt AvdoedHarYtelse, DuplikatGrunnlagsendringsHendelse for avdød med tidligere hendelse`() {
        val doedshendelseInternalAvdoed =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.AVDOED,
                endringstype = Endringstype.OPPRETTET,
            )
        val sakIdd = 1L
        val sak = Sak(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD, sakIdd, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSaker(
                doedshendelseInternalAvdoed.avdoedFnr,
            )
        } returns listOf(sak)
        every {
            behandlingService.hentSisteIverksatte(
                sakIdd,
            )
        } returns foerstegangsbehandling(sakId = sakIdd, status = BehandlingStatus.IVERKSATT)
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalAvdoed.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalAvdoed.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalAvdoed.avdoedDoedsdato, null),
            )

        val oppgaveIntern =
            mockk<OppgaveIntern> {
                every { id } returns UUID.randomUUID()
            }
        val grunnlagshendelseID = UUID.randomUUID()
        val grunnlagsendringshendelse =
            mockk<Grunnlagsendringshendelse> {
                every { id } returns grunnlagshendelseID
                every { gjelderPerson } returns doedshendelseInternalAvdoed.avdoedFnr
                every { type } returns GrunnlagsendringsType.DOEDSFALL
                every { sakId } returns sakIdd
            }

        every {
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns listOf(grunnlagsendringshendelse)
        every { oppgaveService.hentOppgaverForReferanse(grunnlagshendelseID.toString()) } returns listOf(oppgaveIntern)

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalAvdoed,
                bruker,
            )

        kontrollpunkter shouldContainExactlyInAnyOrder
            listOf(
                DoedshendelseKontrollpunkt.AvdoedHarYtelse(sak),
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(grunnlagsendringshendelse.id, oppgaveIntern.id),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom det eksisterer en duplikat grunnlagsendringshendelse`() {
        val sak =
            Sak(
                ident = doedshendelseInternalBP.beroertFnr,
                sakType = doedshendelseInternalBP.sakTypeForEpsEllerBarn(),
                id = 1L,
                enhet = "0000",
            )
        val oppgaveIntern =
            mockk<OppgaveIntern> {
                every { id } returns UUID.randomUUID()
            }
        val grunnlagsendringshendelseId = UUID.randomUUID()
        val grunnlagsendringshendelse =
            mockk<Grunnlagsendringshendelse> {
                every { id } returns grunnlagsendringshendelseId
                every { gjelderPerson } returns doedshendelseInternalBP.avdoedFnr
                every { type } returns GrunnlagsendringsType.DOEDSFALL
            }
        every { sakService.finnSak(any(), any()) } returns sak
        every {
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns listOf(grunnlagsendringshendelse)
        every { oppgaveService.hentOppgaverForReferanse(grunnlagsendringshendelseId.toString()) } returns listOf(oppgaveIntern)

        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalBP,
                bruker,
            )

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(
                    grunnlagsendringshendelseId = grunnlagsendringshendelse.id,
                    oppgaveId = oppgaveIntern.id,
                ),
            )
    }

    @Test
    fun `Skal gi kontrollpunkt dersom gjenlevende ikke har aktiv adresse`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.BARN,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr), null),
                bostedsadresse = listOf(OpplysningDTO(Adresse(AdresseType.VEGADRESSE, false, kilde = "FREG"), null)),
                kontaktadresse = emptyList(),
                oppholdsadresse = emptyList(),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KONTANT_FOT, JOVIAL_LAMA),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )
        val kontrollpunkter =
            kontrollpunktService.identifiserKontrollerpunkter(
                doedshendelseInternalBP,
                bruker,
            )

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.GjenlevendeManglerAdresse)
    }

    @Test
    fun `Skal opprette kontrollpunkt hvis saken er behandlet i Pesys`() {
        coEvery {
            pesysKlient.erTilstoetendeBehandlet(
                doedshendelseInternalBP.beroertFnr,
                any(),
                bruker,
            )
        } returns true

        kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP, bruker) shouldBe
            listOf(DoedshendelseKontrollpunkt.TilstoetendeBehandletIPesys)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt hvis alle sjekker er OK`() {
        kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP, bruker) shouldBe emptyList()
    }
}
