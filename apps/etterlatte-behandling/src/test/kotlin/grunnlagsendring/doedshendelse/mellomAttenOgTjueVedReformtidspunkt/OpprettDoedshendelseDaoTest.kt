package grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt.OpprettDoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt.OpprettDoedshendelseStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class OpprettDoedshendelseDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var opprettDoedshendelseDao: OpprettDoedshendelseDao

    @BeforeAll
    fun setup() {
        opprettDoedshendelseDao = OpprettDoedshendelseDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE mellom_atten_og_tjue_ved_reformtidspunkt CASCADE;").execute()
        }
    }

    @Test
    fun `Skal hente liste over avdoede med status NY `() {
        val avdoedFnr = "12345678902"
        val avdoedFnr2 = "12345678903"
        val avdoedFnr3 = "12345678904"

        dataSource.connection.use {
            it
                .prepareStatement(
                    "INSERT INTO mellom_atten_og_tjue_ved_reformtidspunkt(fnr, opprettet, endret, status) VALUES('$avdoedFnr', NOW(), NOW(), 'NY')",
                ).executeUpdate()
            it
                .prepareStatement(
                    "INSERT INTO mellom_atten_og_tjue_ved_reformtidspunkt(fnr, opprettet, endret, status) VALUES('$avdoedFnr2', NOW(), NOW(), 'NY')",
                ).executeUpdate()
            it
                .prepareStatement(
                    "INSERT INTO mellom_atten_og_tjue_ved_reformtidspunkt(fnr, opprettet, endret, status) VALUES('$avdoedFnr3', NOW(), NOW(), 'OPPRETTET')",
                ).executeUpdate()
        }

        val avdoede = opprettDoedshendelseDao.hentAvdoede(listOf(OpprettDoedshendelseStatus.NY))

        avdoede shouldContain avdoedFnr
        avdoede shouldContain avdoedFnr2
        avdoede shouldNotContain avdoedFnr3
    }

    @Test
    fun `Skal oppdatere med ny status`() {
        val avdoedFnr = "12345678902"

        dataSource.connection.use {
            it
                .prepareStatement(
                    "INSERT INTO mellom_atten_og_tjue_ved_reformtidspunkt(fnr, opprettet, endret, status) VALUES('$avdoedFnr', NOW(), NOW(), 'NY')",
                ).executeUpdate()
        }

        opprettDoedshendelseDao.oppdater(avdoedFnr, OpprettDoedshendelseStatus.OPPRETTET)
        val avdoede = opprettDoedshendelseDao.hentAvdoede(listOf(OpprettDoedshendelseStatus.OPPRETTET))

        avdoede shouldContain avdoedFnr
    }
}
