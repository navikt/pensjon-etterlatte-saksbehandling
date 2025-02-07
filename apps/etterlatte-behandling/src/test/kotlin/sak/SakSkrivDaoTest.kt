package no.nav.etterlatte.sak

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.date.shouldBeToday
import io.kotest.matchers.date.shouldNotBeToday
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.opprettBehandling
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.sql.DataSource
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakSkrivDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var sakLesDao: SakLesDao
    private lateinit var tilgangService: TilgangServiceSjekker
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var kommerBarnetTilGodeDao: KommerBarnetTilGodeDao
    private lateinit var sakendringerDao: SakendringerDao

    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @BeforeAll
    fun beforeAll() {
        val connectionAutoclosing = ConnectionAutoclosingTest(dataSource)
        sakLesDao = SakLesDao(connectionAutoclosing)
        sakendringerDao = SakendringerDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(sakendringerDao)
        tilgangService = TilgangServiceSjekkerImpl(SakTilgangDao(dataSource))
        kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource))
        behandlingRepo =
            BehandlingDao(
                kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                ConnectionAutoclosingTest(dataSource),
            )
        nyKontekstMedBruker(
            mockk<SaksbehandlerMedEnheterOgRoller> {
                every { name() } returns "Børre"
            },
        )
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `klarer å sette opprett for saker der de har det på behandlingen`() {
        val fnrMedBehandling = "1231234"
        val opprettSak = sakSkrivDao.opprettSak(fnrMedBehandling, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val fnrUtenbehandling = "123124124"
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = opprettSak.id,
                prosesstype = Prosesstype.MANUELL,
            )

        behandlingRepo.opprettBehandling(opprettBehandling)

        // setter opprettet dato fra behandling
        dataSource.connection.use {
            it
                .prepareStatement(
                    """
                    UPDATE sak as s
                    SET opprettet = (select behandling_opprettet from behandling as b where b.sak_id = s.id);
                    """.trimIndent(),
                ).executeUpdate()
        }

        val saker =
            dataSource.connection.use {
                it
                    .prepareStatement(
                        """
                        select fnr, opprettet from sak;    
                        """.trimIndent(),
                    ).executeQuery()
                    .toList {
                        Pair(getString("fnr"), getTidspunktOrNull("opprettet"))
                    }
            }
        val sakmedTidligereDato = saker.find { it.first === fnrMedBehandling }

        sakmedTidligereDato?.second?.toLocalDate()?.shouldBeToday()

        dataSource.connection.use {
            it
                .prepareStatement(
                    """
                    UPDATE sak
                    SET opprettet = 'yesterday'::TIMESTAMP
                    WHERE sak.opprettet IS NULL;
                    """.trimIndent(),
                ).executeUpdate()
        }

        val beggesakerMedDato =
            dataSource.connection.use {
                it
                    .prepareStatement(
                        """
                        select fnr, opprettet from sak;    
                        """.trimIndent(),
                    ).executeQuery()
                    .toList {
                        Pair(getString("fnr"), getTidspunktOrNull("opprettet"))
                    }
            }

        val sakmedBehandlingsdato = beggesakerMedDato.find { it.first === fnrMedBehandling }
        sakmedBehandlingsdato?.second?.toLocalDate()?.shouldBeToday()
        val sakUtenbehandlingsdato = beggesakerMedDato.find { it.first === fnrUtenbehandling }
        sakUtenbehandlingsdato?.second?.toLocalDate()?.shouldNotBeToday()
        sakUtenbehandlingsdato?.second?.toLocalDate()?.shouldBeBetween(LocalDate.now().minusDays(2), LocalDate.now())
    }

    @Test
    fun `kan opprette sak`() {
        val opprettSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(Enheter.PORSGRUNN.enhetNr, opprettSak.enhet)
    }

    @Test
    fun `Returnerer null dersom flyktning ikke finnes`() {
        val opprettSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(sakLesDao.finnFlyktningForSak(opprettSak.id), null)
    }

    @Test
    fun `hentSakerMedIder henter kun de sakene med innsendt id`() {
        val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val alleSaker = listOf(sak1, sak2, sak3)

        val alleIder = alleSaker.map { it.id }
        val hentetAlleSaker = sakLesDao.hentSakerMedIder(alleIder)
        val hentetKunSak1 = sakLesDao.hentSakerMedIder(listOf(sak1.id))
        val hentingIngenSaker = sakLesDao.hentSakerMedIder(emptyList())
        val hentingUkjentSak = sakLesDao.hentSakerMedIder(listOf(randomSakId()))

        Assertions.assertEquals(alleSaker, hentetAlleSaker)
        Assertions.assertEquals(listOf(sak1), hentetKunSak1)
        Assertions.assertEquals(emptyList<Sak>(), hentingIngenSaker)
        Assertions.assertEquals(emptyList<Sak>(), hentingUkjentSak)
    }

    @Test
    fun `Skal kunne oppdatere enhet`() {
        val fnr = "fnr"
        val sak = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val funnetSaker = sakLesDao.finnSaker(fnr)
        Assertions.assertEquals(1, funnetSaker.size)
        Assertions.assertEquals(sak.id, funnetSaker[0].id)
        sakSkrivDao.opprettSak(fnr, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr).also {
            Assertions.assertNotNull(it)
        }
        val funnetSakermed2saker = sakLesDao.finnSaker(fnr)
        Assertions.assertEquals(2, funnetSakermed2saker.size)

        val sakerMedNyEnhet =
            funnetSakermed2saker.map {
                SakMedEnhet(it.id, Enheter.EGNE_ANSATTE.enhetNr)
            }

        sakSkrivDao.oppdaterEnheterPaaSaker(sakerMedNyEnhet)

        val sakerMedEgenAnsattEnhet = sakLesDao.finnSaker(fnr)
        sakerMedEgenAnsattEnhet.forEach {
            Assertions.assertEquals(Enheter.EGNE_ANSATTE.enhetNr, it.enhet)
        }
    }

    @Test
    fun `Skal kunne markere flere saker med skjerming`() {
        val sak1 = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.OMSTILLINGSSTOENAD, Enheter.AALESUND.enhetNr)

        sakSkrivDao.markerSakerMedSkjerming(listOf(sak1.id, sak2.id), true)
        sakSkrivDao.markerSakerMedSkjerming(listOf(sak1.id), false)

        sakendringerDao.hentKomplettSak(sak1.id)?.erSkjermet shouldBe false
        sakendringerDao.hentKomplettSak(sak2.id)?.erSkjermet shouldBe true

        sakendringerDao
            .hentEndringerForSak(sak1.id)
            .sortedBy { it.tidspunkt }
            .map { Pair(it.foer?.erSkjermet, it.etter.erSkjermet) } shouldBe
            listOf(
                Pair(null, null),
                Pair(null, true),
                Pair(true, false),
            )
        sakendringerDao
            .hentEndringerForSak(sak2.id)
            .sortedBy { it.tidspunkt }
            .map { Pair(it.foer?.erSkjermet, it.etter.erSkjermet) } shouldBe
            listOf(
                Pair(null, null),
                Pair(null, true),
            )
    }

    @Nested
    inner class HentAlleSaker {
        @Test
        fun `Skal hente angitte saker`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            listOf(sak1, sak2, sak3).forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }

            val saker = sakLesDao.hentSaker("", 2, listOf(sak2.id, sak3.id), emptyList())

            saker.size shouldBe 2
            saker.forEach { it.id shouldNotBe sak1.id }
        }

        @Test
        fun `Skal utelate ekskluderte saker`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak4 = sakSkrivDao.opprettSak("fnr4", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            listOf(sak1, sak2, sak3, sak4).forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }

            val saker = sakLesDao.hentSaker("", 4, emptyList(), ekskluderteSaker = listOf(sak1.id, sak2.id))

            saker.size shouldBe 2
            saker shouldContain sak3
            saker shouldContain sak4
        }

        @Test
        fun `Skal kun returnere spesifikke saker som ikke er ekskludert`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            listOf(sak1, sak2, sak3).forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }

            val saker =
                sakLesDao.hentSaker(
                    "",
                    4,
                    spesifikkeSaker = listOf(sak1.id, sak3.id),
                    ekskluderteSaker = listOf(sak1.id, sak2.id),
                )

            saker.size shouldBe 1
            saker shouldContain sak3
        }

        @Test
        fun `Skal hente alle saker dersom ingen spesifikke er angitt`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            listOf(sak1, sak2, sak3).forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }

            val saker = sakLesDao.hentSaker("", 3, emptyList(), emptyList())

            saker.size shouldBe 3
        }

        @Test
        fun `Hvis kjoering er starta, skal vi ikke hente ut`() {
            val sakid = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    sakId = sakid,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))

            val saker = sakLesDao.hentSaker("K1", 1, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er starta, og saa feila det, skal vi hente ut`() {
            val sakid = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    sakId = sakid,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FEILA, sakid))

            val saker = sakLesDao.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 1
        }

        @Test
        fun `Hvis kjoering er starta, og saa ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    sakId = sakid,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakLesDao.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er starta, og saa feila, og saa ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    sakId = sakid,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FEILA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakLesDao.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    sakId = sakid,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            )
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakLesDao.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Skal hente saker for gitt sakstype hvis sakstype er angitt`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak4 = sakSkrivDao.opprettSak("fnr4", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
            listOf(sak1, sak2, sak3, sak4).forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }
            sakLesDao
                .hentSaker("", 100, emptyList(), emptyList(), SakType.BARNEPENSJON)
                .map { it.ident } shouldContainExactlyInAnyOrder listOf("fnr1", "fnr2", "fnr3")

            sakLesDao
                .hentSaker("", 100, emptyList(), emptyList(), SakType.OMSTILLINGSSTOENAD)
                .map { it.ident } shouldBe listOf("fnr4")

            val saker = sakLesDao.hentSaker("", 2, emptyList(), emptyList(), SakType.BARNEPENSJON)
            saker.map { it.ident } shouldContainAnyOf listOf("fnr1", "fnr2", "fnr3")
            saker.size shouldBe 2
        }

        @Test
        fun `skal ikke hente saker som ikke har behandlinger`() {
            val sak1 = sakSkrivDao.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakSkrivDao.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            sakSkrivDao.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakSkrivDao.opprettSak("fnr4", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            val sakerMedBehandlinger = listOf(sak1, sak2)
            sakerMedBehandlinger.forEach {
                behandlingRepo.opprettBehandling(
                    opprettBehandling(
                        sakId = it.id,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                )
            }
            val saker = sakLesDao.hentSaker("", 100, emptyList(), emptyList())
            saker.map { it.ident } shouldContainExactlyInAnyOrder sakerMedBehandlinger.map { it.ident }
            saker.size shouldBe 2
        }
    }

    @Test
    fun `oppdater ident på sak med lagring av endring`() {
        val opprinneligIdent = Random.nextLong().toString()
        val sak = sakSkrivDao.opprettSak(opprinneligIdent, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        val nyIdent = KONTANT_FOT

        sakSkrivDao.oppdaterIdent(sak.id, nyIdent)

        val oppdatertSak = sakLesDao.hentSak(sak.id)!!

        oppdatertSak.id shouldBe sak.id
        oppdatertSak.enhet shouldBe sak.enhet
        oppdatertSak.sakType shouldBe sak.sakType

        oppdatertSak.ident shouldNotBe sak.ident
        oppdatertSak.ident shouldBe nyIdent.value

        val endringer: List<Pair<Sak, Sak>> =
            dataSource.connection.use {
                it
                    .prepareStatement(
                        """
                        SELECT foer, etter
                        FROM saksendring_bak
                        WHERE foer ->> 'id' = '${sak.id}'
                        """.trimIndent(),
                    ).executeQuery()
                    .toList {
                        deserialize<Sak>(getString("foer")) to deserialize<Sak>(getString("etter"))
                    }
            }

        endringer.single().let { (foer, etter) ->
            foer.id shouldBe sak.id

            foer.id shouldBe etter.id
            foer.enhet shouldBe etter.enhet
            foer.sakType shouldBe etter.sakType

            foer.ident shouldBe opprinneligIdent
            etter.ident shouldBe nyIdent.value
        }
    }

    @Test
    fun `skal hente saker med dato pleieforholdet opphoerte`() {
        val sak1 = sakSkrivDao.opprettSak("ident1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val sak2 = sakSkrivDao.opprettSak("ident2", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val pleieForholdEnStart = LocalDate.of(2021, Month.AUGUST, 7)
        val pleieForholdEnSlutt = LocalDate.of(2024, Month.AUGUST, 9)
        val pleieForholdToStart = LocalDate.of(2022, Month.SEPTEMBER, 1)
        val pleieForholdToSlutt = LocalDate.of(2024, Month.SEPTEMBER, 30)

        val behandlingSakEn =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak1.id,
            )

        behandlingRepo.opprettBehandling(behandlingSakEn)
        behandlingRepo.lagreTidligereFamiliepleier(
            behandlingSakEn.id,
            tidligereFamiliepleier =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                    foedselsnummer = "pleid1",
                    startPleieforhold = pleieForholdEnStart,
                    opphoertPleieforhold = pleieForholdEnSlutt,
                    begrunnelse = "",
                ),
        )
        val behandlingEn = behandlingRepo.hentBehandling(behandlingSakEn.id) as Foerstegangsbehandling
        behandlingRepo.lagreStatus(behandlingEn.copy(status = BehandlingStatus.IVERKSATT))

        val behandlingSakTo =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak2.id,
            )
        behandlingRepo.opprettBehandling(behandlingSakTo)
        behandlingRepo.lagreTidligereFamiliepleier(
            behandlingSakTo.id,
            TidligereFamiliepleier(
                svar = true,
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                foedselsnummer = "pleid2",
                startPleieforhold = pleieForholdToStart,
                opphoertPleieforhold = pleieForholdToSlutt,
                begrunnelse = "",
            ),
        )
        val behandlingTo = behandlingRepo.hentBehandling(behandlingSakTo.id) as Foerstegangsbehandling
        behandlingRepo.lagreStatus(behandlingTo.copy(status = BehandlingStatus.IVERKSATT))

        sakLesDao.finnSakerMedPleieforholdOpphoerer(YearMonth.from(pleieForholdEnSlutt)) shouldBe listOf(sak1.id)
        sakLesDao.finnSakerMedPleieforholdOpphoerer(YearMonth.from(pleieForholdToSlutt)) shouldBe listOf(sak2.id)
        sakLesDao.finnSakerMedPleieforholdOpphoerer(YearMonth.from(pleieForholdEnStart)) shouldBe emptyList()
        sakLesDao.finnSakerMedPleieforholdOpphoerer(YearMonth.from(pleieForholdToStart)) shouldBe emptyList()
    }

    @Test
    fun `skal opprette sak med lagring av endring`() {
        val sak = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)

        val endringForSak = sakendringerDao.hentEndringerForSak(sak.id).single()

        with(endringForSak) {
            foer shouldBe null
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak.id)!!
            endringstype shouldBe Saksendring.Endringstype.OPPRETT_SAK
            identtype shouldBe Saksendring.Identtype.SAKSBEHANDLER
            ident shouldBe "Børre"
            assertTrue(tidspunkt.isAfter(Tidspunkt.now().minus(2, ChronoUnit.SECONDS)))
            assertTrue(tidspunkt.isBefore(Tidspunkt.now()))
        }
    }

    @Test
    fun `skal oppdatere adressebeskyttelse med lagring av endring`() {
        val sak = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val komplettSakFoer = sakendringerDao.hentKomplettSak(sak.id)!!

        sakSkrivDao.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.FORTROLIG)

        val endringForOppdatering =
            sakendringerDao
                .hentEndringerForSak(sak.id)
                .single { it.foer != null }

        with(endringForOppdatering) {
            foer?.id shouldBe sak.id
            etter.id shouldBe sak.id
            foer!! shouldBeEqual komplettSakFoer
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak.id)!!
            foer?.adressebeskyttelse shouldBe null
            etter.adressebeskyttelse shouldBe AdressebeskyttelseGradering.FORTROLIG
        }
    }

    @Test
    fun `skal oppdatere skjerming med lagring av endring`() {
        val sak = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val komplettSakFoer = sakendringerDao.hentKomplettSak(sak.id)!!

        sakSkrivDao.markerSakerMedSkjerming(listOf(sak.id), true)

        val endringForOppdatering =
            sakendringerDao
                .hentEndringerForSak(sak.id)
                .single { it.foer != null }

        with(endringForOppdatering) {
            foer?.id shouldBe sak.id
            etter.id shouldBe sak.id
            foer!! shouldBeEqual komplettSakFoer
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak.id)!!
            foer?.erSkjermet shouldBe null
            etter.erSkjermet shouldBe true
        }
    }

    @Test
    fun `skal oppdatere ident med lagring av endring`() {
        val sak = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val komplettSakFoer = sakendringerDao.hentKomplettSak(sak.id)!!

        sakSkrivDao.oppdaterIdent(sak.id, SOEKER2_FOEDSELSNUMMER)

        val endringForOppdatering =
            sakendringerDao
                .hentEndringerForSak(sak.id)
                .single { it.foer != null }

        with(endringForOppdatering) {
            foer?.id shouldBe sak.id
            etter.id shouldBe sak.id
            foer!! shouldBeEqual komplettSakFoer
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak.id)!!
            foer?.ident shouldBe SOEKER_FOEDSELSNUMMER.value
            etter.ident shouldBe SOEKER2_FOEDSELSNUMMER.value
        }
    }

    @Test
    fun `skal oppdatere enhet på flere saker med lagring av endring`() {
        val sak1 = sakSkrivDao.opprettSak(SOEKER_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)
        val sak2 = sakSkrivDao.opprettSak(SOEKER2_FOEDSELSNUMMER.value, SakType.OMSTILLINGSSTOENAD, Enheter.NORDLAND_BODOE.enhetNr)
        sakSkrivDao.oppdaterEnheterPaaSaker(
            listOf(
                SakMedEnhet(sak1.id, Enheter.AALESUND.enhetNr),
                SakMedEnhet(sak2.id, Enheter.KLAGE_VEST.enhetNr),
            ),
        )

        val endringForSak1 =
            sakendringerDao
                .hentEndringerForSak(sak1.id)
                .single { it.foer != null }
        with(endringForSak1) {
            foer?.enhet shouldBe Enheter.PORSGRUNN.enhetNr
            etter.enhet shouldBe Enheter.AALESUND.enhetNr
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak1.id)!!
        }
        val endringForSak2 =
            sakendringerDao
                .hentEndringerForSak(sak2.id)
                .single { it.foer != null }
        with(endringForSak2) {
            foer?.enhet shouldBe Enheter.NORDLAND_BODOE.enhetNr
            etter.enhet shouldBe Enheter.KLAGE_VEST.enhetNr
            etter shouldBeEqual sakendringerDao.hentKomplettSak(sak2.id)!!
        }
    }
}
