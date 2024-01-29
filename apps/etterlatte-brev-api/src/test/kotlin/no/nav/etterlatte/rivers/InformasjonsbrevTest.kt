package no.nav.etterlatte.rivers

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rivers.StartBrevgenereringRepository.Databasetabell
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.FNR_KEY
import rapidsandrivers.SAK_ID_KEY
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InformasjonsbrevTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var db: BrevRepository
    private lateinit var dataSource: DataSource

    private val behandlingId = UUID.randomUUID()

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            )
        dataSource.migrate()

        db = BrevRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `starter generering av informasjonsbrev, og verifiserer at brevet faktisk blir oppretta og distribuert`() {
        val brevkode = EtterlatteBrevKode.UTSATT_KLAGEFRIST
        val saktype = SakType.BARNEPENSJON
        lagreBrevTilGenereringIDatabasen(brevkode, saktype)
        val testRapid =
            TestRapid().apply {
                StartInformasjonsbrevgenereringRiver(
                    StartBrevgenereringRepository(dataSource),
                    this,
                    sleep = { _ -> run {} },
                ) { it() }.also { it.init() }
            }

        val brevId: Long = 2
        val saksnr = 1L
        val brevoppretter =
            mockk<Brevoppretter>().also {
                coEvery {
                    it.opprettBrev(
                        saksnr,
                        any(),
                        any(),
                        any(),
                    )
                } returns Pair(mockk<Brev>().also { every { it.id } returns brevId }, mockk())
            }
        val pdfGenerator =
            mockk<PDFGenerator>().also {
                coEvery {
                    it.ferdigstillOgGenererPDF(
                        brevId,
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns mockk()
            }
        val journalfoerBrevService =
            mockk<JournalfoerBrevService>().also {
                coEvery { it.journalfoer(brevId, any()) } returns ""
            }
        val brevdistribuerer = mockk<Brevdistribuerer>().also { every { it.distribuer(brevId) } returns "" }
        OpprettJournalfoerOgDistribuerRiver(
            testRapid,
            brevoppretter,
            pdfGenerator,
            journalfoerBrevService,
            brevdistribuerer,
        )

        assertEquals(2, testRapid.inspektør.size)
        with(testRapid.inspektør.message(0)) {
            val fnr = get(FNR_KEY).also { assertEquals(SOEKER_FOEDSELSNUMMER.value, it.asText()) }
            val brevmal = get(BREVMAL_RIVER_KEY).also { assertEquals(brevkode.name, it.asText()) }
            val sakstype = get(SAK_TYPE_KEY).also { assertEquals(saktype.name, it.asText()) }
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(),
                    FNR_KEY to fnr,
                    BREVMAL_RIVER_KEY to brevmal,
                    SAK_TYPE_KEY to sakstype,
                    SAK_ID_KEY to saksnr,
                ),
            ).also { testRapid.sendTestMessage(it.toJson()) }
        }
        with(testRapid.inspektør.message(1)) {
            val behandling = get(BEHANDLING_ID_KEY).also { assertEquals(behandlingId.toString(), it.asText()) }
            val brevmal = get(BREVMAL_RIVER_KEY).also { assertEquals(brevkode.name, it.asText()) }
            val sakstype = get(SAK_TYPE_KEY).also { assertEquals(saktype.name, it.asText()) }
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER.lagEventnameForType(),
                    BEHANDLING_ID_KEY to behandling,
                    BREVMAL_RIVER_KEY to brevmal,
                    SAK_TYPE_KEY to sakstype,
                    SAK_ID_KEY to saksnr,
                ),
            ).also { testRapid.sendTestMessage(it.toJson()) }
        }
        verify { brevdistribuerer.distribuer(brevId) }
    }

    private fun lagreBrevTilGenereringIDatabasen(
        brevkode: EtterlatteBrevKode,
        saktype: SakType,
    ) {
        dataSource.transaction { tx ->
            queryOf(
                "INSERT INTO ${Databasetabell.TABELLNAVN} " +
                    "(${Databasetabell.FNR}, ${Databasetabell.BREVMAL}, ${Databasetabell.SAKTYPE}) " +
                    "VALUES" +
                    "('${SOEKER_FOEDSELSNUMMER.value}', '${brevkode.name}', '${saktype.name}');",
            )
                .let { query -> tx.run(query.asUpdate) }
        }
        dataSource.transaction { tx ->
            queryOf(
                "INSERT INTO ${Databasetabell.TABELLNAVN} " +
                    "(${Databasetabell.BEHANDLING_ID}, ${Databasetabell.BREVMAL}, ${Databasetabell.SAKTYPE}) " +
                    "VALUES" +
                    "('$behandlingId', '${brevkode.name}', '${saktype.name}');",
            )
                .let { query -> tx.run(query.asUpdate) }
        }
    }
}
