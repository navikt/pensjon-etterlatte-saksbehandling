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
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
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
        coEvery { pesysKlient.hentSaker(doedshendelseInternalBP.beroertFnr) } returns emptyList()
        coEvery { pesysKlient.hentSaker(doedshendelseInternalOMS.beroertFnr) } returns emptyList()

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
    fun `Eps har vært skilt mindre enn 5 år siden men ukjent giftedato, EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde`() {
        every { sakService.finnSak(any(), any()) } returns null

        val sivilstandSkilt =
            Sivilstand(
                Sivilstatus.SKILT,
                Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr),
                gyldigFraOgMed = LocalDate.now().minusYears(5L),
                null,
                "",
            )

        val sivilstandGift =
            Sivilstand(
                Sivilstatus.GIFT,
                Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr),
                gyldigFraOgMed = null,
                null,
                "",
            )

        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson()
                .copy(sivilstand = listOf(OpplysningDTO(sivilstandSkilt, "sivilstand"), OpplysningDTO(sivilstandGift, "sivilstand")))

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde(
                    doedshendelseInternalOMS.avdoedDoedsdato,
                    doedshendelseInternalOMS.avdoedFnr,
                ),
            )
    }

    @Test
    fun `Eps har vaert gift mindre enn 5 aar, uten barn, EpsVarighetUnderFemAarUtenBarn`() {
        every { sakService.finnSak(any(), any()) } returns null

        val sivilstandGift =
            Sivilstand(
                Sivilstatus.GIFT,
                Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr),
                gyldigFraOgMed = LocalDate.now().minusYears(3),
                null,
                "",
            )

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
                sivilstand = listOf(OpplysningDTO(sivilstandGift, "sivilstand")),
                familieRelasjon = null,
            )
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson()
                .copy(
                    foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr), null),
                    familieRelasjon = null,
                )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenBarn)
    }

    @Test
    fun `Eps har vaert gift mindre enn 5 aar, uten felles barn, EpsVarighetUnderFemAarUtenFellesBarn`() {
        every { sakService.finnSak(any(), any()) } returns null

        val sivilstandGift =
            Sivilstand(
                sivilstatus = Sivilstatus.GIFT,
                relatertVedSiviltilstand = Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr),
                gyldigFraOgMed = LocalDate.now().minusYears(3),
                bekreftelsesdato = null,
                kilde = "",
            )

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = doedshendelseInternalBP.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
                sivilstand = listOf(OpplysningDTO(sivilstandGift, "sivilstand")),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = emptyList(),
                            barn = listOf(LITE_BARN),
                        ),
                        null,
                    ),
            )
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternalOMS.beroertFnr), null),
                familieRelasjon = null,
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenFellesBarn)
    }

    @Test
    fun `Eps har vært gift i 15 år med avdød og skilt ila de siste 5 år, EpsHarVaertSkiltSiste5EllerGiftI15`() {
        every { sakService.finnSak(any(), any()) } returns null

        val sivilstandGift =
            Sivilstand(
                Sivilstatus.GIFT,
                Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr),
                gyldigFraOgMed = LocalDate.now().minusYears(16L),
                null,
                "",
            )

        val sivilstandSkiltSiste5Aar =
            Sivilstand(
                Sivilstatus.SKILT,
                Folkeregisteridentifikator.of(doedshendelseInternalOMS.avdoedFnr),
                gyldigFraOgMed = LocalDate.now().minusYears(2L),
                null,
                "",
            )

        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson()
                .copy(
                    sivilstand =
                        listOf(
                            OpplysningDTO(sivilstandGift, "sivilstand"),
                            OpplysningDTO(sivilstandSkiltSiste5Aar, "sivilstand"),
                        ),
                )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5OgGiftI15(
                    doedshendelseInternalOMS.avdoedDoedsdato,
                    doedshendelseInternalOMS.avdoedFnr,
                ),
            )
    }

    @Test
    fun `Eps er 67 år EpsKanHaAlderspensjon`() {
        every { sakService.finnSak(any(), any()) } returns null

        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson()
                .copy(
                    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(67L), "foedselsdato"),
                )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon)
    }

    @Test
    fun `Samboer gir SamboerSammeAdresseOgFellesBarn`() {
        every { sakService.finnSak(any(), any()) } returns null

        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson()
                .copy(
                    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(60L), "foedselsdato"),
                )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS.copy(relasjon = Relasjon.SAMBOER))

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamboerSammeAdresseOgFellesBarn)
    }

    @Test
    fun `Skal gi kontrollpunkt eps er død om eps er død, EpsHarDoedsdato`() {
        every { sakService.finnSak(any(), any()) } returns null

        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns
            mockPerson().copy(
                doedsdato =
                    OpplysningDTO(
                        LocalDate.now(),
                        "doedsdato",
                    ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsHarDoedsdato)
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

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalAvdoed)

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

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalAvdoed)

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

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalAvdoed)

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

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalAvdoed)

        kontrollpunkter shouldContainExactlyInAnyOrder
            listOf(
                DoedshendelseKontrollpunkt.AvdoedHarYtelse(sak),
                DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse(grunnlagsendringshendelse.id, oppgaveIntern.id),
            )
    }

    @Test
    fun `Skal returnere kontrollpunkt BarnHarBarnepensjon for relasjon barn og har BP`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
        every {
            behandlingService.hentSisteIverksatte(
                any(),
            )
        } returns foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT)

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)
        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnHarBarnepensjon(sak))
    }

    @Test
    fun `Skal returnere kontrollpunkt BarnHarUfoereTrygd for relasjon barn og har uføretrygd`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeYtelseIPesysEps)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede hadde utvandring`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
                doedsdato = OpplysningDTO(doedshendelseInternalBP.avdoedDoedsdato, null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamtidigDoedsfall)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom vi ikke finner den andre forelderen`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
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
    fun `Skal opprette kontrollpunkt EpsHarSakMedIverksattBehandlingIGjenny ved sak og med iverksatt behandling for EPS`() {
        val sak =
            Sak(
                ident = doedshendelseInternalOMS.beroertFnr,
                sakType = doedshendelseInternalOMS.sakTypeForEpsEllerBarn(),
                id = 1L,
                enhet = "0000",
            )
        every { sakService.finnSak(any(), any()) } returns sak
        every {
            behandlingService.hentSisteIverksatte(
                sak.id,
            )
        } returns foerstegangsbehandling(sakId = sak.id, status = BehandlingStatus.IVERKSATT)
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsHarSakMedIverksattBehandlingIGjenny(sak))
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt EpsHarSakMedIverksattBehandlingIGjenny dersom det eksisterer kun sak for EPS`() {
        val sak =
            Sak(
                ident = doedshendelseInternalOMS.beroertFnr,
                sakType = doedshendelseInternalOMS.sakTypeForEpsEllerBarn(),
                id = 1L,
                enhet = "0000",
            )
        every { sakService.finnSak(sak.ident, sak.sakType) } returns sak
        every { behandlingService.hentSisteIverksatte(sak.id) } returns null
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly emptyList()
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt EpsHarSakMedIverksattBehandlingIGjenny dersom det ikke eksisterer sak for EPS`() {
        every { sakService.finnSak(any(), any()) } returns null
        coEvery {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelseInternalOMS.beroertFnr,
                PersonRolle.GJENLEVENDE,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalOMS)

        kontrollpunkter shouldContainExactly emptyList()
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

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternalBP)

        kontrollpunkter shouldContainExactly
            listOf(
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
