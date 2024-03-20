package no.nav.etterlatte.saksbehandler

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SaksbehandlerServiceImplTest(val dataSource: DataSource) {
    private lateinit var dao: SaksbehandlerInfoDao
    private val axsysKlient: AxsysKlient = mockk<AxsysKlient>()
    private lateinit var service: SaksbehandlerService
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeAll
    fun beforeAll() {
        dao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
        service = SaksbehandlerServiceImpl(dao, axsysKlient)
        nyKontekstMedBrukerOgDatabase(user, dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE saksbehandler_info CASCADE;").execute()
        }
    }

    @Test
    fun `Enheter finnes ikke i database skal hente fra axsys`() {
        val ident = "ident"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        coEvery { axsysKlient.hentEnheterForIdent(ident) } returns listOf(porsgrunn)
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)
        hentetEnhetForSaksbehandler shouldBe listOf(porsgrunn)
        coVerify { axsysKlient.hentEnheterForIdent(ident) }
        confirmVerified(axsysKlient)
    }

    @Test
    fun `Ikke registrert enhet må kalle axsys`() {
        val ident = "ident"
        val navMoldeEnhetsnr = "1502"
        val navMoldeNavn = "NAV Molde"
        val molde = SaksbehandlerEnhet(navMoldeEnhetsnr, navMoldeNavn)
        coEvery { axsysKlient.hentEnheterForIdent(ident) } returns listOf(molde)
        val enheterForSaksbehandlerIDb = Pair(ident, listOf(molde))
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Legitim gate"))
        dao.upsertSaksbehandlerEnheter(enheterForSaksbehandlerIDb)
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)
        hentetEnhetForSaksbehandler shouldBe listOf(molde)

        coVerify { axsysKlient.hentEnheterForIdent(ident) }
        confirmVerified(axsysKlient)
    }

    @Test
    fun `Skal hente fra db, har navneinfo`() {
        val ident = "ident"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        val enheterForSaksbehandlerIDb = Pair(ident, listOf(porsgrunn))
        coEvery { axsysKlient.hentEnheterForIdent(ident) } returns listOf(porsgrunn)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Legitim gate"))
        dao.upsertSaksbehandlerEnheter(enheterForSaksbehandlerIDb)
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)

        hentetEnhetForSaksbehandler shouldBe listOf(porsgrunn)
        coVerify(exactly = 0) { axsysKlient.hentEnheterForIdent(ident) }
        confirmVerified(axsysKlient)
    }
}
