package no.nav.etterlatte.sak

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.date.shouldBeToday
import io.kotest.matchers.date.shouldNotBeToday
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.omregning.OmregningDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.opprettBehandling
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SakDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakRepo: SakDao
    private lateinit var tilgangService: TilgangService
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var kommerBarnetTilGodeDao: KommerBarnetTilGodeDao

    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @BeforeAll
    fun beforeAll() {
        sakRepo = SakDao(ConnectionAutoclosingTest(dataSource))
        tilgangService = TilgangServiceImpl(SakTilgangDao(dataSource))
        kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource))
        behandlingRepo =
            BehandlingDao(
                kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                ConnectionAutoclosingTest(dataSource),
            )
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `klarer å sette opprett for saker der de har det på behandlingen`() {
        val fnrMedBehandling = "1231234"
        val opprettSak = sakRepo.opprettSak(fnrMedBehandling, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val fnrUtenbehandling = "123124124"
        val sakutenopprettet = sakRepo.opprettSak(fnrUtenbehandling, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
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
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(Enheter.PORSGRUNN.enhetNr, opprettSak.enhet)
    }

    @Test
    fun `kan lagre og hente flyktning`() {
        val flyktning = Flyktning(true, LocalDate.of(2024, 1, 1), "Migrert", Grunnlagsopplysning.Pesys.create())
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        sakRepo.oppdaterFlyktning(opprettSak.id, flyktning)
        val oppdatertFlyktning = sakRepo.finnFlyktningForSak(opprettSak.id)

        Assertions.assertEquals(flyktning, oppdatertFlyktning)
    }

    @Test
    fun `Returnerer null dersom flyktning ikke finnes`() {
        val opprettSak = sakRepo.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        Assertions.assertEquals(sakRepo.finnFlyktningForSak(opprettSak.id), null)
    }

    @Test
    fun `hentSakerMedIder henter kun de sakene med innsendt id`() {
        val sak1 = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val sak2 = sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val sak3 = sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val alleSaker = listOf(sak1, sak2, sak3)

        val alleIder = alleSaker.map { it.id }
        val hentetAlleSaker = sakRepo.hentSakerMedIder(alleIder)
        val hentetKunSak1 = sakRepo.hentSakerMedIder(listOf(sak1.id))
        val hentingIngenSaker = sakRepo.hentSakerMedIder(emptyList())
        val hentingUkjentSak = sakRepo.hentSakerMedIder(listOf(alleIder.sum()))

        Assertions.assertEquals(alleSaker, hentetAlleSaker)
        Assertions.assertEquals(listOf(sak1), hentetKunSak1)
        Assertions.assertEquals(emptyList<Sak>(), hentingIngenSaker)
        Assertions.assertEquals(emptyList<Sak>(), hentingUkjentSak)
    }

    @Test
    fun `Skal kunne oppdatere enhet`() {
        val fnr = "fnr"
        val sak = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        val funnetSaker = sakRepo.finnSaker(fnr)
        Assertions.assertEquals(1, funnetSaker.size)
        Assertions.assertEquals(sak.id, funnetSaker[0].id)
        sakRepo.opprettSak(fnr, SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr).also {
            Assertions.assertNotNull(it)
        }
        val funnetSakermed2saker = sakRepo.finnSaker(fnr)
        Assertions.assertEquals(2, funnetSakermed2saker.size)

        val sakerMedNyEnhet =
            funnetSakermed2saker.map {
                SakMedEnhet(it.id, Enheter.EGNE_ANSATTE.enhetNr)
            }

        sakRepo.oppdaterEnheterPaaSaker(sakerMedNyEnhet)

        val sakerMedEgenAnsattEnhet = sakRepo.finnSaker(fnr)
        sakerMedEgenAnsattEnhet.forEach {
            Assertions.assertEquals(Enheter.EGNE_ANSATTE.enhetNr, it.enhet)
        }
    }

    @Nested
    inner class HentAlleSaker {
        @Test
        fun `Skal hente angitte saker`() {
            val sak1 = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            val saker = sakRepo.hentSaker("", 2, listOf(sak2.id, sak3.id), emptyList())

            saker.size shouldBe 2
            saker.forEach { it.id shouldNotBe sak1.id }
        }

        @Test
        fun `Skal utelate ekskluderte saker`() {
            val sak1 = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak4 = sakRepo.opprettSak("fnr4", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            val saker = sakRepo.hentSaker("", 4, emptyList(), ekskluderteSaker = listOf(sak1.id, sak2.id))

            saker.size shouldBe 2
            saker shouldContain sak3
            saker shouldContain sak4
        }

        @Test
        fun `Skal kun returnere spesifikke saker som ikke er ekskludert`() {
            val sak1 = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak2 = sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak3 = sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            val sak4 = sakRepo.opprettSak("fnr4", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            val saker =
                sakRepo.hentSaker(
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
            sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

            val saker = sakRepo.hentSaker("", 3, emptyList(), emptyList())

            saker.size shouldBe 3
        }

        @Test
        fun `Hvis kjoering er starta, skal vi ikke hente ut`() {
            val sakid = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))

            val saker = sakRepo.hentSaker("K1", 1, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er starta, og saa feila det, skal vi hente ut`() {
            val sakid = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FEILA, sakid))

            val saker = sakRepo.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 1
        }

        @Test
        fun `Hvis kjoering er starta, og saa ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakRepo.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er starta, og saa feila, og saa ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FEILA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.STARTA, sakid))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakRepo.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Hvis kjoering er ferdigstilt, skal vi ikke hente ut`() {
            val sakid = sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr).id
            val omregningDao = OmregningDao(ConnectionAutoclosingTest(dataSource))
            omregningDao.oppdaterKjoering(KjoeringRequest("K1", KjoeringStatus.FERDIGSTILT, sakid))

            val saker = sakRepo.hentSaker("K1", 3, emptyList(), emptyList())

            saker.size shouldBe 0
        }

        @Test
        fun `Skal hente saker for gitt sakstype hvis sakstype er angitt`() {
            sakRepo.opprettSak("fnr1", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakRepo.opprettSak("fnr2", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakRepo.opprettSak("fnr3", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
            sakRepo.opprettSak("fnr4", SakType.OMSTILLINGSSTOENAD, Enheter.PORSGRUNN.enhetNr)

            sakRepo
                .hentSaker("", 100, emptyList(), emptyList(), SakType.BARNEPENSJON)
                .map { it.ident } shouldContainExactlyInAnyOrder listOf("fnr1", "fnr2", "fnr3")

            sakRepo
                .hentSaker("", 100, emptyList(), emptyList(), SakType.OMSTILLINGSSTOENAD)
                .map { it.ident } shouldBe listOf("fnr4")

            val saker = sakRepo.hentSaker("", 2, emptyList(), emptyList(), SakType.BARNEPENSJON)
            saker.map { it.ident } shouldContainAnyOf listOf("fnr1", "fnr2", "fnr3")
            saker.size shouldBe 2
        }
    }
}
