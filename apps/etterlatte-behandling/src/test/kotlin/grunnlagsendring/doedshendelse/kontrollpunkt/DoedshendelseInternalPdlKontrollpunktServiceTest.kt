package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DoedshendelseInternalPdlKontrollpunktServiceTest {
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
            beroertFnr = JOVIAL_LAMA.value,
            relasjon = Relasjon.BARN,
            endringstype = Endringstype.OPPRETTET,
        )

    private val doedshendelseInternalOMS =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = KONTANT_FOT.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = JOVIAL_LAMA.value,
            relasjon = Relasjon.EPS,
            endringstype = Endringstype.OPPRETTET,
        )

    @BeforeEach
    fun oppsett() {
        coEvery { pesysKlient.hentSaker(doedshendelseInternalBP.beroertFnr) } returns emptyList()

        every { behandlingService.hentSisteIverksatte(any()) } returns null
        every {
            pdlTjenesterKlient.hentPdlModell(
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
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = any(),
                rolle = PersonRolle.BARN,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
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
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr), null),
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
    fun `Skal returnere kontrollpunkt BarnHarBarnepensjon for relasjon barn og har BP`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns mockPerson().copy(doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null))
        val sakId = 1L
        val sak =
            Sak(
                doedshendelseInternalBP.beroertFnr,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )
        every { sakService.finnSak(any(), any()) } returns sak
        every { behandlingService.hentSisteIverksatte(any()) } returns foerstegangsbehandling(sakId = sakId)

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)
        val sakEksistererIntern = DoedshendelseKontrollpunkt.SakEksistererIGjenny(sak)
        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnHarBarnepensjon, sakEksistererIntern)
    }

    @Test
    fun `Skal returnere kontrollpunkt BarnHarUfoereTrygd for relasjon barn og har uføretrygd`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns mockPerson().copy(doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null))
        coEvery { pesysKlient.hentSaker(doedshendelseInternalBP.beroertFnr) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now().minusMonths(2),
                    tomDate = null,
                ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnHarUfoereTrygd)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis avdoed ikke har doedsdato i PDL`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den beroerte har ufoeretrygd`() {
        coEvery { pesysKlient.hentSaker(doedshendelseInternalOMS.beroertFnr) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now().minusMonths(2),
                    tomDate = null,
                ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeYtelseIPesys)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede hadde utvandring`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson(
                Utland(
                    innflyttingTilNorge = emptyList(),
                    utflyttingFraNorge = listOf(UtflyttingFraNorge("Sverige", LocalDate.now().minusMonths(2))),
                ),
            ).copy(
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarUtvandret)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede har D-nummer`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of("69057949961"), null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarDNummer)
    }

    @Test
    fun `Skal opprette kontrollpunkt ved samtidig doedsfall`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternalBP.beroertFnr,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalBP.beroertFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamtidigDoedsfall)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom vi ikke finner den andre forelderen`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = any(),
                rolle = PersonRolle.BARN,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KONTANT_FOT),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom det eksisterer en sak fra foer`() {
        val sak =
            Sak(
                ident = doedshendelseInternalBP.beroertFnr,
                sakType = doedshendelseInternalBP.sakType(),
                id = 1L,
                enhet = "0000",
            )
        every { sakService.finnSak(any(), any()) } returns sak

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SakEksistererIGjenny(sak))
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom det eksisterer en duplikat grunnlagsendringshendelse`() {
        val sak =
            Sak(
                ident = doedshendelseInternalBP.beroertFnr,
                sakType = doedshendelseInternalBP.sakType(),
                id = 1L,
                enhet = "0000",
            )
        val oppgaveIntern =
            mockk<OppgaveIntern> {
                every { id } returns UUID.randomUUID()
            }
        val grunnlagsendringshendelse =
            mockk<Grunnlagsendringshendelse> {
                every { id } returns UUID.randomUUID()
                every { gjelderPerson } returns doedshendelseInternalBP.avdoedFnr
                every { type } returns GrunnlagsendringsType.DOEDSFALL
            }
        every { sakService.finnSak(any(), any()) } returns sak
        every {
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns listOf(grunnlagsendringshendelse)
        every { oppgaveService.hentOppgaverForReferanse(any()) } returns listOf(oppgaveIntern)

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.SakEksistererIGjenny(sak),
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(
                    grunnlagsendringshendelseId = grunnlagsendringshendelse.id,
                    oppgaveId = oppgaveIntern.id,
                ),
            )
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt hvis alle sjekker er OK`() {
        kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP) shouldBe emptyList()
    }
}
