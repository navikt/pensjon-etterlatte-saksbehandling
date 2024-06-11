package no.nav.etterlatte.behandling.revurdering

import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.utland.LandMedDokumenter
import no.nav.etterlatte.behandling.utland.MottattDokument
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.GrunnForOmgjoering
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KlageOmgjoering
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevurderingServiceIntegrationTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns false
                every { harRolleAttestant() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        startServer()
        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun shutdown() = afterAll()

    val fnr: String = "123"

    private fun opprettSakMedFoerstegangsbehandling(
        fnr: String,
        behandlingFactory: BehandlingFactory? = null,
    ): Pair<Sak, Foerstegangsbehandling?> {
        val sak =
            inTransaction {
                applicationContext.sakDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }
        val factory = behandlingFactory ?: applicationContext.behandlingFactory
        val behandling =
            inTransaction {
                factory
                    .opprettBehandling(
                        sak.id,
                        persongalleri(),
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                        factory.hentDataForOpprettBehandling(sak.id),
                    )
            }?.also { it.sendMeldingForHendelse() }?.behandling

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val aktivitetspliktDao = spyk(applicationContext.aktivitetspliktDao)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling!!)
        }

        val revurdering =
            inTransaction {
                revurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    aktivitetspliktDao,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.ANNEN,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        verify { grunnlagService.leggInnNyttGrunnlag(revurdering, any()) }
        coVerify { grunnlagService.hentPersongalleri(any()) }
        verify {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurdering.id.toString(),
                sak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.REVURDERING,
                null,
            )
            oppgaveService.tildelSaksbehandler(any(), "saksbehandler")
            inTransaction {
                oppgaveService.hentOppgaverForSak(sak.id)
            }
        }
        verify { aktivitetspliktDao.kopierAktiviteter(behandling!!.id, revurdering.id) }
        inTransaction {
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering.id))
            verify { hendelser.sendMeldingForHendelseMedDetaljertBehandling(any(), BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `kan lagre og oppdatere revurderinginfo paa en revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling!!)
        }
        val revurderingService =
            revurderingService(
                oppgaveService,
                grunnlagService,
                hendelser,
            )
        val revurdering =
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SOESKENJUSTERING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        val revurderingInfo = RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.SOESKEN_DOER)
        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurdering.id,
                RevurderingInfoMedBegrunnelse(revurderingInfo, "abc"),
                "saksbehandler",
            )
        }

        inTransaction {
            val lagretRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            assertEquals(revurderingInfo, lagretRevurdering.revurderingInfo?.revurderingInfo)
            assertEquals("abc", lagretRevurdering.revurderingInfo?.begrunnelse)
        }

        // kan oppdatere
        val nyRevurderingInfo =
            RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.FORPLEID_ETTER_BARNEVERNSLOVEN)

        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurdering.id,
                RevurderingInfoMedBegrunnelse(nyRevurderingInfo, null),
                "saksbehandler",
            )
        }
        inTransaction {
            val oppdatert = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            assertEquals(nyRevurderingInfo, oppdatert.revurderingInfo?.revurderingInfo)
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                revurdering.id,
                BehandlingStatus.IVERKSATT,
                LocalDateTime.now(),
            )
        }

        assertThrows<BehandlingKanIkkeEndres> {
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering.id,
                    RevurderingInfoMedBegrunnelse(revurderingInfo, null),
                    "saksbehandler",
                )
            }
        }

        inTransaction {
            val ferdigRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            assertEquals(nyRevurderingInfo, ferdigRevurdering.revurderingInfo?.revurderingInfo)
            verify { hendelser.sendMeldingForHendelseMedDetaljertBehandling(any(), BehandlingHendelseType.OPPRETTET) }
            verify { grunnlagService.leggInnNyttGrunnlag(any(), any()) }
            verify { oppgaveService.hentOppgaverForSak(sak.id) }
            coVerify { grunnlagService.hentPersongalleri(any()) }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    revurdering.id.toString(),
                    sak.id,
                    OppgaveKilde.BEHANDLING,
                    OppgaveType.REVURDERING,
                    null,
                )
                oppgaveService.tildelSaksbehandler(any(), "saksbehandler")
            }
            confirmVerified(hendelser, grunnlagService, oppgaveService)
        }
    }

    @Test
    fun `Ny regulering skal haandtere hendelser om nytt grunnbeløp`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        val revurderingService =
            revurderingService(
                oppgaveService,
                grunnlagService,
                hendelser,
            )

        val behandlingFactory =
            BehandlingFactory(
                oppgaveService = oppgaveService,
                grunnlagService = grunnlagService,
                revurderingService = AutomatiskRevurderingService(revurderingService),
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = hendelser,
                migreringKlient = mockk(),
            )

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling!!)
        }
        val hendelse =
            inTransaction {
                applicationContext.grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                    Grunnlagsendringshendelse(
                        UUID.randomUUID(),
                        sak.id,
                        GrunnlagsendringsType.GRUNNBELOEP,
                        LocalDateTime.now(),
                        GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                        null,
                        Saksrolle.SOEKER,
                        sak.ident,
                        SamsvarMellomKildeOgGrunnlag.Grunnbeloep(false),
                    ),
                )
            }

        val oppgave =
            inTransaction {
                applicationContext.oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    referanse = hendelse.id.toString(),
                    sakId = sak.id,
                    oppgaveKilde = OppgaveKilde.HENDELSE,
                    oppgaveType = OppgaveType.VURDER_KONSEKVENS,
                    merknad = null,
                )
            }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveId = oppgave.id,
                saksbehandler = saksbehandler.ident,
            )
        }

        val revurdering =
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = hendelse.id.toString(),
                    begrunnelse = null,
                    saksbehandler = saksbehandler,
                )
            }

        inTransaction {
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering.id))
            val grunnlaghendelse =
                applicationContext.grunnlagsendringshendelseDao.hentGrunnlagsendringshendelse(
                    hendelse.id,
                )
            assertEquals(revurdering.id, grunnlaghendelse?.behandlingId)
            coVerify { grunnlagService.hentPersongalleri(any()) }
            verify { grunnlagService.leggInnNyttGrunnlag(behandling as Behandling, any()) }
            verify { grunnlagService.laasTilGrunnlagIBehandling(revurdering, behandling!!.id) }
            verify { oppgaveService.hentOppgaverForSak(sak.id) }
            verify { hendelser.sendMeldingForHendelseMedDetaljertBehandling(any(), BehandlingHendelseType.OPPRETTET) }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    behandling!!.id.toString(),
                    sak.id,
                    OppgaveKilde.BEHANDLING,
                    OppgaveType.FOERSTEGANGSBEHANDLING,
                    null,
                )
            }
            verify {
                oppgaveService.opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
                    behandling!!.id.toString(),
                    sak.id,
                )
                oppgaveService.tildelSaksbehandler(any(), saksbehandler.ident)
            }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    revurdering.id.toString(),
                    sak.id,
                    OppgaveKilde.BEHANDLING,
                    OppgaveType.REVURDERING,
                    null,
                )
            }
            verify { oppgaveService.ferdigStillOppgaveUnderBehandling(any(), any(), any()) }
            verify {
                hendelser.sendMeldingForHendelseMedDetaljertBehandling(
                    behandling!!.toStatistikkBehandling(
                        persongalleri(),
                    ),
                    BehandlingHendelseType.OPPRETTET,
                )
            }
            confirmVerified(hendelser, grunnlagService, oppgaveService)
        }
    }

    @Test
    fun `kan opprette ny revurdering med aarsak = SLUTTBEHANDLING_UTLAND og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling!!)
        }

        inTransaction {
            applicationContext.behandlingDao.lagreBoddEllerArbeidetUtlandet(
                behandling!!.id,
                BoddEllerArbeidetUtlandet(
                    true,
                    Grunnlagsopplysning.Saksbehandler("ident", Tidspunkt.now()),
                    "begrunnelse",
                    boddArbeidetIkkeEosEllerAvtaleland = true,
                    boddArbeidetEosNordiskKonvensjon = true,
                    boddArbeidetAvtaleland = true,
                    vurdereAvoededsTrygdeavtale = true,
                    skalSendeKravpakke = true,
                ),
            )
        }

        val revurdering =
            inTransaction {
                revurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING_UTLAND,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        verify { grunnlagService.leggInnNyttGrunnlag(revurdering, any()) }
        coVerify { grunnlagService.hentPersongalleri(any()) }
        verify {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurdering.id.toString(),
                sak.id,
                OppgaveKilde.BEHANDLING,
                OppgaveType.REVURDERING,
                null,
            )
            oppgaveService.tildelSaksbehandler(any(), "saksbehandler")
            inTransaction {
                oppgaveService.hentOppgaverForSak(sak.id)
            }
        }
        inTransaction {
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering.id))
            verify { hendelser.sendMeldingForHendelseMedDetaljertBehandling(any(), BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `Kan ikke opprette revurdering SLUTTBEHANDLING_UTLAND hvis man mangler en tidligere behandling med kravpakke`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling!!)
        }
        assertThrows<RevurderingSluttbehandlingUtlandMaaHaEnBehandlingMedSkalSendeKravpakke> {
            inTransaction {
                revurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING_UTLAND,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }
        }
    }

    @Test
    fun `Skal kunne hente revurdering basert paa sak og revurderingsaarsak`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }

        val saksbehandler = "saksbehandler"
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, saksbehandler)
        }
        inTransaction {
            applicationContext.behandlingDao.lagreBoddEllerArbeidetUtlandet(
                behandling!!.id,
                BoddEllerArbeidetUtlandet(
                    boddEllerArbeidetUtlandet = true,
                    skalSendeKravpakke = true,
                    begrunnelse = "enbegrunnelse",
                    kilde = Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                ),
            )
        }
        inTransaction {
            applicationContext.oppgaveService.tilAttestering(
                referanse = behandling!!.id.toString(),
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
            ferdigstillOppgaver(behandling, saksbehandler)
        }
        val revurderingen =
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    begrunnelse = null,
                    paaGrunnAvHendelseId = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }
        inTransaction {
            applicationContext.behandlingService.avbrytBehandling(
                revurderingen.id,
                BrukerTokenInfo.of("acc", "saksbehandler", oid = null, sub = "sub", claims = null),
            )
        }

        val revurderingto =
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING_UTLAND,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        val hentRevurderingsinfoForSakMedAarsak =
            inTransaction {
                revurderingService.hentRevurderingsinfoForSakMedAarsak(
                    sak.id,
                    Revurderingaarsak.REGULERING,
                )
            }
        assertEquals(0, hentRevurderingsinfoForSakMedAarsak.size)

        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurderingto.id,
                RevurderingInfoMedBegrunnelse(
                    RevurderingInfo.SluttbehandlingUtland(
                        listOf(
                            LandMedDokumenter(
                                landIsoKode = "AFG",
                                dokumenter =
                                    listOf(
                                        MottattDokument(
                                            dokumenttype = "P2000",
                                            dato = LocalDate.now(),
                                            kommentar = "kom",
                                        ),
                                    ),
                            ),
                        ),
                    ),
                    begrunnelse = "nei",
                ),
                navIdent = "ident",
            )
        }

        val sluttbehandlingermedinfo =
            inTransaction {
                revurderingService.hentRevurderingsinfoForSakMedAarsak(
                    sak.id,
                    Revurderingaarsak.SLUTTBEHANDLING_UTLAND,
                )
            }
        assertEquals(1, sluttbehandlingermedinfo.size)
    }

    @Test
    fun `Skal faa bad request hvis man mangler hendelsesid`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        val err =
            assertThrows<BadRequestException> {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = "124124124",
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "Jenny", null),
                )
            }
        assertTrue(
            err.message!!.startsWith("${Revurderingaarsak.REGULERING} har en ugyldig hendelse id for sakid"),
        )
    }

    @Test
    fun `Skal faa bad request hvis man mangler forrige iverksattebehandling`() {
        val revurderingService = revurderingService()

        val sak =
            inTransaction {
                applicationContext.sakDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }

        assertThrows<RevurderingManglerIverksattBehandling> {
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = UUID.randomUUID().toString(),
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }
        }
    }

    @Test
    fun `Kaster egen exception hvis revurderingaarsak ikke er stoettet for miljoe`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val revurderingsAarsakIkkeStoettetIMiljoeBarn =
            mockk<Revurderingaarsak>().also {
                every { it.kanBrukesIMiljo() } returns false
                every { it.name } returns Revurderingaarsak.BARN.name
            }
        assertThrows<RevurderingaarsakIkkeStoettet> {
            revurderingService.opprettManuellRevurderingWrapper(
                sakId = sak.id,
                aarsak = revurderingsAarsakIkkeStoettetIMiljoeBarn,
                paaGrunnAvHendelseId = UUID.randomUUID().toString(),
                begrunnelse = null,
                saksbehandler = Saksbehandler("", "saksbehandler", null),
            )
        }
    }

    @Test
    fun `Kan ikke opprette ny manuell revurdering hvis det finnes en oppgave under behandling for sak med oppgavekilde behandling`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, "sakbeahndler")
        }
        assertThrows<MaksEnAktivOppgavePaaBehandling> {
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = UUID.randomUUID().toString(),
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }
        }
    }

    @Test
    fun `Kan opprette revurdering for omgjoering av klage`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }

        val saksbehandlerIdent = "saksbehandler"
        val saksbehandler = Saksbehandler("", saksbehandlerIdent, null)
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveForFoerstegangsbehandling.id,
                saksbehandlerIdent,
            )
        }
        inTransaction {
            applicationContext.oppgaveService.tilAttestering(
                referanse = behandling!!.id.toString(),
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        }
        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        }
        val klage =
            inTransaction {
                applicationContext.klageService.opprettKlage(
                    sakId = sak.id,
                    innkommendeKlage =
                        InnkommendeKlage(
                            mottattDato = LocalDate.of(2024, 1, 1),
                            journalpostId = "En id for journalpost",
                            innsender = SOEKER_FOEDSELSNUMMER.value,
                        ),
                    saksbehandler = saksbehandler,
                )
            }
        val klageOppgave =
            inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }.filter { it.referanse == klage.id.toString() }[0]
        inTransaction { applicationContext.oppgaveService.tildelSaksbehandler(klageOppgave.id, saksbehandlerIdent) }
        inTransaction {
            applicationContext.klageService.lagreFormkravIKlage(
                klageId = klage.id,
                formkrav =
                    Formkrav(
                        vedtaketKlagenGjelder =
                            VedtaketKlagenGjelder(
                                behandlingId = behandling!!.id.toString(),
                                datoAttestert =
                                    ZonedDateTime.of(
                                        LocalDate.of(2023, 10, 10),
                                        LocalTime.MIDNIGHT,
                                        ZoneId.systemDefault(),
                                    ),
                                vedtakType = VedtakType.INNVILGELSE,
                                id = "123",
                            ),
                        erKlagerPartISaken = JaNei.JA,
                        erKlagenSignert = JaNei.JA,
                        gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                        erKlagenFramsattInnenFrist = JaNei.JA,
                        erFormkraveneOppfylt = JaNei.JA,
                        begrunnelse = "Jeg er enig",
                    ),
                saksbehandler = saksbehandler,
            )
        }

        val oppgaveForOmgjoering =
            inTransaction {
                applicationContext.klageService.lagreInitieltUtfallMedBegrunnelseAvKlage(
                    klageId = klage.id,
                    utfall = InitieltUtfallMedBegrunnelseDto(KlageUtfall.OMGJOERING, "Vi må endre vedtak"),
                    saksbehandler = saksbehandler,
                )
                applicationContext.klageService.lagreUtfallAvKlage(
                    klageId = klage.id,
                    utfall =
                        KlageUtfallUtenBrev.Omgjoering(
                            omgjoering =
                                KlageOmgjoering(
                                    grunnForOmgjoering = GrunnForOmgjoering.FEIL_REGELVERKSFORSTAAELSE,
                                    begrunnelse = "Vi må endre vedtak",
                                ),
                        ),
                    saksbehandler = saksbehandler,
                )
                applicationContext.klageService.ferdigstillKlage(klageId = klage.id, saksbehandler = saksbehandler)
                applicationContext.oppgaveService
                    .hentOppgaverForSak(sak.id)
                    .first { it.type == OppgaveType.OMGJOERING }
            }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveId = oppgaveForOmgjoering.id,
                saksbehandlerIdent,
            )
        }
        val revurderingOmgjoering =
            inTransaction { revurderingService.opprettOmgjoeringKlage(sak.id, oppgaveForOmgjoering.id, saksbehandler) }

        assertEquals(revurderingOmgjoering.relatertBehandlingId, klage.id.toString())
        assertEquals(revurderingOmgjoering.revurderingsaarsak, Revurderingaarsak.OMGJOERING_ETTER_KLAGE)

        inTransaction { applicationContext.behandlingService.avbrytBehandling(revurderingOmgjoering.id, saksbehandler) }

        val oppgaverISakEtterAlt =
            inTransaction {
                applicationContext.oppgaveService.hentOppgaverForSak(sak.id)
            }
        val antallOmgjoeringsoppgaver = oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING }
        val antallAapneOmgjoeringsoppgaver =
            oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING && !it.status.erAvsluttet() }
        val antallOmgjoeringsoppgaverSomPekerPaaKlage =
            oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING && it.referanse == klage.id.toString() }
        assertEquals(2, antallOmgjoeringsoppgaver)
        assertEquals(1, antallAapneOmgjoeringsoppgaver)
        assertEquals(2, antallOmgjoeringsoppgaverSomPekerPaaKlage)
    }

    @Test
    fun `Kan opprette revurdering hvis en oppgave er under behandling med en annen oppgavekilde enn BEHANDLING`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val nonNullBehandling = behandling!!
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }

        val saksbehandler = "saksbehandler"
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, saksbehandler)
        }
        inTransaction {
            applicationContext.behandlingDao.lagreBoddEllerArbeidetUtlandet(
                nonNullBehandling.id,
                BoddEllerArbeidetUtlandet(
                    boddEllerArbeidetUtlandet = true,
                    skalSendeKravpakke = true,
                    begrunnelse = "enbegrunnelse",
                    kilde = Grunnlagsopplysning.Saksbehandler.create("saksbehandler"),
                ),
            )
        }
        inTransaction {
            applicationContext.oppgaveService.tilAttestering(
                referanse = nonNullBehandling.id.toString(),
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                nonNullBehandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
            ferdigstillOppgaver(behandling, saksbehandler)
        }

        val utlandsoppgaveref =
            inTransaction {
                applicationContext.oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    nonNullBehandling.id.toString(),
                    sak.id,
                    OppgaveKilde.GENERELL_BEHANDLING,
                    OppgaveType.KRAVPAKKE_UTLAND,
                    merknad = null,
                )
            }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(utlandsoppgaveref.id, "utlandssaksbehandler")
        }
        inTransaction {
            revurderingService.opprettManuellRevurderingWrapper(
                sakId = sak.id,
                aarsak = Revurderingaarsak.REGULERING,
                paaGrunnAvHendelseId = UUID.randomUUID().toString(),
                begrunnelse = null,
                saksbehandler = Saksbehandler("", "saksbehandler", null),
            )
        }

        val oppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaverUnderBehandling = oppgaverForSak.filter { it.status == Status.UNDER_BEHANDLING }
        oppgaverUnderBehandling.single { it.kilde == OppgaveKilde.GENERELL_BEHANDLING }
    }

    @Test
    fun `Kan opprette revurdering paa bakgrunn av en revurderingsoppgave, skal oppdatere triggende oppgave til aa gjelde behandling`() {
        val oppgaveService = applicationContext.oppgaveService
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        // Forutsetninger - sak med iverksatt behandling
        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        inTransaction {
            iverksett(behandling!!)
        }

        // Opprett en revurderingsoppgave som gjelder en hendelse
        val oppgaveHendelse =
            inTransaction {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    referanse = "",
                    sakId = sak.id,
                    oppgaveKilde = OppgaveKilde.HENDELSE,
                    oppgaveType = OppgaveType.REVURDERING,
                    merknad = "Aldersovergang v/20 år",
                    frist = Tidspunkt.now(),
                )
            }

        // Tildel oppgaven
        inTransaction {
            oppgaveService.tildelSaksbehandler(oppgaveHendelse.id, "Z123456")
        }

        // Opprett revurdering, triggende oppgave konverteres til aa gjelde behandlingen
        val revurdering =
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.ALDERSOVERGANG,
                    paaGrunnAvHendelseId = null,
                    paaGrunnAvOppgaveId = oppgaveHendelse.id.toString(),
                    begrunnelse = oppgaveHendelse.merknad,
                    saksbehandler = Saksbehandler("", "Z123456", null),
                )
            }

        with(
            inTransaction {
                oppgaveService.hentOppgave(oppgaveHendelse.id)
            },
        ) {
            status shouldBe Status.UNDER_BEHANDLING
            referanse shouldBe revurdering.id.toString()
        }
    }

    private fun iverksett(behandling: Behandling) {
        applicationContext.behandlingDao.lagreStatus(
            behandling.id,
            BehandlingStatus.IVERKSATT,
            Tidspunkt.now().toLocalDatetimeUTC(),
        )
        ferdigstillOppgaver(behandling)
    }

    private fun ferdigstillOppgaver(
        behandling: Behandling,
        saksbehandlerId: String = "sbh",
    ) {
        with(applicationContext.oppgaveService) {
            this.hentOppgaverForReferanse(behandling.id.toString()).forEach {
                if (it.manglerSaksbehandler()) {
                    this.byttSaksbehandler(it.id, saksbehandlerId) // for aa kunne ferdgistille oppgaven
                }
                this.ferdigstillOppgave(
                    id = it.id,
                    saksbehandler = BrukerTokenInfo.of("acc", saksbehandlerId, oid = null, sub = "sub", claims = null),
                )
            }
        }
    }

    private fun revurderingService(
        oppgaveService: OppgaveService = applicationContext.oppgaveService,
        grunnlagService: GrunnlagService = applicationContext.grunnlagsService,
        behandlingsHendelser: BehandlingsHendelserKafkaProducerImpl = applicationContext.behandlingsHendelser,
        aktivitetspliktDao: AktivitetspliktDao = applicationContext.aktivitetspliktDao,
    ) = RevurderingService(
        oppgaveService,
        grunnlagService,
        behandlingsHendelser,
        applicationContext.behandlingDao,
        applicationContext.hendelseDao,
        applicationContext.grunnlagsendringshendelseDao,
        applicationContext.kommerBarnetTilGodeService,
        applicationContext.revurderingDao,
        applicationContext.klageService,
        applicationContext.behandlingService,
        aktivitetspliktDao,
    )

    private fun behandlingFactory() =
        BehandlingFactory(
            oppgaveService = applicationContext.oppgaveService,
            grunnlagService = applicationContext.grunnlagsService,
            revurderingService = applicationContext.automatiskRevurderingService,
            gyldighetsproevingService = applicationContext.gyldighetsproevingService,
            sakService = applicationContext.sakService,
            behandlingDao = applicationContext.behandlingDao,
            hendelseDao = applicationContext.hendelseDao,
            behandlingHendelser = applicationContext.behandlingsHendelser,
            migreringKlient = mockk(),
        )
}
