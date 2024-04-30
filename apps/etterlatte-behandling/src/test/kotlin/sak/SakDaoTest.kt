package no.nav.etterlatte.sak

import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.date.shouldBeToday
import io.kotest.matchers.date.shouldNotBeToday
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.opprettBehandling
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SakDaoTest(val dataSource: DataSource) {
    private lateinit var sakRepo: SakDao
    private lateinit var tilgangService: TilgangService
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var kommerBarnetTilGodeDao: KommerBarnetTilGodeDao

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
        dataSource.connection.prepareStatement(
            """
            UPDATE sak as s
            SET opprettet = (select behandling_opprettet from behandling as b where b.sak_id = s.id);
            """.trimIndent(),
        ).executeUpdate()

        val saker =
            dataSource.connection.prepareStatement(
                """
                select * from sak;    
                """.trimIndent(),
            ).executeQuery().toList {
                Pair(getString("fnr"), getTidspunktOrNull("opprettet"))
            }
        val sakmedTidligereDato = saker.find { it.first === fnrMedBehandling }

        sakmedTidligereDato?.second?.toLocalDate()?.shouldBeToday()

        dataSource.connection.prepareStatement(
            """
            UPDATE sak
            SET opprettet = 'yesterday'::TIMESTAMP
            WHERE sak.opprettet IS NULL;
            """.trimIndent(),
        ).executeUpdate()

        val beggesakerMedDato =
            dataSource.connection.prepareStatement(
                """
                select * from sak;    
                """.trimIndent(),
            ).executeQuery().toList {
                Pair(getString("fnr"), getTidspunktOrNull("opprettet"))
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
                GrunnlagsendringshendelseService.SakMedEnhet(it.id, Enheter.EGNE_ANSATTE.enhetNr)
            }

        sakRepo.oppdaterEnheterPaaSaker(sakerMedNyEnhet)

        val sakerMedEgenAnsattEnhet = sakRepo.finnSaker(fnr)
        sakerMedEgenAnsattEnhet.forEach {
            Assertions.assertEquals(Enheter.EGNE_ANSATTE.enhetNr, it.enhet)
        }
    }
}
