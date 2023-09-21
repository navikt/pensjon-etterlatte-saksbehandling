package behandling.klage

import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.FormkravMedBeslutter
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlageDaoImplTest {
    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var klageDao: KlageDaoImpl

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
        sakRepo = SakDao { connection }
        klageDao = KlageDaoImpl { connection }
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE klage""")
            .executeUpdate()
        dataSource.connection.prepareStatement("""TRUNCATE TABLE sak CASCADE """)
            .executeUpdate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `lagreKlage oppdaterer status og formkrav hvis klagen allerede eksisterer`() {
        val sak = sakRepo.opprettSak(fnr = "en bruker", type = SakType.BARNEPENSJON, enhet = "1337")
        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)

        val foersteHentedeKlage = klageDao.hentKlage(klage.id)
        Assertions.assertEquals(klage, foersteHentedeKlage)

        val formkrav =
            FormkravMedBeslutter(
                Formkrav(
                    vedtaketKlagenGjelder =
                        VedtaketKlagenGjelder(
                            id = "",
                            behandlingId = "",
                            datoAttestert = null,
                            vedtakType = null,
                        ),
                    erKlagerPartISaken = JaNei.JA,
                    erKlagenSignert = JaNei.JA,
                    gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                    erKlagenFramsattInnenFrist = JaNei.JA,
                    erFormkraveneOppfylt = JaNei.JA,
                ),
                Grunnlagsopplysning.Saksbehandler.create("en saksbehandler"),
            )

        val oppdatertKlage =
            klage.copy(
                status = KlageStatus.FORMKRAV_OPPFYLT,
                formkrav = formkrav,
            )
        klageDao.lagreKlage(oppdatertKlage)

        val hentetKlage = klageDao.hentKlage(oppdatertKlage.id)

        Assertions.assertEquals(KlageStatus.FORMKRAV_OPPFYLT, hentetKlage?.status)
        Assertions.assertEquals(formkrav, hentetKlage?.formkrav)
    }

    @Test
    fun `lagreKlage oppdaterer ikke opprettet tidspunkt eller saken hvis klagen allerede eksisterer`() {
        val sak = sakRepo.opprettSak(fnr = "en bruker", type = SakType.BARNEPENSJON, enhet = "1337")
        val sak2 = sakRepo.opprettSak(fnr = "en annen bruker", type = SakType.OMSTILLINGSSTOENAD, enhet = "3137")
        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)

        val foersteHentedeKlage = klageDao.hentKlage(klage.id)
        Assertions.assertEquals(klage, foersteHentedeKlage)

        val klageMedEndretSakOgOpprettet = klage.copy(sak = sak2, opprettet = klage.opprettet.plus(2, ChronoUnit.HOURS))
        klageDao.lagreKlage(klageMedEndretSakOgOpprettet)

        val andreHentedeKlage = klageDao.hentKlage(klageMedEndretSakOgOpprettet.id)
        Assertions.assertEquals(foersteHentedeKlage, andreHentedeKlage)
        Assertions.assertNotEquals(foersteHentedeKlage?.sak, klageMedEndretSakOgOpprettet.sak)
        Assertions.assertNotEquals(foersteHentedeKlage?.opprettet, klageMedEndretSakOgOpprettet.opprettet)
    }
}
