package no.nav.etterlatte.behandling.revurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingNotFoundException
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.AutomatiskRevurdering
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManuellRevurdering
import no.nav.etterlatte.behandling.domain.OpphoerFraTidligereBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.utland.LandMedDokumenter
import no.nav.etterlatte.behandling.utland.MottattDokument
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.defaultPersongalleriGydligeFnr
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.soeker
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
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
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManuellRevurderingServiceTest : BehandlingIntegrationTest() {
    val user = mockSaksbehandler("test")
    private val norg2Klient = mockk<Norg2Klient>()

    @BeforeAll
    fun start() {
        val standardEnhet = ArbeidsFordelingEnhet(Enheter.defaultEnhet.navn, Enheter.defaultEnhet.enhetNr)

        coEvery { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(any()) } returns listOf(standardEnhet)

        startServer(
            norg2Klient = norg2Klient,
        )
        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun shutdown() = afterAll()

    val fnr: String = soeker

    private fun opprettSakMedFoerstegangsbehandling(
        fnr: String,
        behandlingFactory: BehandlingFactory? = null,
    ): Pair<Sak, Foerstegangsbehandling> {
        val sak =
            inTransaction {
                applicationContext.sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }

        val sakmedenhet = inTransaction { applicationContext.sakLesDao.hentSak(sak.id) }
        println(sakmedenhet)
        /* Sakenhet fra brukerService blir child^2 #2123 aner ikke hvordan det skjer da den er mocka til noe annet men aldri brukes
        Men det fungerer i egenansattroutetest?
        jævlig rart men prøvd med
                    coEvery {
                norg2Klient.hentArbeidsfordelingForOmraadeOgTema(any())
            } returns listOf(ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr))
         */

        val factory = behandlingFactory ?: applicationContext.behandlingFactory
        val behandling =
            inTransaction {
                factory
                    .opprettBehandling(
                        sak.id,
                        defaultPersongalleriGydligeFnr,
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                        factory.hentDataForOpprettBehandling(sak.id),
                        BehandlingOpprinnelse.UKJENT,
                    )
            }.also { it.sendMeldingForHendelse() }.behandling

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val aktivitetspliktDao = spyk(applicationContext.aktivitetspliktDao)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling)
        }

        val revurdering =
            inTransaction {
                opprettManuellRevurdering(oppgaveService, grunnlagService, hendelser, aktivitetspliktDao, sak)
            }

        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify {
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
            grunnlagService.hentPersongalleri(behandling.id)
            grunnlagService.hentPersongalleri(sak.id)
            grunnlagService.lagreNyePersonopplysninger(sak.id, revurdering.id, any(), any())
            grunnlagService.lagreNyeSaksopplysninger(sak.id, revurdering.id, any())
            oppgaveService.opprettOppgave(
                referanse = revurdering.id.toString(),
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.REVURDERING,
                merknad = revurdering.revurderingsaarsak?.lesbar(),
                gruppeId = defaultPersongalleriGydligeFnr.avdoed.first(),
            )
            oppgaveService.tildelSaksbehandler(any(), "saksbehandler")
            oppgaveService.hentOppgaverForSak(sak.id)
            oppgaveService.hentOppgave(any())
            aktivitetspliktDao.kopierAktiviteter(behandling.id, revurdering.id)
        }
        inTransaction {
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering.id))
            verify { hendelser.sendMeldingForHendelseStatistikk(any(), BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `kan opprette revurdering med opphoer fra en annen behandling enn den forrige`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagService)
        val oppgaveService = spyk(applicationContext.oppgaveService)
        val aktivitetspliktDao = spyk(applicationContext.aktivitetspliktDao)

        val (sak, foerstegangsbehandling) =
            opprettSakMedFoerstegangsbehandling(fnr)
                .let { (sak, behandling) ->
                    sak to settVirkningstidspunkt(behandling.id, YearMonth.now().withMonth(1))
                }

        assertNotNull(foerstegangsbehandling)

        inTransaction {
            iverksett(foerstegangsbehandling)
        }

        val opphoerFraOgMed = foerstegangsbehandling.virkningstidspunkt!!.dato.plusMonths(7)

        val revurderingMedOpphoer =
            inTransaction {
                val revurdering =
                    opprettManuellRevurdering(
                        oppgaveService = oppgaveService,
                        grunnlagService = grunnlagService,
                        hendelser = hendelser,
                        aktivitetspliktDao = aktivitetspliktDao,
                        sak = sak,
                    )

                applicationContext.behandlingService.lagreOpphoerFom(revurdering.id, opphoerFraOgMed)
                runBlocking {
                    applicationContext.behandlingService.oppdaterViderefoertOpphoer(
                        revurdering.id,
                        ViderefoertOpphoer(
                            JaNei.JA,
                            revurdering.id,
                            opphoerFraOgMed,
                            VilkaarType.BP_ALDER_BARN,
                            "begr",
                            kilde,
                            true,
                        ),
                        simpleSaksbehandler(),
                    )
                }
                iverksett(revurdering, "saksbehandler")

                applicationContext.behandlingService.hentBehandling(revurdering.id)!!
            }

        inTransaction {
            val revurdering2 =
                opprettManuellRevurdering(
                    oppgaveService = oppgaveService,
                    grunnlagService = grunnlagService,
                    hendelser = hendelser,
                    aktivitetspliktDao = aktivitetspliktDao,
                    sak = sak,
                    opphoer =
                        OpphoerFraTidligereBehandling(
                            opphoerFraOgMed = opphoerFraOgMed,
                            behandlingId = revurderingMedOpphoer.id,
                        ),
                    forrigeBehandlingId = foerstegangsbehandling.id,
                ).let {
                    applicationContext.behandlingService.hentBehandling(it.id)!!
                }

            val eksisterendeViderefoertOpphoer = applicationContext.behandlingDao.hentViderefoertOpphoer(revurdering2.id)!!
            val nyViderefoertOpphoer = applicationContext.behandlingDao.hentViderefoertOpphoer(revurdering2.id)!!

            assertEquals(opphoerFraOgMed, revurdering2.opphoerFraOgMed)
            assertEquals(opphoerFraOgMed, nyViderefoertOpphoer.dato)
            nyViderefoertOpphoer.shouldBeEqualToIgnoringFields(
                eksisterendeViderefoertOpphoer,
                ViderefoertOpphoer::behandlingId,
            )
        }
    }

    private fun opprettManuellRevurdering(
        oppgaveService: OppgaveService,
        grunnlagService: GrunnlagService,
        hendelser: BehandlingsHendelserKafkaProducerImpl,
        aktivitetspliktDao: AktivitetspliktDao,
        sak: Sak,
        opphoer: OpphoerFraTidligereBehandling? = null,
        forrigeBehandlingId: UUID? = null,
    ): Revurdering =
        manuellRevurderingService(
            revurderingService =
                revurderingService(
                    oppgaveService,
                    grunnlagService,
                    hendelser,
                    aktivitetspliktDao,
                ),
            oppgaveService,
            grunnlagService,
        ).opprettManuellRevurderingWrapper(
            sakId = sak.id,
            aarsak = Revurderingaarsak.ANNEN,
            paaGrunnAvHendelseId = null,
            begrunnelse = null,
            saksbehandler = simpleSaksbehandler(),
            opphoerFraTidligereBehandling = opphoer,
            forrigeBehandlingId = forrigeBehandlingId,
        )

    @Test
    fun `kan lagre og oppdatere revurderinginfo paa en revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling)
        }
        val revurderingService =
            revurderingService(
                oppgaveService,
                grunnlagService,
                hendelser,
            )
        val manuellRevurderingService =
            manuellRevurderingService(
                revurderingService,
                oppgaveService,
                grunnlagService,
            )
        val revurdering =
            inTransaction {
                manuellRevurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SOESKENJUSTERING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = simpleSaksbehandler(),
                )
            }

        val revurderingInfo = RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.SOESKEN_DOER)
        val saksbehandler = "saksbehandler"
        inTransaction {
            revurderingService.lagreRevurderingInfo(
                revurdering.id,
                RevurderingInfoMedBegrunnelse(revurderingInfo, "abc"),
                saksbehandler,
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
                saksbehandler,
            )
        }
        inTransaction {
            val oppdatert = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            assertEquals(nyRevurderingInfo, oppdatert.revurderingInfo?.revurderingInfo)
        }

        inTransaction {
            iverksett(revurdering, saksbehandler)
        }

        assertThrows<BehandlingKanIkkeEndres> {
            inTransaction {
                revurderingService.lagreRevurderingInfo(
                    revurdering.id,
                    RevurderingInfoMedBegrunnelse(revurderingInfo, null),
                    saksbehandler,
                )
            }
        }

        inTransaction {
            val ferdigRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            assertEquals(nyRevurderingInfo, ferdigRevurdering.revurderingInfo?.revurderingInfo)
            verify {
                hendelser.sendMeldingForHendelseStatistikk(any(), BehandlingHendelseType.OPPRETTET)
                oppgaveService.hentOppgaverForSak(sak.id)
                oppgaveService.hentOppgave(any())
                oppgaveService.opprettOppgave(
                    referanse = revurdering.id.toString(),
                    sakId = sak.id,
                    kilde = OppgaveKilde.BEHANDLING,
                    type = OppgaveType.REVURDERING,
                    merknad = revurdering.revurderingsaarsak?.lesbar(),
                    gruppeId = defaultPersongalleriGydligeFnr.avdoed.first(),
                )
                oppgaveService.tildelSaksbehandler(any(), saksbehandler)
                grunnlagService.hentPersongalleri(behandling.id)
                grunnlagService.hentPersongalleri(sak.id)
                grunnlagService.lagreNyePersonopplysninger(sak.id, revurdering.id, any(), any())
                grunnlagService.lagreNyeSaksopplysninger(sak.id, revurdering.id, any())
                grunnlagService.hentOpplysningsgrunnlagForSak(any())
            }
            coVerify {
                grunnlagService.opprettGrunnlag(any(), any())
            }
            confirmVerified(hendelser, grunnlagService, oppgaveService)
        }
    }

    @Test
    fun `kan opprette ny revurdering med aarsak = SLUTTBEHANDLING og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val grunnlagService = spyk(applicationContext.grunnlagService)
        val oppgaveService = spyk(applicationContext.oppgaveService)

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            iverksett(behandling)
        }

        inTransaction {
            applicationContext.behandlingDao.lagreBoddEllerArbeidetUtlandet(
                behandling.id,
                BoddEllerArbeidetUtlandet(
                    true,
                    Grunnlagsopplysning.Saksbehandler("ident", Tidspunkt.now()),
                    "begrunnelse",
                    boddArbeidetIkkeEosEllerAvtaleland = true,
                    boddArbeidetEosNordiskKonvensjon = true,
                    boddArbeidetAvtaleland = true,
                    vurdereAvdoedesTrygdeavtale = true,
                    skalSendeKravpakke = true,
                ),
            )
        }

        val revurdering =
            inTransaction {
                manuellRevurderingService(
                    revurderingService(
                        oppgaveService,
                        grunnlagService,
                        hendelser,
                    ),
                    oppgaveService,
                    grunnlagService,
                ).opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = simpleSaksbehandler(),
                )
            }

        coVerify {
            grunnlagService.opprettGrunnlag(any(), any())
        }
        verify {
            grunnlagService.hentOpplysningsgrunnlagForSak(any())
            grunnlagService.hentPersongalleri(behandling.id)
            grunnlagService.hentPersongalleri(sak.id)
            grunnlagService.lagreNyePersonopplysninger(sak.id, revurdering.id, any(), any())
            grunnlagService.lagreNyeSaksopplysninger(sak.id, revurdering.id, any())
            oppgaveService.opprettOppgave(
                referanse = revurdering.id.toString(),
                sakId = sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.REVURDERING,
                merknad = revurdering.revurderingsaarsak?.lesbar(),
                gruppeId = defaultPersongalleriGydligeFnr.avdoed.first(),
            )
            oppgaveService.tildelSaksbehandler(any(), "saksbehandler")
            oppgaveService.hentOppgaverForSak(sak.id)
            oppgaveService.hentOppgave(any())
        }
        inTransaction {
            assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering.id))
            verify { hendelser.sendMeldingForHendelseStatistikk(any(), BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `Skal kunne hente revurdering basert paa sak og revurderingsaarsak`() {
        val revurderingService = revurderingService()
        val manuellRevurderingService = manuellRevurderingService(revurderingService)
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
                behandling.id,
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
                referanse = behandling.id.toString(),
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        }

        inTransaction {
            iverksett(behandling)
            ferdigstillOppgaver(behandling, saksbehandler)
        }
        val revurderingen =
            inTransaction {
                manuellRevurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    begrunnelse = null,
                    paaGrunnAvHendelseId = null,
                    saksbehandler = simpleSaksbehandler(),
                )
            }
        inTransaction {
            applicationContext.behandlingService.avbrytBehandling(
                revurderingen.id,
                simpleSaksbehandler(),
            )
        }

        val revurderingto =
            inTransaction {
                manuellRevurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.SLUTTBEHANDLING,
                    paaGrunnAvHendelseId = null,
                    begrunnelse = null,
                    saksbehandler = simpleSaksbehandler(),
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
                    Revurderingaarsak.SLUTTBEHANDLING,
                )
            }
        assertEquals(1, sluttbehandlingermedinfo.size)
    }

    @Test
    fun `Skal faa bad request hvis man mangler hendelsesid`() {
        val revurderingService = revurderingService()
        val manuellRevurderingService = manuellRevurderingService(revurderingService)
        val behandlingFactory = behandlingFactory()

        val (sak, _) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

        val err =
            assertThrows<UgyldigForespoerselException> {
                manuellRevurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = "124124124",
                    begrunnelse = null,
                    saksbehandler = simpleSaksbehandler(),
                )
            }
        assertTrue(
            err.message!!.startsWith("${Revurderingaarsak.REGULERING} har en ugyldig hendelse id for sakid"),
        )
    }

    @Test
    fun `Skal faa bad request hvis man mangler forrige iverksattebehandling`() {
        val revurderingService = manuellRevurderingService(revurderingService())

        val sak =
            inTransaction {
                applicationContext.sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }

        assertThrows<RevurderingManglerIverksattBehandling> {
            inTransaction {
                revurderingService.opprettManuellRevurderingWrapper(
                    sakId = sak.id,
                    aarsak = Revurderingaarsak.REGULERING,
                    paaGrunnAvHendelseId = UUID.randomUUID().toString(),
                    begrunnelse = null,
                    saksbehandler = simpleSaksbehandler(),
                )
            }
        }
    }

    @Test
    fun `Kaster egen exception hvis revurderingaarsak ikke er stoettet for miljoe`() {
        val revurderingService = manuellRevurderingService(revurderingService())
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
                saksbehandler = simpleSaksbehandler(),
            )
        }
    }

    @Test
    fun `Kan ikke opprette ny manuell revurdering hvis det finnes en oppgave under behandling for sak med oppgavekilde behandling`() {
        val revurderingService = manuellRevurderingService(revurderingService())
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
                    saksbehandler = simpleSaksbehandler(),
                )
            }
        }
    }

    @Test
    fun `Kan opprette revurdering hvis en oppgave er under behandling med en annen oppgavekilde enn BEHANDLING`() {
        val revurderingService = manuellRevurderingService(revurderingService())
        val behandlingFactory = behandlingFactory()

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val nonNullBehandling = behandling
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
            applicationContext.behandlingDao.lagreStatus(nonNullBehandling.copy(status = BehandlingStatus.IVERKSATT))
            ferdigstillOppgaver(behandling, saksbehandler)
        }

        val utlandsoppgaveref =
            inTransaction {
                applicationContext.oppgaveService.opprettOppgave(
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
                saksbehandler = simpleSaksbehandler(),
            )
        }

        val oppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaverUnderBehandling = oppgaverForSak.filter { it.status == Status.UNDER_BEHANDLING }
        oppgaverUnderBehandling.single { it.kilde == OppgaveKilde.GENERELL_BEHANDLING }
    }

    @Test
    fun `Kan opprette revurdering paa bakgrunn av en revurderingsoppgave, skal oppdatere triggende oppgave til aa gjelde behandling`() {
        val oppgaveService = applicationContext.oppgaveService
        val revurderingService = manuellRevurderingService(revurderingService())
        val behandlingFactory = behandlingFactory()

        // Forutsetninger - sak med iverksatt behandling
        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        inTransaction {
            iverksett(behandling)
        }

        // Opprett en revurderingsoppgave som gjelder en hendelse
        val oppgaveHendelse =
            inTransaction {
                oppgaveService.opprettOppgave(
                    referanse = "",
                    sakId = sak.id,
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.REVURDERING,
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
                    saksbehandler = simpleSaksbehandler(),
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

    private fun iverksett(
        behandling: Behandling,
        saksbehandlerId: String = "sbh",
    ) {
        val iverksatt =
            when (behandling) {
                is Foerstegangsbehandling -> behandling.copy(status = BehandlingStatus.IVERKSATT)
                is ManuellRevurdering -> behandling.copy(status = BehandlingStatus.IVERKSATT)
                is AutomatiskRevurdering -> behandling.copy(status = BehandlingStatus.IVERKSATT)
            }
        applicationContext.behandlingDao.lagreStatus(iverksatt)
        ferdigstillOppgaver(behandling, saksbehandlerId)
    }

    private fun settVirkningstidspunkt(
        behandlingId: UUID,
        virk: YearMonth,
    ): Behandling =
        runBlocking {
            inTransaction {
                runBlocking {
                    applicationContext.behandlingService.oppdaterVirkningstidspunkt(
                        behandlingId,
                        virk,
                        simpleSaksbehandler("saksbehandler"),
                        "aarsak",
                    )
                    applicationContext.behandlingService.hentBehandling(behandlingId)
                        ?: throw BehandlingNotFoundException(behandlingId)
                }
            }
        }

    private fun ferdigstillOppgaver(
        behandling: Behandling,
        saksbehandlerId: String = "sbh",
    ) {
        with(applicationContext.oppgaveService) {
            this.hentOppgaverForReferanse(behandling.id.toString()).forEach {
                if (it.manglerSaksbehandler()) {
                    this.tildelSaksbehandler(it.id, saksbehandlerId) // for aa kunne ferdgistille oppgaven
                }
                this.ferdigstillOppgave(
                    id = it.id,
                    saksbehandler = simpleSaksbehandler(saksbehandlerId),
                )
            }
        }
    }

    private fun manuellRevurderingService(
        revurderingService: RevurderingService,
        oppgaveService: OppgaveService = applicationContext.oppgaveService,
        grunnlagService: GrunnlagService = applicationContext.grunnlagService,
        behandlingService: BehandlingService = applicationContext.behandlingService,
    ) = ManuellRevurderingService(
        revurderingService = revurderingService,
        behandlingService = behandlingService,
        grunnlagService = grunnlagService,
        oppgaveService = oppgaveService,
        grunnlagsendringshendelseDao = applicationContext.grunnlagsendringshendelseDao,
    )

    private fun revurderingService(
        oppgaveService: OppgaveService = applicationContext.oppgaveService,
        grunnlagService: GrunnlagService = applicationContext.grunnlagService,
        behandlingsHendelser: BehandlingsHendelserKafkaProducerImpl = applicationContext.behandlingsHendelser,
        aktivitetspliktDao: AktivitetspliktDao = applicationContext.aktivitetspliktDao,
        aktivitetspliktKopierService: AktivitetspliktKopierService = applicationContext.aktivitetspliktKopierService,
    ) = RevurderingService(
        oppgaveService,
        grunnlagService,
        behandlingsHendelser,
        applicationContext.behandlingDao,
        applicationContext.hendelseDao,
        applicationContext.kommerBarnetTilGodeService,
        applicationContext.revurderingDao,
        aktivitetspliktDao,
        aktivitetspliktKopierService,
    )

    private fun behandlingFactory() =
        BehandlingFactory(
            oppgaveService = applicationContext.oppgaveService,
            grunnlagService = applicationContext.grunnlagService,
            revurderingService = applicationContext.revurderingService,
            gyldighetsproevingService = applicationContext.gyldighetsproevingService,
            sakService = applicationContext.sakService,
            behandlingDao = applicationContext.behandlingDao,
            hendelseDao = applicationContext.hendelseDao,
            behandlingHendelser = applicationContext.behandlingsHendelser,
            vilkaarsvurderingService = applicationContext.vilkaarsvurderingService,
            kommerBarnetTilGodeService = applicationContext.kommerBarnetTilGodeService,
            behandlingInfoService = mockk(),
            tilgangsService =
                OppdaterTilgangService(
                    applicationContext.skjermingKlient,
                    applicationContext.pdlTjenesterKlient,
                    applicationContext.brukerService,
                    applicationContext.oppgaveService,
                    applicationContext.sakSkrivDao,
                    applicationContext.sakTilgang,
                    applicationContext.sakLesDao,
                    applicationContext.featureToggleService,
                ),
        )
}
