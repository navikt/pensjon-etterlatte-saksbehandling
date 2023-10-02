package no.nav.etterlatte.behandling.revurdering

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
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingServiceFeatureToggle
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
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakServiceFeatureToggle
import no.nav.etterlatte.token.Saksbehandler
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevurderingIntegrationTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        startServer()
        Kontekst.set(Context(mockk(), DatabaseContext(applicationContext.dataSource)))
    }

    @AfterEach
    fun afterEach() {
        resetDatabase()
    }

    @AfterAll
    fun shutdown() = afterAll()

    val fnr: String = "123"

    fun opprettSakMedFoerstegangsbehandling(
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
            }

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val featureToggleService = mockk<FeatureToggleService>()
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any(),
            )
        } returns true

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false,
            )
        } returns false

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
                RevurderingServiceImpl(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    featureToggleService,
                    applicationContext.behandlingDao,
                    applicationContext.hendelseDao,
                    applicationContext.grunnlagsendringshendelseDao,
                    applicationContext.kommerBarnetTilGodeService,
                    applicationContext.revurderingDao,
                    applicationContext.behandlingService,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = RevurderingAarsak.REGULERING,
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
            oppgaveService.hentOppgaverForSak(sak.id)
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
        val featureToggleService = mockk<FeatureToggleService>()
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any(),
            )
        } returns true

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false,
            )
        } returns false

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
            RevurderingServiceImpl(
                oppgaveService,
                grunnlagService,
                hendelser,
                featureToggleService,
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
                    aarsak = RevurderingAarsak.SOESKENJUSTERING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "saksbehandler", null),
                )
            }

        val revurderingInfo = RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.SOESKEN_DOER)
        val fikkLagret =
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering!!.id,
                    RevurderingMedBegrunnelse(revurderingInfo, "abc"),
                    "saksbehandler",
                )
            }
        assertTrue(fikkLagret)
        inTransaction {
            val lagretRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering!!.id) as Revurdering
            assertEquals(revurderingInfo, lagretRevurdering.revurderingInfo?.revurderingInfo)
            assertEquals("abc", lagretRevurdering.revurderingInfo?.begrunnelse)
        }

        // kan oppdatere
        val nyRevurderingInfo =
            RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.FORPLEID_ETTER_BARNEVERNSLOVEN)
        assertTrue(
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering!!.id,
                    RevurderingMedBegrunnelse(nyRevurderingInfo, null),
                    "saksbehandler",
                )
            },
        )
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

        // kan ikke oppdatere en ferdig revurdering
        assertFalse(
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering!!.id,
                    RevurderingMedBegrunnelse(revurderingInfo, null),
                    "saksbehandler",
                )
            },
        )
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
    fun `hvis featuretoggle er av saa opprettes ikke revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any(),
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false,
            )
        } returns false

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC(),
            )
        }

        assertNull(
            inTransaction {
                RevurderingServiceImpl(
                    applicationContext.oppgaveService,
                    applicationContext.grunnlagsService,
                    hendelser,
                    featureToggleService,
                    applicationContext.behandlingDao,
                    applicationContext.hendelseDao,
                    applicationContext.grunnlagsendringshendelseDao,
                    applicationContext.kommerBarnetTilGodeService,
                    applicationContext.revurderingDao,
                    applicationContext.behandlingService,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = RevurderingAarsak.REGULERING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "Jenny", null),
                )
            },
        )

        confirmVerified(hendelser)
    }

    @Test
    fun `Ny regulering skal håndtere hendelser om nytt grunnbeløp`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val featureToggleService = mockk<FeatureToggleService>()
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val saksbehandler = Saksbehandler("", "saksbehandler", null)

        every {
            featureToggleService.isEnabled(
                SakServiceFeatureToggle.FiltrerMedEnhetId,
                false,
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false,
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any(),
            )
        } returns true

        val revurderingService =
            RevurderingServiceImpl(
                oppgaveService,
                grunnlagService,
                hendelser,
                featureToggleService,
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
                revurderingService = revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = hendelser,
                featureToggleService = featureToggleService,
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
                    aarsak = RevurderingAarsak.REGULERING,
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
    fun `Skal få bad request hvis man mangler hendelsesid`() {
        val revurderingService =
            RevurderingServiceImpl(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.featureToggleService,
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
                revurderingService = applicationContext.revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = applicationContext.behandlingsHendelser,
                featureToggleService = applicationContext.featureToggleService,
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        val err =
            assertThrows<BadRequestException> {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = RevurderingAarsak.REGULERING,
                    paaGrunnAvHendelseId = "124124124",
                    begrunnelse = null,
                    saksbehandler = Saksbehandler("", "Jenny", null),
                )
            }
        assertTrue(
            err.message!!.startsWith("${RevurderingAarsak.REGULERING} har en ugyldig hendelse id for sakid"),
        )
    }

    @Test
    fun `Skal få bad request hvis man mangler forrige iverksattebehandling`() {
        val revurderingService =
            RevurderingServiceImpl(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.featureToggleService,
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
                revurderingService = applicationContext.revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = applicationContext.behandlingsHendelser,
                featureToggleService = applicationContext.featureToggleService,
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        assertThrows<RevurderingManglerIverksattBehandlingException> {
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = RevurderingAarsak.REGULERING,
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
            RevurderingServiceImpl(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.featureToggleService,
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
                revurderingService = applicationContext.revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = applicationContext.behandlingsHendelser,
                featureToggleService = applicationContext.featureToggleService,
            )

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val revurderingsAarsakIkkeStoettetIMiljoeBarn =
            mockk<RevurderingAarsak>().also {
                every { it.kanBrukesIMiljo() } returns false
                every { it.name } returns RevurderingAarsak.BARN.name
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
            RevurderingServiceImpl(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.featureToggleService,
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
                revurderingService = applicationContext.revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = applicationContext.behandlingsHendelser,
                featureToggleService = applicationContext.featureToggleService,
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
                    aarsak = RevurderingAarsak.REGULERING,
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
            RevurderingServiceImpl(
                applicationContext.oppgaveService,
                applicationContext.grunnlagsService,
                applicationContext.behandlingsHendelser,
                applicationContext.featureToggleService,
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
                revurderingService = applicationContext.revurderingService,
                gyldighetsproevingService = applicationContext.gyldighetsproevingService,
                sakService = applicationContext.sakService,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = applicationContext.behandlingsHendelser,
                featureToggleService = applicationContext.featureToggleService,
            )

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val nonNullBehandling = behandling!!
        val hentOppgaverForSak = applicationContext.oppgaveService.hentOppgaverForSak(sak.id)
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }

        val saksbehandler = "saksbehandler"
        applicationContext.oppgaveService.tildelSaksbehandler(oppgaveForFoerstegangsbehandling.id, saksbehandler)
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
        val attesteringsoppgave =
            inTransaction {
                applicationContext.oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
                    VedtakOppgaveDTO(
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
                    OppgaveType.UTLAND,
                    merknad = null,
                )
            }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(utlandsoppgaveref.id, "utlandssaksbehandler")
        }
        revurderingService.opprettManuellRevurderingWrapper(
            sakId = sak.id,
            aarsak = RevurderingAarsak.REGULERING,
            paaGrunnAvHendelseId = UUID.randomUUID().toString(),
            begrunnelse = null,
            saksbehandler = Saksbehandler("", "saksbehandler", null),
        )

        val oppgaverForSak = applicationContext.oppgaveService.hentOppgaverForSak(sak.id)
        val oppgaverUnderBehandling = oppgaverForSak.filter { it.status == Status.UNDER_BEHANDLING }
        oppgaverUnderBehandling.single { it.kilde == OppgaveKilde.GENERELL_BEHANDLING }
    }
}
