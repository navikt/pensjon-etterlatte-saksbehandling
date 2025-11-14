package no.nav.etterlatte.trygdetid

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlient
import no.nav.etterlatte.trygdetid.klienter.VedtaksvurderingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidServiceIntegrationTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val saksbehandler = simpleSaksbehandler()

    private val repository = TrygdetidRepository(dataSource)
    private lateinit var trygdetidService: TrygdetidService

    private val pdlKilde: Grunnlagsopplysning.Pdl = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
    private val regelKilde: Grunnlagsopplysning.RegelKilde = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")

    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()

    private val behandlingKlient = mockk<BehandlingKlient>()
    private val avtaleService = mockk<AvtaleService>()
    private val vedtaksvurderingKlient = mockk<VedtaksvurderingKlient>()

    @BeforeAll
    fun beforeAll() {
        no.nav.etterlatte.logger
            .info(dbExtension.properties().toString())
        trygdetidService =
            TrygdetidServiceImpl(
                repository,
                behandlingKlient,
                grunnlagKlient,
                TrygdetidBeregningService,
                mockk<PesysKlient>(),
                avtaleService,
                vedtaksvurderingKlient,
                DummyFeatureToggleService().also {
                    it.settBryter(TrygdetidToggles.OPPDATER_BEREGNET_TRYGDETID_VED_KOPIERING, true)
                },
            )
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal hente trygdetid med differanse i opplysninger`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val nyDoedsdato =
            grunnlagTestData.avdoede
                .first()
                .doedsdato!!
                .plusDays(6)
        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagMedNyDoedsdato(nyDoedsdato)

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = randomSakId(),
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }

        with(trygdetider.firstOrNull()?.opplysningerDifferanse!!) {
            differanse shouldBe true
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoede.first().foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe nyDoedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(66)
            }
        }
    }

    @Test
    fun `skal opprette ny trygdetid og overskrive eksisterende`() {
        val behandlingId = UUID.randomUUID()
        val sakId = randomSakId()
        val grunnlagTestData = GrunnlagTestData()

        coEvery { avtaleService.hentAvtaleForBehandling(any()) } returns null
        coEvery { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) } returns true
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler) } returns true
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns
            mockk {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { tidligereFamiliepleier } returns null
            }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlagTestData.hentOpplysningsgrunnlag()

        val trygdetidOpprinnelig =
            repository.opprettTrygdetid(
                trygdetid(
                    behandlingId = behandlingId,
                    sakId = sakId,
                    opplysninger = opplysningsgrunnlag(grunnlagTestData),
                ),
            )

        val trygdetidOverskrevet =
            runBlocking {
                trygdetidService.opprettTrygdetiderForBehandling(behandlingId, saksbehandler, overskriv = true)
            }.first()

        trygdetidOverskrevet shouldNotBe null
        trygdetidOverskrevet.id shouldNotBeEqual trygdetidOpprinnelig.id
    }

    @Test
    fun `skal hente trygdetid uten differanse i opplysninger`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagTestData.hentOpplysningsgrunnlag()

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = randomSakId(),
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }
        with(trygdetider.firstOrNull()?.opplysningerDifferanse!!) {
            differanse shouldBe false
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoede.first().foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe grunnlagTestData.avdoede.first().doedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(66)
            }
        }
    }

    @Test
    fun `skal hente trygdetid uten opplysningsgrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()
        val standardOpplysningsgrunnlag = grunnlagTestData.hentOpplysningsgrunnlag()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns
            Grunnlag(
                soeker = standardOpplysningsgrunnlag.soeker,
                familie = emptyList(),
                sak = standardOpplysningsgrunnlag.sak,
                metadata = standardOpplysningsgrunnlag.metadata,
            )
        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = randomSakId(),
                ident = UKJENT_AVDOED,
                opplysninger = emptyList(),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        with(trygdetider.first()) {
            ident shouldBe UKJENT_AVDOED
            opplysningerDifferanse!! shouldBeEqual OpplysningerDifferanse(false, GrunnlagOpplysningerDto.tomt())
        }
    }

    @Test
    fun `skal slette trygdetid hvis ident ikke lenger finnes i familie`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagTestData.hentOpplysningsgrunnlag()

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = randomSakId(),
                ident = "123",
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        trygdetider shouldBe emptyList()
    }

    @Test
    fun `skal kopiere trygdetid fra annen behandling`() {
        val behandlingId = UUID.randomUUID()
        val kildeBehandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val opplysningsgrunnlag = grunnlagTestData.hentOpplysningsgrunnlag()
        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns opplysningsgrunnlag

        coEvery { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) } returns true
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler) } returns true
        every { avtaleService.hentAvtaleForBehandling(any()) } returns opprettTrygdeavtale(kildeBehandlingId)
        justRun { avtaleService.opprettAvtale(any()) }
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns
            mockk {
                every { id } returns behandlingId
                every { sak } returns randomSakId()
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
                every { tidligereFamiliepleier } returns null
                every { revurderingsaarsak } returns null
                every { prosesstype } returns Prosesstype.MANUELL
            }

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = randomSakId(),
                trygdetidGrunnlag =
                    listOf(
                        trygdetidGrunnlag(
                            periode = TrygdetidPeriode(fra = LocalDate.of(2021, 1, 1), til = LocalDate.of(2021, 2, 20)),
                        ),
                    ),
            ),
        )

        repository
            .opprettTrygdetid(
                trygdetid(
                    behandlingId = kildeBehandlingId,
                    sakId = randomSakId(),
                    trygdetidGrunnlag =
                        listOf(
                            trygdetidGrunnlag(
                                periode = TrygdetidPeriode(fra = LocalDate.of(2020, 5, 1), til = LocalDate.of(2020, 7, 1)),
                            ),
                        ),
                    overstyrtNorskPoengaar = 22,
                    yrkesskade = true,
                ),
            ).also { it.beregnetTrygdetid shouldBe null }

        runBlocking { trygdetidService.kopierOgOverskrivTrygdetid(behandlingId, kildeBehandlingId, saksbehandler) }

        val trygdetidList = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }
        trygdetidList.size shouldBe 1

        val trygdetid = trygdetidList.first()
        trygdetid.kopiertGrunnlagFraBehandling shouldBe kildeBehandlingId
        with(trygdetid.trygdetidGrunnlag.sortedBy { it.periode.fra }) {
            this[0].periode.fra shouldBe LocalDate.of(2020, 5, 1)
            this[0].periode.til shouldBe LocalDate.of(2020, 7, 1)
        }
        trygdetid.beregnetTrygdetid shouldNotBe null
        trygdetid.overstyrtNorskPoengaar shouldBe 22
        trygdetid.yrkesskade shouldBe true

        verify(exactly = 1) {
            avtaleService.hentAvtaleForBehandling(kildeBehandlingId)
            avtaleService.opprettAvtale(any())
        }
    }

    @Test
    fun `skal kopiere uten reberegning av trygdetid fra annen behandling hvis vi har en automatisk prosesstype`() {
        val behandlingId = UUID.randomUUID()
        val kildeBehandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val opplysningsgrunnlag = grunnlagTestData.hentOpplysningsgrunnlag()
        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns opplysningsgrunnlag

        coEvery { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) } returns true
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler) } returns true
        every { avtaleService.hentAvtaleForBehandling(any()) } returns opprettTrygdeavtale(kildeBehandlingId)
        justRun { avtaleService.opprettAvtale(any()) }
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns
            mockk {
                every { id } returns behandlingId
                every { sak } returns randomSakId()
                every { behandlingType } returns BehandlingType.REVURDERING
                every { tidligereFamiliepleier } returns null
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
                every { prosesstype } returns Prosesstype.AUTOMATISK
            }
        val detaljertResultat =
            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge = null,
                faktiskTrygdetidTeoretisk = null,
                fremtidigTrygdetidNorge = null,
                fremtidigTrygdetidTeoretisk = null,
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = 40,
                prorataBroek = null,
                overstyrt = false,
                yrkesskade = false,
                beregnetSamletTrygdetidNorge = null,
                overstyrtBegrunnelse = null,
            )
        repository
            .opprettTrygdetid(
                trygdetid(
                    behandlingId = kildeBehandlingId,
                    sakId = randomSakId(),
                    trygdetidGrunnlag = listOf(),
                    overstyrtNorskPoengaar = null,
                    yrkesskade = true,
                    beregnetTrygdetid =
                        DetaljertBeregnetTrygdetid(
                            resultat = detaljertResultat,
                            tidspunkt = Tidspunkt.now(),
                            regelResultat = "".toJsonNode(),
                        ),
                ),
            )

        runBlocking { trygdetidService.kopierSisteTrygdetidberegninger(behandlingId, kildeBehandlingId, saksbehandler) }

        val trygdetidList = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }
        trygdetidList.size shouldBe 1

        val trygdetid = trygdetidList.first()
        trygdetid.kopiertGrunnlagFraBehandling shouldBe kildeBehandlingId
        trygdetid.beregnetTrygdetid shouldNotBe null
        trygdetid.beregnetTrygdetid?.resultat shouldBe detaljertResultat
    }

    @Test
    fun `skal kopiere uten reberegning av trygdetid fra annen behandling hvis trygdetid er overstyrt`() {
        val behandlingId = UUID.randomUUID()
        val kildeBehandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val opplysningsgrunnlag = grunnlagTestData.hentOpplysningsgrunnlag()
        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns opplysningsgrunnlag

        coEvery { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) } returns true
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler) } returns true
        every { avtaleService.hentAvtaleForBehandling(any()) } returns opprettTrygdeavtale(kildeBehandlingId)
        justRun { avtaleService.opprettAvtale(any()) }
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns
            mockk {
                every { id } returns behandlingId
                every { sak } returns randomSakId()
                every { behandlingType } returns BehandlingType.REVURDERING
                every { tidligereFamiliepleier } returns null
                every { revurderingsaarsak } returns Revurderingaarsak.ANNEN
            }

        val detaljertResultat =
            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge = null,
                faktiskTrygdetidTeoretisk = null,
                fremtidigTrygdetidNorge = null,
                fremtidigTrygdetidTeoretisk = null,
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = 40,
                prorataBroek = null,
                overstyrt = true,
                yrkesskade = false,
                beregnetSamletTrygdetidNorge = null,
                overstyrtBegrunnelse = "Overstyrt for moro skyld",
            )
        repository
            .opprettTrygdetid(
                trygdetid(
                    behandlingId = kildeBehandlingId,
                    sakId = randomSakId(),
                    trygdetidGrunnlag = listOf(),
                    overstyrtNorskPoengaar = null,
                    yrkesskade = true,
                    beregnetTrygdetid =
                        DetaljertBeregnetTrygdetid(
                            resultat = detaljertResultat,
                            tidspunkt = Tidspunkt.now(),
                            regelResultat = "".toJsonNode(),
                        ),
                ),
            )

        runBlocking { trygdetidService.kopierSisteTrygdetidberegninger(behandlingId, kildeBehandlingId, saksbehandler) }

        val trygdetidList = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }
        trygdetidList.size shouldBe 1

        val trygdetid = trygdetidList.first()
        trygdetid.kopiertGrunnlagFraBehandling shouldBe kildeBehandlingId
        trygdetid.beregnetTrygdetid shouldNotBe null
        trygdetid.beregnetTrygdetid?.resultat shouldBe detaljertResultat
    }

    @Test
    fun `skal oppdatere begrunnelse i trygdetid`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()
        val begrunnelseTekst = "En begrunnelse"

        coEvery { grunnlagKlient.hentGrunnlag(any(), any()) } returns grunnlagTestData.hentOpplysningsgrunnlag()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) } returns true

        val opprettetTrygdetid =
            repository.opprettTrygdetid(
                trygdetid(
                    behandlingId = behandlingId,
                    sakId = randomSakId(),
                    opplysninger = opplysningsgrunnlag(grunnlagTestData),
                ),
            )

        val trygdetidMedBegrunnelse =
            runBlocking {
                trygdetidService.oppdaterTrygdetidMedBegrunnelse(
                    trygdetidId = opprettetTrygdetid.id,
                    behandlingId = behandlingId,
                    begrunnelse = begrunnelseTekst,
                    brukerTokenInfo = saksbehandler,
                )
            }

        trygdetidMedBegrunnelse.begrunnelse shouldBe begrunnelseTekst

        val trygdetidMedSlettetBegrunnelse =
            runBlocking {
                trygdetidService.oppdaterTrygdetidMedBegrunnelse(
                    trygdetidId = opprettetTrygdetid.id,
                    behandlingId = behandlingId,
                    begrunnelse = null,
                    brukerTokenInfo = saksbehandler,
                )
            }

        trygdetidMedSlettetBegrunnelse.begrunnelse shouldBe null
    }

    private fun opprettTrygdeavtale(behandlingId: UUID) =
        Trygdeavtale(
            behandlingId = behandlingId,
            avtaleKode = "avtaleKode",
            avtaleDatoKode = "avtaleDatoKode",
            avtaleKriteriaKode = "avtaleKriteriaKode",
            personKrets = null,
            arbInntekt1G = null,
            arbInntekt1GKommentar = "arbInntekt1GKommentar",
            beregArt50 = null,
            beregArt50Kommentar = "beregArt50Kommentar",
            nordiskTrygdeAvtale = null,
            nordiskTrygdeAvtaleKommentar = "nordiskTrygdeAvtaleKommentar",
            kilde = Grunnlagsopplysning.Saksbehandler("ident", Tidspunkt.now()),
        )

    private fun opplysningsgrunnlag(grunnlagTestData: GrunnlagTestData): List<Opplysningsgrunnlag> {
        val foedselsdato = grunnlagTestData.avdoede.first().foedselsdato!!
        val doedsdato = grunnlagTestData.avdoede.first().doedsdato!!
        val seksten =
            grunnlagTestData.avdoede
                .first()
                .foedselsdato!!
                .plusYears(16)
        val sekstiseks =
            grunnlagTestData.avdoede
                .first()
                .foedselsdato!!
                .plusYears(66)
        return listOf(
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdlKilde, foedselsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdlKilde, doedsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regelKilde, seksten),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regelKilde, sekstiseks),
        )
    }

    private fun grunnlagMedNyDoedsdato(nyDoedsdato: LocalDate): Grunnlag {
        val grunnlagTestData =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides =
                    mapOf(Opplysningstype.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, nyDoedsdato.toJsonNode())),
            )
        return grunnlagTestData.hentOpplysningsgrunnlag()
    }
}
