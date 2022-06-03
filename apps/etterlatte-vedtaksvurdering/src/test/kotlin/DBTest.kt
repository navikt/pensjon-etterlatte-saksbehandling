package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultatType
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.etterlatte.libs.common.vikaar.PersoninfoGjenlevendeForelder
import no.nav.etterlatte.libs.common.vikaar.PersoninfoSoeker
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
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
        vedtaksvurderingService.lagreBeregningsresultat(
            "12321423523545", uuid, "", BeregningsResultat(
                UUID.randomUUID(),
                Beregningstyper.BPGP,
                Endringskode.NY,
                BeregningsResultatType.BEREGNET,
                beregningsperiode,
                now
            )
        )
    }

    fun leggtilvilkaarsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()
        vedtaksvurderingService.lagreVilkaarsresultat("12321423523545", uuid, "fnr", VilkaarResultat(
            VurderingsResultat.OPPFYLT,
            emptyList(),
            now
        ), LocalDate.now())
    }

    fun leggtilkommersoekertilgoderesultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()

        vedtaksvurderingService.lagreKommerSoekerTilgodeResultat(
            "12321423523545", uuid, "fnr", KommerSoekerTilgode(
                VilkaarResultat(
                    VurderingsResultat.OPPFYLT,
                    emptyList(),
                    now
                ),

                Familiemedlemmer(
                    avdoed = PersoninfoAvdoed(
                        "Navn",
                        Foedselsnummer.of("03108718357"),
                        PersonRolle.AVDOED,
                        Adresser(null, null, null),
                        LocalDate.of(2020, 1, 1),
                    ),
                    soeker = PersoninfoSoeker(
                        "Navn",
                        Foedselsnummer.of("03108718357"),
                        PersonRolle.BARN,
                        Adresser(null, null, null),
                        LocalDate.of(2010, 1, 1),
                    ),
                    gjenlevendeForelder = PersoninfoGjenlevendeForelder(
                        "Navn",
                        Foedselsnummer.of("03108718357"),
                        PersonRolle.BARN,
                        Adresser(null, null, null),
                        "Oppgitt adresse i søknad",
                    )
                )

            )

        )

    }

    fun leggtilavkortingsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        vedtaksvurderingService.lagreAvkorting("12321423523545", uuid, "fnr", AvkortingsResultat(UUID.randomUUID(), Beregningstyper.BPGP, no.nav.etterlatte.libs.common.avkorting.Endringskode.NY, AvkortingsResultatType.BEREGNET, emptyList(), LocalDateTime.now()))
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
        assert(vedtaket?.beregningsResultat?.grunnlagVerson == 0.toLong())
        assert(vedtaket?.vilkaarsResultat != null)
        assert(vedtaket?.kommerSoekerTilgodeResultat != null)
        Assertions.assertNotNull(vedtaket?.virkningsDato)


        vedtaksvurderingService.fattVedtak("12321423523545", uuid, "saksbehandler")
        val fattetVedtak = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertTrue(fattetVedtak?.vedtakFattet!!)


        vedtaksvurderingService.attesterVedtak("12321423523545", uuid, "attestant")
        val attestertVedtak = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertNotNull(attestertVedtak?.attestant)
        Assertions.assertNotNull(attestertVedtak?.datoattestert)
        Assertions.assertNotNull(attestertVedtak?.virkningsDato)


        val fulltvedtak = vedtaksvurderingService.hentFellesVedtak("12321423523545", uuid)
        Assertions.assertNotNull(fulltvedtak)
    }
}
