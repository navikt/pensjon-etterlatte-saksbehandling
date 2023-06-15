package institusjonsopphold.institusjonsopphold

import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdBegrunnelse
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InstitusjonsoppholdDaoTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var dataSource: DataSource
    private lateinit var institusjonsoppholdDao: InstitusjonsoppholdDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }
        val connection = dataSource.connection
        institusjonsoppholdDao = InstitusjonsoppholdDao { connection }
    }

    @Test
    fun `kan legge til vurdering av institusjonsoppholdshendelse`() {
        val sakId = 1L
        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123123")
        val grunnlagshendelseId = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelse = InstitusjonsoppholdBegrunnelse(
            JaNei.JA,
            "kommentaren",
            JaNei.NEI,
            "kommentarto",
            grunnlagshendelseId
        )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelse)
        val hentBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseId)
        Assertions.assertEquals(saksbehandler.ident, hentBegrunnelse?.saksbehandler?.ident)
        Assertions.assertEquals("kommentaren", hentBegrunnelse?.kanGiReduksjonAvYtelseBegrunnelse)
        Assertions.assertEquals(JaNei.JA, hentBegrunnelse?.kanGiReduksjonAvYtelse)

        val grunnlagshendelseIdTo = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelseNummerTo = InstitusjonsoppholdBegrunnelse(
            JaNei.JA,
            "kommentaren",
            JaNei.NEI,
            "kommentarto",
            grunnlagshendelseIdTo
        )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelseNummerTo)
        val hentetBegrunnelseTo = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseIdTo)
        Assertions.assertNotNull(hentetBegrunnelseTo)
        val skalIkkeFinnesBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(UUID.randomUUID().toString())
        Assertions.assertNull(skalIkkeFinnesBegrunnelse)
    }
}