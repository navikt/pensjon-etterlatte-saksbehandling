package no.nav.etterlatte.behandling.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilbakekrevingDaoTest {
    private lateinit var dataSource: DataSource
    private lateinit var sakDao: SakDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao

    private lateinit var sak: Sak

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    @BeforeAll
    fun setup() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        val connection = dataSource.connection
        sakDao = SakDao { connection }
        tilbakekrevingDao = TilbakekrevingDao { connection }
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE tilbakekreving""")
            .executeUpdate()
        dataSource.connection.prepareStatement("""TRUNCATE TABLE sak CASCADE """)
            .executeUpdate()
        sak = sakDao.opprettSak(fnr = "en bruker", type = SakType.OMSTILLINGSSTOENAD, enhet = "1337")
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Lagre ny tilbakekreving`() {
        val ny = tilbakekreving(sak)
        val lagret = tilbakekrevingDao.lagreTilbakekreving(ny)
        lagret shouldBe ny
    }

    @Test
    fun `Lagre eksisterende tilbakekreving`() {
        val ny = tilbakekreving(sak)
        val lagret = tilbakekrevingDao.lagreTilbakekreving(ny)
        val oppdatert = lagret.copy(status = TilbakekrevingStatus.UNDER_ARBEID)
        val lagretOppdatert = tilbakekrevingDao.lagreTilbakekreving(oppdatert)
        lagretOppdatert shouldBe oppdatert
    }

    companion object {
        private fun tilbakekreving(sak: Sak) =
            Tilbakekreving.ny(
                sak = sak,
                kravgrunnlag =
                    Kravgrunnlag(
                        kravgrunnlagId = KravgrunnlagId(123L),
                        sakId = SakId(474L),
                        vedtakId = VedtakId(2L),
                        kontrollFelt = Kontrollfelt(""),
                        status = KravgrunnlagStatus.ANNU,
                        saksbehandler = NavIdent(""),
                        sisteUtbetalingslinjeId = UUID30(""),
                        perioder =
                            listOf(
                                KravgrunnlagPeriode(
                                    periode =
                                        Periode(
                                            fraOgMed = YearMonth.of(2023, 1),
                                            tilOgMed = YearMonth.of(2023, 2),
                                        ),
                                    skatt = BigDecimal(200),
                                    grunnlagsbeloep =
                                        listOf(
                                            Grunnlagsbeloep(
                                                kode = KlasseKode(""),
                                                type = KlasseType.YTEL,
                                                bruttoUtbetaling = BigDecimal(1000),
                                                nyBruttoUtbetaling = BigDecimal(1200),
                                                bruttoTilbakekreving = BigDecimal(200),
                                                beloepSkalIkkeTilbakekreves = BigDecimal(200),
                                                skatteProsent = BigDecimal(20),
                                                resultat = null,
                                                skyld = null,
                                                aarsak = null,
                                            ),
                                            Grunnlagsbeloep(
                                                kode = KlasseKode(""),
                                                type = KlasseType.FEIL,
                                                bruttoUtbetaling = BigDecimal(0),
                                                nyBruttoUtbetaling = BigDecimal(0),
                                                bruttoTilbakekreving = BigDecimal(0),
                                                beloepSkalIkkeTilbakekreves = BigDecimal(0),
                                                skatteProsent = BigDecimal(0),
                                                resultat = null,
                                                skyld = null,
                                                aarsak = null,
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )
    }
}
