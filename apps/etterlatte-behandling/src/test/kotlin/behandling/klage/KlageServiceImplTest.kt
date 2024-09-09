package no.nav.etterlatte.behandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleAttestant
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.GrunnForOmgjoering
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabalUtenBrev
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOmgjoering
import no.nav.etterlatte.libs.common.behandling.KlageResultat
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.KlageVedtaksbrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SendtInnstillingsbrev
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.route.FeatureIkkeStoettetException
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random
import no.nav.etterlatte.brev.model.Status as BrevStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlageServiceImplTest : BehandlingIntegrationTest() {
    private lateinit var service: KlageService
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var oppgaveService: OppgaveService
    private lateinit var hendelseDao: HendelseDao
    private lateinit var klageDao: KlageDao
    private val brevApiKlientMock = mockk<BrevApiKlient>()

    private val saksbehandler = simpleSaksbehandler(ident = saksbehandlerIdent)
    private val attestant = simpleAttestant(ident = attestantIdent)
    private val klagerFnr = GrunnlagTestData().gjenlevende.foedselsnummer.value
    private val enhet = "1337"

    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @BeforeEach
    fun setUp() {
        sakSkrivDao = applicationContext.sakSkrivDao
        service = applicationContext.klageService
        oppgaveService = applicationContext.oppgaveService
        klageDao = applicationContext.klageDao
        hendelseDao = applicationContext.hendelseDao

        coEvery { brevApiKlientMock.distribuerBrev(any(), any(), any()) } returns BestillingsIdDto(randomString())
        coEvery { brevApiKlientMock.ferdigstillVedtaksbrev(any(), any(), any()) } just runs
        coEvery { brevApiKlientMock.ferdigstillOversendelseBrev(any(), any(), any()) } just runs
        coEvery { brevApiKlientMock.hentBrev(any(), any(), any()) } returns randomOpprettetBrevDto()
        coEvery { brevApiKlientMock.journalfoerBrev(any(), any(), any()) } returns JournalpostIdDto(randomString())
        coEvery { brevApiKlientMock.journalfoerNotatKa(any(), any()) } returns OpprettJournalpostDto(randomString())
        coEvery { brevApiKlientMock.opprettKlageOversendelsesbrevISak(any(), any()) } returns randomOpprettetBrevDto()
        coEvery { brevApiKlientMock.opprettVedtaksbrev(any(), any(), any()) } returns randomOpprettetBrevDto()
        coEvery { brevApiKlientMock.hentVedtaksbrev(any(), any()) } returns randomOpprettetBrevDto()
        coEvery { brevApiKlientMock.hentOversendelsesbrev(any(), any()) } returns randomOpprettetBrevDto()
        coEvery { brevApiKlientMock.slettVedtaksbrev(any(), any()) } returns Unit
        coEvery { brevApiKlientMock.slettOversendelsesbrev(any(), any()) } returns Unit
    }

    @BeforeAll
    fun start() {
        val user =
            mockk<SaksbehandlerMedEnheterOgRoller> {
                every { name() } returns "User"
                every { enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)
                every { saksbehandlerMedRoller } returns
                    mockk<SaksbehandlerMedRoller> {
                        every { harRolleAttestant() } returns true
                        every { harRolleStrengtFortrolig() } returns false
                        every { harRolleEgenAnsatt() } returns false
                    }
            }
        startServer(
            featureToggleService = DummyFeatureToggleService(),
            brevApiKlient = brevApiKlientMock,
        )
        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun afterAllTests() {
        afterAll()
    }

    @AfterEach
    fun tearDown() {
        dbExtension.resetDb()
    }

    @Test
    fun `hentKlage gir null naar klagen ikke finnes`() {
        inTransaction {
            assertNull(service.hentKlage(UUID.randomUUID()))
        }
    }

    @Test
    fun `avbryt klage avbryter både klagen og oppgaven`() {
        val klage =
            inTransaction {
                val sak = oppprettOmsSak()
                opprettKlage(sak)
            }
        inTransaction {
            service.avbrytKlage(klage.id, AarsakTilAvbrytelse.FEILREGISTRERT, "Fordi jeg vil", saksbehandler)

            val hentetKlage = requireNotNull(service.hentKlage(klage.id))
            with(hentetKlage) {
                status shouldBe KlageStatus.AVBRUTT
                aarsakTilAvbrytelse shouldBe AarsakTilAvbrytelse.FEILREGISTRERT
            }

            val hendelserISak = hendelseDao.hentHendelserISak(klage.sak.id)
            hendelserISak.size shouldBe 2
            hendelserISak.first().hendelse shouldBe "KLAGE:OPPRETTET"
            hendelserISak.last().let {
                it.behandlingId shouldBe klage.id
                it.hendelse shouldBe "KLAGE:AVBRUTT"
                it.ident shouldBe saksbehandlerIdent
                it.identType shouldBe "SAKSBEHANDLER"
                it.inntruffet shouldNotBe null
                it.kommentar shouldBe "Fordi jeg vil"
                it.opprettet shouldNotBe null
                it.sakId shouldBe klage.sak.id
                it.valgtBegrunnelse shouldBe "FEILREGISTRERT"
                it.valgtBegrunnelse shouldBe AarsakTilAvbrytelse.FEILREGISTRERT.name
                it.vedtakId shouldBe null
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
        KlageStatus::class,
        names = [
            "OPPRETTET",
            "FORMKRAV_OPPFYLT",
            "FORMKRAV_IKKE_OPPFYLT",
            "UTFALL_VURDERT",
            "FATTET_VEDTAK",
            "RETURNERT",
        ],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `avbryt klage feiler hvis ulovlig status`(statusUnderTest: KlageStatus) {
        val klage =
            inTransaction {
                val sak = oppprettOmsSak()
                opprettKlage(sak, status = statusUnderTest)
            }
        shouldThrow<IkkeTillattException> {
            inTransaction {
                service.avbrytKlage(
                    klage.id,
                    AarsakTilAvbrytelse.FEILREGISTRERT,
                    "Fordi jeg vil",
                    saksbehandler,
                )
            }
        }
    }

    @Test
    fun `lagreInitieltUtfallMedBegrunnelseAvKlage gaar bra`() {
        val klage =
            inTransaction {
                val sak = oppprettOmsSak()
                val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)
                service.lagreFormkravIKlage(
                    klage.id,
                    formkrav(),
                    saksbehandler,
                )
            }
        inTransaction {
            service.lagreInitieltUtfallMedBegrunnelseAvKlage(
                klageId = klage.id,
                utfall =
                    InitieltUtfallMedBegrunnelseDto(
                        utfall = KlageUtfall.STADFESTE_VEDTAK,
                        begrunnelse = "Yndig rosmarin",
                    ),
                saksbehandler = saksbehandler,
            )
            val oppdatert = service.hentKlage(klage.id)
            oppdatert?.initieltUtfall?.utfallMedBegrunnelse?.utfall shouldBe KlageUtfall.STADFESTE_VEDTAK
            oppdatert?.initieltUtfall?.utfallMedBegrunnelse?.begrunnelse shouldBe "Yndig rosmarin"
            oppdatert?.initieltUtfall?.tidspunkt shouldNotBe null
            oppdatert?.initieltUtfall?.saksbehandler shouldBe saksbehandler.ident
        }
    }

    @Test
    fun `lagreInitieltUtfallMedBegrunnelseAvKlage kaster hvis utfallet ikke er stottet`() {
        val klage =
            inTransaction {
                val sak = oppprettOmsSak()
                val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)
                service.lagreFormkravIKlage(
                    klage.id,
                    formkrav(),
                    saksbehandler,
                )
            }
        shouldThrow<FeatureIkkeStoettetException> {
            inTransaction {
                service.lagreInitieltUtfallMedBegrunnelseAvKlage(
                    klageId = klage.id,
                    utfall =
                        InitieltUtfallMedBegrunnelseDto(
                            utfall = KlageUtfall.DELVIS_OMGJOERING,
                            begrunnelse = "Yndig revebjelle",
                        ),
                    saksbehandler = saksbehandler,
                )
            }
        }
    }

    @Test
    fun `lagreUtfallAvKlage feiler hvis initielt utfall ikke er satt`() {
        val klage =
            inTransaction {
                val sak = oppprettOmsSak()
                val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)
                service.lagreFormkravIKlage(klage.id, formkrav(), saksbehandler)
            }
        shouldThrow<IllegalStateException> {
            inTransaction {
                service.lagreUtfallAvKlage(
                    klage.id,
                    KlageUtfallUtenBrev.Omgjoering(
                        KlageOmgjoering(GrunnForOmgjoering.FEIL_LOVANVENDELSE, "Lorem ipsum"),
                    ),
                    saksbehandler,
                )
            }
        }
    }

    @Test
    fun `lagring av utfall avvist gjenbruker vedtaksbrev hvis det finnes fra før`() {
        coEvery { brevApiKlientMock.hentVedtaksbrev(any(), any()) } returns opprettetBrevDto(1814)

        val klage =
            inTransaction {
                opprettKlageOgSettInitieltUtfallAvvist()
            }

        inTransaction {
            service.lagreUtfallAvKlage(
                klageId = klage.id,
                utfall = KlageUtfallUtenBrev.Avvist(),
                saksbehandler = saksbehandler,
            )
        }

        inTransaction {
            val oppdatert = requireNotNull(service.hentKlage(klage.id))
            with(oppdatert.utfall as KlageUtfallMedData.Avvist) {
                this.brev shouldBe KlageVedtaksbrev(1814)
            }
        }
    }

    @Test
    fun `attestering trigger sletting av oversendelsesbrev`() {
        coEvery { brevApiKlientMock.hentOversendelsesbrev(any(), any()) } returns opprettetBrevDto(1432)
        val klageTilAttestering: Klage =
            inTransaction {
                val klage = opprettKlageOgSettInitieltUtfallAvvist()
                service.lagreUtfallAvKlage(
                    klageId = klage.id,
                    utfall = KlageUtfallUtenBrev.Avvist(),
                    saksbehandler = saksbehandler,
                )
                service.fattVedtak(klage.id, saksbehandler).also {
                    oppgaveService.tildelSaksbehandler(
                        oppgaveService.hentOppgaverForReferanse(klage.id.toString()).single().id,
                        attestant.ident,
                    )
                }
            }
        inTransaction {
            service.attesterVedtak(klageTilAttestering.id, "Gul Golf", attestant)
        }

        inTransaction {
            coVerify { brevApiKlientMock.hentOversendelsesbrev(klageTilAttestering.id, attestant) }
            coVerify { brevApiKlientMock.slettOversendelsesbrev(klageTilAttestering.id, attestant) }
        }
    }

    @Test
    fun `ferdigstilling med stadfesting lykkes`() {
        val oversendelsebrevId: Long = 234
        with(brevApiKlientMock) {
            coEvery { hentBrev(any(), oversendelsebrevId, any()) } returns opprettetBrevDto(oversendelsebrevId)
            coEvery { opprettKlageOversendelsesbrevISak(any(), any()) } returns opprettetBrevDto(oversendelsebrevId)
            coEvery { hentOversendelsesbrev(any(), any()) } returns opprettetBrevDto(oversendelsebrevId)
        }
        val klage: Klage =
            inTransaction {
                val klage = opprettKlageOgSettInitieltUtfallOmgjoering()
                service.lagreUtfallAvKlage(klage.id, utfallStadfesting(), saksbehandler)
            }
        inTransaction {
            service.ferdigstillKlage(klage.id, saksbehandler)
        }

        inTransaction {
            with(service.hentKlage(klage.id)!!) {
                status shouldBe KlageStatus.FERDIGSTILT
                resultat?.opprettetOppgaveOmgjoeringId shouldBe null
                resultat?.sendtInnstillingsbrev?.brevId shouldBe oversendelsebrevId
                resultat?.sendtInnstillingsbrev?.sendtKabalTidspunkt shouldNotBe null
                resultat?.sendtInnstillingsbrev?.journalpostId shouldNotBe null
                resultat?.sendtInnstillingsbrev?.journalfoerTidspunkt shouldNotBe null
                KlageResultat(null, SendtInnstillingsbrev(Tidspunkt.now(), Tidspunkt.now(), 2, ""))
            }
        }
    }

    @Test
    fun `ferdigstilling med omgjoering lykkes`() {
        val klage: Klage =
            inTransaction {
                val klage = opprettKlageOgSettInitieltUtfallOmgjoering()
                service.lagreUtfallAvKlage(klage.id, utfallOmgjoering(), saksbehandler)
            }
        inTransaction {
            service.ferdigstillKlage(klage.id, saksbehandler)
        }

        inTransaction {
            val omgjoeringsOppgave =
                oppgaveService
                    .hentOppgaverForReferanse(klage.id.toString())
                    .single { it.type == OppgaveType.OMGJOERING }
                    .also {
                        it.saksbehandler shouldBe null
                        it.sakId shouldBe klage.sak.id
                        it.kilde shouldBe OppgaveKilde.BEHANDLING
                        it.status shouldBe Status.NY
                    }

            with(service.hentKlage(klage.id)!!) {
                status shouldBe KlageStatus.FERDIGSTILT
                resultat shouldBe KlageResultat(omgjoeringsOppgave.id, null)
            }
            coVerify { brevApiKlientMock.hentVedtaksbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.slettVedtaksbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.hentOversendelsesbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.slettOversendelsesbrev(klage.id, saksbehandler) }
        }
    }

    @Test
    fun `ferdigstilling takler feil ved sletting av vedtaksbrev`() {
        coEvery { brevApiKlientMock.slettVedtaksbrev(any(), any()) } throws IllegalStateException()

        val klage: Klage =
            inTransaction {
                val klage = opprettKlageOgSettInitieltUtfallOmgjoering()
                service.lagreUtfallAvKlage(klage.id, utfallOmgjoering(), saksbehandler)
            }
        inTransaction {
            service.ferdigstillKlage(klage.id, saksbehandler)
        }

        inTransaction {
            service.hentKlage(klage.id)?.status shouldBe KlageStatus.FERDIGSTILT

            coVerify { brevApiKlientMock.hentVedtaksbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.slettVedtaksbrev(klage.id, saksbehandler) }
        }
    }

    @Test
    fun `ferdigstilling takler feil ved sletting av oversendelsesbrev`() {
        coEvery { brevApiKlientMock.slettOversendelsesbrev(any(), any()) } throws IllegalStateException()

        val klage: Klage =
            inTransaction {
                val klage = opprettKlageOgSettInitieltUtfallOmgjoering()
                service.lagreUtfallAvKlage(klage.id, utfallOmgjoering(), saksbehandler)
            }
        inTransaction {
            service.ferdigstillKlage(klage.id, saksbehandler)
        }

        inTransaction {
            service.hentKlage(klage.id)?.status shouldBe KlageStatus.FERDIGSTILT

            coVerify { brevApiKlientMock.hentVedtaksbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.slettVedtaksbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.hentOversendelsesbrev(klage.id, saksbehandler) }
            coVerify { brevApiKlientMock.slettOversendelsesbrev(klage.id, saksbehandler) }
        }
    }

    private fun utfallOmgjoering() =
        KlageUtfallUtenBrev.Omgjoering(
            KlageOmgjoering(GrunnForOmgjoering.FEIL_LOVANVENDELSE, "Bra klage"),
        )

    private fun utfallStadfesting() =
        KlageUtfallUtenBrev.StadfesteVedtak(
            InnstillingTilKabalUtenBrev(
                KabalHjemmel.FVL_36.name,
                "Intern kommentar",
                "Bra klage, men vi er uenige!",
            ),
        )

    private fun formkrav() =
        Formkrav(
            vedtaketKlagenGjelder =
                VedtaketKlagenGjelder(
                    behandlingId = UUID.randomUUID().toString(),
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
        )

    private fun opprettKlageOgSettInitieltUtfallAvvist(): Klage {
        val sak = oppprettOmsSak()
        val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)
        oppgaveService.tildelSaksbehandler(
            oppgaveService.hentOppgaverForReferanse(klage.id.toString()).single().id,
            saksbehandler.ident,
        )
        service.lagreFormkravIKlage(klage.id, formkrav(), saksbehandler)
        return service.lagreInitieltUtfallMedBegrunnelseAvKlage(
            klage.id,
            InitieltUtfallMedBegrunnelseDto(KlageUtfall.AVVIST, "Rød Trost"),
            saksbehandler,
        )
    }

    private fun opprettKlageOgSettInitieltUtfallOmgjoering(): Klage {
        val sak = oppprettOmsSak()
        val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)

        oppgaveService.tildelSaksbehandler(
            oppgaveService.hentOppgaverForReferanse(klage.id.toString()).single().id,
            saksbehandler.ident,
        )
        service.lagreFormkravIKlage(klage.id, formkrav(), saksbehandler)
        return service.lagreInitieltUtfallMedBegrunnelseAvKlage(
            klage.id,
            InitieltUtfallMedBegrunnelseDto(KlageUtfall.OMGJOERING, "Tander Tiger"),
            saksbehandler,
        )
    }

    private fun oppprettOmsSak() = sakSkrivDao.opprettSak(klagerFnr, SakType.OMSTILLINGSSTOENAD, enhet)

    private fun opprettKlage(
        sak: Sak,
        status: KlageStatus? = null,
    ): Klage {
        val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""), saksbehandler)
        tildelOppgave(klage)
        return if (status != null) {
            klage.copy(status = status).also { klageDao.lagreKlage(it) }
        } else {
            klage
        }
    }

    private fun tildelOppgave(klage: Klage) {
        oppgaveService.hentOppgaverForReferanse(klage.id.toString()).single().let {
            oppgaveService.tildelSaksbehandler(it.id, saksbehandler.ident)
        }
    }

    private fun opprettetBrevDto(brevId: Long) =
        Brev(
            id = brevId,
            status = BrevStatus.OPPRETTET,
            mottaker =
                Mottaker(
                    navn = "Mottaker mottakersen",
                    foedselsnummer = MottakerFoedselsnummer("19448310410"),
                    orgnummer = null,
                    adresse =
                        Adresse(
                            adresseType = "",
                            landkode = "",
                            land = "",
                        ),
                ),
            journalpostId = null,
            bestillingId = null,
            sakId = 0,
            behandlingId = null,
            tittel = null,
            spraak = Spraak.NB,
            prosessType = BrevProsessType.REDIGERBAR,
            soekerFnr = "",
            statusEndret = Tidspunkt.now(),
            opprettet = Tidspunkt.now(),
            brevtype = Brevtype.MANUELT,
            brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
        )

    private fun randomString() = Random.nextLong().toString()

    private fun randomOpprettetBrevDto() = opprettetBrevDto(Random.nextLong())
}
