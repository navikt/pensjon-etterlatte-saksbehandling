package no.nav.etterlatte.behandling.revurdering

import behandling.utland.LandMedDokumenter
import behandling.utland.MottattDokument
import io.ktor.server.plugins.BadRequestException
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
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
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevurderingServiceIntegrationTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()

        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        startServer()
        Kontekst.set(Context(user, DatabaseContext(applicationContext.dataSource)))
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
        val behandling =
            inTransaction {
                (behandlingFactory ?: applicationContext.behandlingFactory)
                    .opprettBehandling(
                        sak.id,
                        persongalleri(),
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                    )
            }?.behandling

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        }

        val revurdering =
            inTransaction {
                RevurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    applicationContext.behandlingDao,
                    applicationContext.hendelseDao,
                    applicationContext.grunnlagsendringshendelseDao,
                    applicationContext.kommerBarnetTilGodeService,
                    applicationContext.revurderingDao,
                    applicationContext.behandlingService,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        verify { grunnlagService.leggInnNyttGrunnlag(revurdering!!, any()) }
        coVerify { grunnlagService.hentPersongalleri(any()) }
        verify {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurdering?.id.toString(),
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
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            verify { hendelser.sendMeldingForHendelseMedDetaljertBehandling(any(), BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `kan lagre og oppdatere revurderinginfo på en revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        }
        val revurderingService =
            RevurderingService(
                oppgaveService,
                grunnlagService,
                hendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
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
                revurdering!!.id,
                RevurderingInfoMedBegrunnelse(revurderingInfo, "abc"),
                "saksbehandler",
            )
        }

        inTransaction {
            val lagretRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering!!.id) as Revurdering
            assertEquals(revurderingInfo, lagretRevurdering.revurderingInfo?.revurderingInfo)
            assertEquals("abc", lagretRevurdering.revurderingInfo?.begrunnelse)
        }

        // kan oppdatere
        val nyRevurderingInfo =
            RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.FORPLEID_ETTER_BARNEVERNSLOVEN)

        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurdering!!.id,
                RevurderingInfoMedBegrunnelse(nyRevurderingInfo, null),
                "saksbehandler",
            )
        }
        inTransaction {
            val oppdatert = applicationContext.behandlingDao.hentBehandling(revurdering!!.id) as Revurdering
            assertEquals(nyRevurderingInfo, oppdatert.revurderingInfo?.revurderingInfo)
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                revurdering!!.id,
                BehandlingStatus.IVERKSATT,
                LocalDateTime.now(),
            )
        }

        assertThrows<BehandlingKanIkkeEndres> {
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering!!.id,
                    RevurderingInfoMedBegrunnelse(revurderingInfo, null),
                    "saksbehandler",
                )
            }
        }

        inTransaction {
            val ferdigRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering!!.id) as Revurdering
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
    fun `Ny regulering skal håndtere hendelser om nytt grunnbeløp`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        val revurderingService =
            RevurderingService(
                oppgaveService,
                grunnlagService,
                hendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
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
                pdltjenesterKlient = mockk(),
            )

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
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
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            val grunnlaghendelse =
                applicationContext.grunnlagsendringshendelseDao.hentGrunnlagsendringshendelse(
                    hendelse.id,
                )
            assertEquals(revurdering.id, grunnlaghendelse?.behandlingId)
            coVerify { grunnlagService.hentPersongalleri(any()) }
            verify { grunnlagService.leggInnNyttGrunnlag(behandling as Behandling, any()) }
            verify { grunnlagService.leggInnNyttGrunnlag(revurdering, any()) }
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
            verify { oppgaveService.ferdigStillOppgaveUnderBehandling(any(), any()) }
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
    fun `kan opprette ny revurdering med årsak = SLUTTBEHANDLING_UTLAND og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
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
                RevurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    applicationContext.behandlingDao,
                    applicationContext.hendelseDao,
                    applicationContext.grunnlagsendringshendelseDao,
                    applicationContext.kommerBarnetTilGodeService,
                    applicationContext.revurderingDao,
                    applicationContext.behandlingService,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING_UTLAND,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        verify { grunnlagService.leggInnNyttGrunnlag(revurdering!!, any()) }
        coVerify { grunnlagService.hentPersongalleri(any()) }
        verify {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurdering?.id.toString(),
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
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
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
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        }
        assertThrows<RevurderingSluttbehandlingUtlandMaaHaEnBehandlingMedSkalSendeKravpakke> {
            inTransaction {
                RevurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    applicationContext.behandlingDao,
                    applicationContext.hendelseDao,
                    applicationContext.grunnlagsendringshendelseDao,
                    applicationContext.kommerBarnetTilGodeService,
                    applicationContext.revurderingDao,
                    applicationContext.behandlingService,
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
    fun `Skal kunne hente revurdering basert på sak og revurderingsårsak`() {
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )

        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )
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
            applicationContext.oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(
                    sakId = sak.id,
                    referanse = behandling!!.id.toString(),
                ),
                oppgaveType = OppgaveType.ATTESTERING,
                saksbehandler = Saksbehandler("", saksbehandler, null),
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
                revurderingen!!.id,
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
            inTransaction { revurderingService.hentRevurderingsinfoForSakMedAarsak(sak.id, Revurderingaarsak.REGULERING) }
        assertEquals(0, hentRevurderingsinfoForSakMedAarsak.size)

        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurderingto!!.id,
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
            inTransaction { revurderingService.hentRevurderingsinfoForSakMedAarsak(sak.id, Revurderingaarsak.SLUTTBEHANDLING_UTLAND) }
        assertEquals(1, sluttbehandlingermedinfo.size)
    }

    @Test
    fun `Skal få bad request hvis man mangler hendelsesid`() {
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )
        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )

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
    fun `Skal få bad request hvis man mangler forrige iverksattebehandling`() {
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )
        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        assertThrows<RevurderingManglerIverksattBehandlingException> {
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
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )
        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val revurderingsAarsakIkkeStoettetIMiljoeBarn =
            mockk<Revurderingaarsak>().also {
                every { it.kanBrukesIMiljo() } returns false
                every { it.name } returns Revurderingaarsak.BARN.name
            }
        assertThrows<RevurderingaarsakIkkeStoettetIMiljoeException> {
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
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )
        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, "sakbeahndler")
        }
        assertThrows<MaksEnBehandlingsOppgaveUnderbehandlingException> {
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
    fun `Kan opprette revurdering hvis en oppgave er under behandling med en annen oppgavekilde enn BEHANDLING`() {
        val revurderingService =
            RevurderingService(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                applicationContext.behandlingService,
            )
        val behandlingFactory =
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
                pdltjenesterKlient = mockk(),
            )

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
            applicationContext.oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                SakIdOgReferanse(
                    sakId = sak.id,
                    referanse = nonNullBehandling.id.toString(),
                ),
                oppgaveType = OppgaveType.ATTESTERING,
                saksbehandler = Saksbehandler("", saksbehandler, null),
                merknad = null,
            )
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                nonNullBehandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
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
}
