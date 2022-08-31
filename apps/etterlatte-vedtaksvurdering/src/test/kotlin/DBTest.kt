package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultatType
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
            "12321423523545",
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, uuid),
            "",
            BeregningsResultat(
                UUID.randomUUID(),
                Beregningstyper.BPGP,
                Endringskode.NY,
                BeregningsResultatType.BEREGNET,
                beregningsperiode,
                now,
                0L
            )
        )
    }

    fun leggtilvilkaarsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()
        vedtaksvurderingService.lagreVilkaarsresultat(
            "12321423523545",
            "BARNEPENSJON",
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, uuid),
            "fnr",
            VilkaarResultat(
                VurderingsResultat.OPPFYLT,
                emptyList(),
                now
            ),
            LocalDate.now()
        )
    }

    fun leggtilkommersoekertilgoderesultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        val now = LocalDateTime.now()

        vedtaksvurderingService.lagreKommerSoekerTilgodeResultat(
            "12321423523545",
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, uuid),
            "fnr",
            KommerSoekerTilgode(
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
                        listOf(),
                        LocalDate.of(2020, 1, 1),
                        barn = listOf(Foedselsnummer.of("03108718357"))
                    ),
                    soeker = PersoninfoSoeker(
                        "Navn",
                        Foedselsnummer.of("03108718357"),
                        PersonRolle.BARN,
                        listOf(),
                        null,
                        LocalDate.of(2010, 1, 1)
                    ),
                    gjenlevendeForelder = PersoninfoGjenlevendeForelder(
                        "Navn",
                        Foedselsnummer.of("03108718357"),
                        PersonRolle.BARN,
                        listOf()
                    )
                )
            )
        )
    }

    fun leggtilavkortingsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        vedtaksvurderingService.lagreAvkorting(
            "12321423523545",
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, uuid),
            "fnr",
            AvkortingsResultat(
                UUID.randomUUID(),
                Beregningstyper.BPGP,
                no.nav.etterlatte.libs.common.avkorting.Endringskode.NY,
                AvkortingsResultatType.BEREGNET,
                emptyList(),
                LocalDateTime.now()
            )
        )
    }

    fun lagreIverksattVedtak() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        vedtaksvurderingService.lagreIverksattVedtak(uuid)
    }

    @Test
    fun testDB() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)
        leggtilavkortingsresultat()
        val avkortet = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertEquals(VedtakStatus.AVKORTET, avkortet?.vedtakStatus)
        leggtilvilkaarsresultat()
        val vilkaarsvurdert = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertEquals(VedtakStatus.VILKAARSVURDERT, vilkaarsvurdert?.vedtakStatus)
        leggtilkommersoekertilgoderesultat()
        leggtilberegningsresultat()
        val beregnet = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertEquals(VedtakStatus.BEREGNET, beregnet?.vedtakStatus)

        val vedtaket = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        assert(vedtaket?.beregningsResultat != null)
        assert(vedtaket?.avkortingsResultat != null)
        assert(vedtaket?.beregningsResultat?.grunnlagVersjon == 0.toLong())
        assert(vedtaket?.vilkaarsResultat != null)
        assert(vedtaket?.kommerSoekerTilgodeResultat != null)
        assert(vedtaket?.vedtakStatus != null)
        Assertions.assertNotNull(vedtaket?.virkningsDato)

        vedtaksvurderingService.fattVedtak(uuid, "saksbehandler")
        val fattetVedtak = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertTrue(fattetVedtak?.vedtakFattet!!)
        Assertions.assertEquals(VedtakStatus.FATTET_VEDTAK, fattetVedtak.vedtakStatus)

        vedtaksvurderingService.underkjennVedtak(uuid)
        val underkjentVedtak = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertEquals(VedtakStatus.RETURNERT, underkjentVedtak?.vedtakStatus)

        vedtaksvurderingService.fattVedtak(uuid, "saksbehandler")

        vedtaksvurderingService.attesterVedtak(uuid, "attestant")
        val attestertVedtak = vedtaksvurderingService.hentVedtak("12321423523545", uuid)
        Assertions.assertNotNull(attestertVedtak?.attestant)
        Assertions.assertNotNull(attestertVedtak?.datoattestert)
        Assertions.assertNotNull(attestertVedtak?.virkningsDato)
        Assertions.assertEquals(VedtakStatus.ATTESTERT, attestertVedtak?.vedtakStatus)

        lagreIverksattVedtak()
        val iverksattVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.IVERKSATT, iverksattVedtak?.vedtakStatus)

        vedtaksvurderingService.slettSak(12321423523545)
        Assertions.assertNull(vedtaksvurderingService.hentVedtak("12321423523545", uuid))
    }

    @Test
    fun `kan lagre og hente alle vedtak`() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()

        lagreNyttVilkaarsresultat(vedtaksvurderingService, "1", uuid1)
        lagreNyttVilkaarsresultat(vedtaksvurderingService, "2", uuid2)
        lagreNyttVilkaarsresultat(vedtaksvurderingService, "3", uuid3)

        Assertions.assertEquals(3, vedtaksvurderingService.hentVedtakBolk(listOf(uuid1, uuid2, uuid3)).size)
    }

    private fun lagreNyttVilkaarsresultat(service: VedtaksvurderingService, sakId: String, behandlingId: UUID) {
        service.lagreVilkaarsresultat(
            sakId,
            "BARNEPENSJON",
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            "fnr",
            VilkaarResultat(
                VurderingsResultat.OPPFYLT,
                emptyList(),
                LocalDateTime.now()
            ),
            LocalDate.now()
        )
    }
}