package no.nav.etterlatte.saksbehandler

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SaksbehandlerInfoDaoTransTest {
    private val dataSource = DatabaseExtension.dataSource
    private lateinit var saksbehandlerInfoDaoTrans: SaksbehandlerInfoDaoTrans
    private lateinit var saksbehandlerInfoDao: SaksbehandlerInfoDao

    @BeforeAll
    fun beforeAll() {
        val connection = dataSource.connection
        saksbehandlerInfoDaoTrans = SaksbehandlerInfoDaoTrans { connection }
        saksbehandlerInfoDao = SaksbehandlerInfoDao(dataSource)
    }

    @Test
    fun `Finner saksbehandlere med riktig enhet`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(sbporsgrunn.ident to listOf(Enheter.PORSGRUNN.enhetNr))
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(sbaalesund.ident to listOf(Enheter.AALESUND.enhetNr))

        val saksBehandlereMedAalesundEnhet = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.AALESUND.enhetNr)
        Assertions.assertEquals(1, saksBehandlereMedAalesundEnhet.size)
        Assertions.assertEquals(sbaalesund.ident, saksBehandlereMedAalesundEnhet[0])

        val porsgrunn = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.PORSGRUNN.enhetNr)
        Assertions.assertEquals(1, porsgrunn.size)
        Assertions.assertEquals(sbporsgrunn.ident, porsgrunn[0])

        val ingenSteinkjerSaksbehandlere = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.STEINKJER.enhetNr)
        Assertions.assertEquals(0, ingenSteinkjerSaksbehandlere.size)
    }
}
