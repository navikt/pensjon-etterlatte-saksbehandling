package institusjonsopphold.institusjonsopphold

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdBegrunnelse
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdDao
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class InstitusjonsoppholdDaoTest {
    private val dataSource = DatabaseExtension.dataSource
    private lateinit var institusjonsoppholdDao: InstitusjonsoppholdDao

    @BeforeAll
    fun beforeAll() {
        val connection = dataSource.connection
        institusjonsoppholdDao = InstitusjonsoppholdDao { connection }
    }

    @Test
    fun `kan legge til vurdering av institusjonsoppholdshendelse`() {
        val sakId = 1L
        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("Z123123")
        val grunnlagshendelseId = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelse =
            InstitusjonsoppholdBegrunnelse(
                JaNeiMedBegrunnelse(JaNei.JA, "kommentaren"),
                JaNeiMedBegrunnelse(JaNei.NEI, "kommentarto"),
                grunnlagshendelseId,
            )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelse)
        val hentBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseId)
        Assertions.assertEquals(saksbehandler.ident, hentBegrunnelse?.saksbehandler?.ident)
        Assertions.assertEquals("kommentaren", hentBegrunnelse?.kanGiReduksjonAvYtelse?.begrunnelse)
        Assertions.assertEquals(JaNei.JA, hentBegrunnelse?.kanGiReduksjonAvYtelse?.svar)

        val grunnlagshendelseIdTo = UUID.randomUUID().toString()
        val institusjonsoppholdBegrunnelseNummerTo =
            InstitusjonsoppholdBegrunnelse(
                JaNeiMedBegrunnelse(JaNei.JA, "kommentaren"),
                JaNeiMedBegrunnelse(JaNei.NEI, "kommentarto"),
                grunnlagshendelseIdTo,
            )
        institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonsoppholdBegrunnelseNummerTo)
        val hentetBegrunnelseTo = institusjonsoppholdDao.hentBegrunnelse(grunnlagshendelseIdTo)
        Assertions.assertNotNull(hentetBegrunnelseTo)
        val skalIkkeFinnesBegrunnelse = institusjonsoppholdDao.hentBegrunnelse(UUID.randomUUID().toString())
        Assertions.assertNull(skalIkkeFinnesBegrunnelse)
    }
}
