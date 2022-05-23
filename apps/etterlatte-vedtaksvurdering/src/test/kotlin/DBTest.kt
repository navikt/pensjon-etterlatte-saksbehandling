package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DBTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource

    private val uuid = UUID.randomUUID()

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    fun leggtilberegningsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

        val now = LocalDateTime.now()
        val beregningsperiode = listOf<Beregningsperiode>()
        vedtaksvurderingService.lagreBeregningsresultat("12321423523545", uuid, BeregningsResultat(
            UUID.randomUUID(),
            Beregningstyper.BPGP,
            Endringskode.NY,
            BeregningsResultatType.BEREGNET,
            beregningsperiode,
            now
        ))
    }

    fun leggtilvilkaarsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()
        vedtaksvurderingService.lagreVilkaarsresultat("12321423523545", uuid, VilkaarResultat(
            VurderingsResultat.OPPFYLT,
            emptyList(),
            now
        ))
    }

    fun leggtilkommersoekertilgoderesultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()
        vedtaksvurderingService.lagreKommerSoekerTilgodeResultat("12321423523545", uuid, VilkaarResultat(
            VurderingsResultat.OPPFYLT,
            emptyList(),
            now
        ))
    }

    fun leggtilavkortingsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        vedtaksvurderingService.lagreAvkorting("12321423523545", uuid, objectMapper.writeValueAsString("Test"))
    }

    @Test
    fun testDB() {
        leggtilavkortingsresultat()
        leggtilvilkaarsresultat()
        leggtilkommersoekertilgoderesultat()
        leggtilberegningsresultat()

        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val vedtaket = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        assert(vedtaket?.beregningsResultat != null)
        assert(vedtaket?.avkortingsResultat != null)
        assert(vedtaket?.vilkaarsResultat != null)
        assert(vedtaket?.kommerSoekerTilgodeResultat != null)
    }


}
