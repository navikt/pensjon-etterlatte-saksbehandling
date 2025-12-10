package no.nav.etterlatte.saksbehandler

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.azureAdSaksbehandlerClaim
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SaksbehandlerServiceImplTest(
    val dataSource: DataSource,
) {
    private lateinit var dao: SaksbehandlerInfoDao
    private val entraProxyKlient: EntraProxyKlient = mockk<EntraProxyKlient>()
    private val navansattKlient: NavAnsattKlient = mockk<NavAnsattKlient>()
    private lateinit var service: SaksbehandlerService
    private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

    @BeforeAll
    fun beforeAll() {
        dao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
        service = SaksbehandlerServiceImpl(dao, navansattKlient, entraProxyKlient)
        nyKontekstMedBrukerOgDatabase(user.also { every { it.name() } returns this::class.java.simpleName }, dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE saksbehandler_info CASCADE;").execute()
        }
        clearMocks(navansattKlient, entraProxyKlient)
    }

    @Test
    fun `Skal legge inn enheter og navn for ny ukjent saksbehandler`() {
        val nyidentSaksbehandler = "S12345"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        coEvery { entraProxyKlient.hentEnheterForIdent(nyidentSaksbehandler) } returns listOf(porsgrunn)
        coEvery {
            navansattKlient.hentSaksbehanderNavn(
                nyidentSaksbehandler,
            )
        } returns SaksbehandlerInfo(nyidentSaksbehandler, "navn navnesen")
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdSaksbehandlerClaim)),
                mapOf(AzureGroup.SAKSBEHANDLER to azureAdSaksbehandlerClaim),
            )
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.enheterMedLesetilgang(any()) } returns listOf(Enheter.defaultEnhet.enhetNr)
        every { user.enheterMedSkrivetilgang() } returns emptyList()
        every { user.kanSeOppgaveBenken() } returns true

        service.hentKomplettSaksbehandler(nyidentSaksbehandler)
        coVerify(exactly = 1) { entraProxyKlient.hentEnheterForIdent(nyidentSaksbehandler) }
        coVerify(exactly = 1) { navansattKlient.hentSaksbehanderNavn(nyidentSaksbehandler) }
        confirmVerified(entraProxyKlient, navansattKlient)
    }

    @Test
    fun `Skal ikke legge inn enheter og navn for kjent saksbehandler`() {
        val nyidentSaksbehandler = "S12345"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        coEvery { entraProxyKlient.hentEnheterForIdent(nyidentSaksbehandler) } returns listOf(porsgrunn)
        coEvery {
            navansattKlient.hentSaksbehanderNavn(
                nyidentSaksbehandler,
            )
        } returns SaksbehandlerInfo(nyidentSaksbehandler, "navn navnesen")
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdSaksbehandlerClaim)),
                mapOf(AzureGroup.SAKSBEHANDLER to azureAdSaksbehandlerClaim),
            )
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.enheterMedLesetilgang(any()) } returns listOf(Enheter.defaultEnhet.enhetNr)
        every { user.enheterMedSkrivetilgang() } returns emptyList()
        every { user.kanSeOppgaveBenken() } returns true

        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(nyidentSaksbehandler, "navn navnesen"))
        dao.upsertSaksbehandlerEnheter(Pair(nyidentSaksbehandler, listOf(porsgrunn)))
        service.hentKomplettSaksbehandler(nyidentSaksbehandler)
        coVerify(exactly = 0) { entraProxyKlient.hentEnheterForIdent(nyidentSaksbehandler) }
        coVerify(exactly = 0) { navansattKlient.hentSaksbehanderNavn(nyidentSaksbehandler) }
        confirmVerified(entraProxyKlient, navansattKlient)
    }

    @Test
    fun `Enheter finnes ikke i database skal hente fra EntraProxy`() {
        val ident = "ident"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        coEvery { entraProxyKlient.hentEnheterForIdent(ident) } returns listOf(porsgrunn)
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)
        hentetEnhetForSaksbehandler shouldBe listOf(porsgrunn)
        coVerify { entraProxyKlient.hentEnheterForIdent(ident) }
        confirmVerified(entraProxyKlient)
    }

    @Test
    fun `Ikke registrert enhet må kalle EntraProxy`() {
        val ident = "ident"
        val navMoldeEnhetsnr = Enhetsnummer("1502")
        val navMoldeNavn = "NAV Molde"
        val molde = SaksbehandlerEnhet(navMoldeEnhetsnr, navMoldeNavn)
        coEvery { entraProxyKlient.hentEnheterForIdent(ident) } returns listOf(molde)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Legitim gate"))
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)
        hentetEnhetForSaksbehandler shouldBe listOf(molde)

        coVerify(exactly = 1) { entraProxyKlient.hentEnheterForIdent(ident) }
        confirmVerified(entraProxyKlient)
    }

    @Test
    fun `Skal hente fra db, har navneinfo`() {
        val ident = "ident"
        val porsgrunn = SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn)
        val enheterForSaksbehandlerIDb = Pair(ident, listOf(porsgrunn))
        coEvery { entraProxyKlient.hentEnheterForIdent(ident) } returns listOf(porsgrunn)
        dao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, "Legitim gate"))
        dao.upsertSaksbehandlerEnheter(enheterForSaksbehandlerIDb)
        val hentetEnhetForSaksbehandler = service.hentEnheterForSaksbehandlerIdentWrapper(ident)

        hentetEnhetForSaksbehandler shouldBe listOf(porsgrunn)
        coVerify(exactly = 0) { entraProxyKlient.hentEnheterForIdent(ident) }
        confirmVerified(entraProxyKlient)
    }
}
