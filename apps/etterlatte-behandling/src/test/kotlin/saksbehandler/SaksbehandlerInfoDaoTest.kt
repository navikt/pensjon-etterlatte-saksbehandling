package no.nav.etterlatte.saksbehandler

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enhet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SaksbehandlerInfoDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var saksbehandlerInfoDao: SaksbehandlerInfoDao

    @BeforeAll
    fun beforeAll() {
        saksbehandlerInfoDao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `Kan hente navn for saksbehandler`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        val saksbehandlerNavn = saksbehandlerInfoDao.hentSaksbehandlerNavn(sbporsgrunn.ident)
        saksbehandlerNavn shouldBe sbporsgrunn.navn
    }

    @Test
    fun `Kan hente alle saksbehandlere sine identer`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)

        val sbaalesundfinnes = saksbehandlerInfoDao.saksbehandlerFinnes(sbaalesund.ident)
        sbaalesundfinnes shouldBe true

        val sbporsgrunnfinnes = saksbehandlerInfoDao.saksbehandlerFinnes(sbporsgrunn.ident)
        sbporsgrunnfinnes shouldBe true

        val randomidentfinnesikke = saksbehandlerInfoDao.saksbehandlerFinnes("ident")
        randomidentfinnesikke shouldBe false
    }

    @Test
    fun `Kan hente alle saksbehandlere sine identer og navn`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn2")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)

        val allesaksbehandlere = saksbehandlerInfoDao.hentAlleSaksbehandlere()
        allesaksbehandlere.size shouldBe 2
        allesaksbehandlere shouldContain sbporsgrunn
        allesaksbehandlere shouldContain sbaalesund
    }

    @Test
    fun `Finner saksbehandlere med riktig enhet`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enhet.PORSGRUNN.enhetNr, Enhet.PORSGRUNN.navn)),
        )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbaalesund.ident to listOf(SaksbehandlerEnhet(Enhet.AALESUND.enhetNr, Enhet.AALESUND.navn)),
        )

        val saksBehandlereMedAalesundEnhet = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enhet.AALESUND)
        Assertions.assertEquals(1, saksBehandlereMedAalesundEnhet.size)
        Assertions.assertEquals(sbaalesund.ident, saksBehandlereMedAalesundEnhet[0].ident)

        val porsgrunn = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enhet.PORSGRUNN)
        Assertions.assertEquals(1, porsgrunn.size)
        Assertions.assertEquals(sbporsgrunn.ident, porsgrunn[0].ident)

        val ingenSteinkjerSaksbehandlere = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enhet.STEINKJER)
        Assertions.assertEquals(0, ingenSteinkjerSaksbehandlere.size)
    }

    @Test
    fun `Kan hente enheter for saksbehandlerident`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enhet.PORSGRUNN.enhetNr, Enhet.PORSGRUNN.navn)),
        )

        val enheterAalesundSaksbehandler =
            listOf(
                SaksbehandlerEnhet(Enhet.AALESUND.enhetNr, Enhet.AALESUND.navn),
                SaksbehandlerEnhet(Enhet.AALESUND_UTLAND.enhetNr, Enhet.AALESUND_UTLAND.navn),
            )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(sbaalesund.ident to enheterAalesundSaksbehandler)

        val hentetEnheterForAalesundSB = saksbehandlerInfoDao.hentSaksbehandlerEnheter(sbaalesund.ident)
        hentetEnheterForAalesundSB shouldContainExactly enheterAalesundSaksbehandler
    }
}
