package no.nav.etterlatte.institusjonsopphold

import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class InstitusjonsoppholdDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var institusjonsoppholdDao: InstitusjonsoppholdDao

    @BeforeAll
    fun beforeAll() {
        institusjonsoppholdDao = InstitusjonsoppholdDao(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `kan legge til vurdering av institusjonsoppholdshendelse`() {
        val sakId = sakId1
        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123123")
        val grunnlagshendelseId = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelse =
            InstitusjonsoppholdBegrunnelse(
                JaNei.JA,
                "kommentaren",
                JaNei.NEI,
                "kommentarto",
                grunnlagshendelseId,
            )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelse)
        val hentBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseId)
        Assertions.assertEquals(saksbehandler.ident, hentBegrunnelse?.saksbehandler?.ident)
        Assertions.assertEquals("kommentaren", hentBegrunnelse?.kanGiReduksjonAvYtelseBegrunnelse)
        Assertions.assertEquals(JaNei.JA, hentBegrunnelse?.kanGiReduksjonAvYtelse)

        val grunnlagshendelseIdTo = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelseNummerTo =
            InstitusjonsoppholdBegrunnelse(
                JaNei.JA,
                "kommentaren",
                JaNei.NEI,
                "kommentarto",
                grunnlagshendelseIdTo,
            )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelseNummerTo)
        val hentetBegrunnelseTo = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseIdTo)
        Assertions.assertNotNull(hentetBegrunnelseTo)
        val skalIkkeFinnesBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(UUID.randomUUID().toString())
        Assertions.assertNull(skalIkkeFinnesBegrunnelse)
    }
}
