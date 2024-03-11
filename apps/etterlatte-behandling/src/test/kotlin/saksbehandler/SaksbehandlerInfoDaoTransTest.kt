package no.nav.etterlatte.saksbehandler

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SaksbehandlerInfoDaoTransTest(val dataSource: DataSource) {
    private lateinit var saksbehandlerInfoDaoTrans: SaksbehandlerInfoDao
    private lateinit var saksbehandlerInfoDao: SaksbehandlerInfoDao

    @BeforeAll
    fun beforeAll() {
        saksbehandlerInfoDaoTrans = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
        saksbehandlerInfoDao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `Finner saksbehandlere med riktig enhet`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.navn)),
        )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbaalesund.ident to listOf(SaksbehandlerEnhet(Enheter.AALESUND.enhetNr, Enheter.AALESUND.navn)),
        )

        val saksBehandlereMedAalesundEnhet = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.AALESUND.enhetNr)
        Assertions.assertEquals(1, saksBehandlereMedAalesundEnhet.size)
        Assertions.assertEquals(sbaalesund.ident, saksBehandlereMedAalesundEnhet[0].ident)

        val porsgrunn = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.PORSGRUNN.enhetNr)
        Assertions.assertEquals(1, porsgrunn.size)
        Assertions.assertEquals(sbporsgrunn.ident, porsgrunn[0].ident)

        val ingenSteinkjerSaksbehandlere = saksbehandlerInfoDaoTrans.hentSaksbehandlereForEnhet(Enheter.STEINKJER.enhetNr)
        Assertions.assertEquals(0, ingenSteinkjerSaksbehandlere.size)
    }

    @Test
    fun `Kan hente enheter for saksbehandlerident`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.navn)),
        )

        val enheterAalesundSaksbehandler =
            listOf(
                SaksbehandlerEnhet(Enheter.AALESUND.enhetNr, Enheter.AALESUND.navn),
                SaksbehandlerEnhet(Enheter.AALESUND_UTLAND.enhetNr, Enheter.AALESUND_UTLAND.navn),
            )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(sbaalesund.ident to enheterAalesundSaksbehandler)

        val hentetEnheterForAalesundSB = saksbehandlerInfoDaoTrans.hentSaksbehandlerEnheter(sbaalesund.ident)
        hentetEnheterForAalesundSB shouldContainExactly enheterAalesundSaksbehandler
    }
}
