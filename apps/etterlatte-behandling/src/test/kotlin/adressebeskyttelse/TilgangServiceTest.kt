package no.nav.etterlatte.adressebeskyttelse

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.etterlatte.SaksbehandlerMedRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.config.AzureGroup
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilgangServiceTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var tilgangService: TilgangService
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val fortroligDev = "ea930b6b-9397-44d9-b9e6-f4cf527a632a"
    private val egenAnsattDev = "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d"

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

        tilgangService = TilgangServiceImpl(
            SakTilgangDao(dataSource),
            mapOf(
                AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev,
                AzureGroup.FORTROLIG to fortroligDev,
                AzureGroup.EGEN_ANSATT to egenAnsattDev
            )
        )
        sakRepo = SakDao { dataSource.connection }
        behandlingRepo = BehandlingDao { dataSource.connection }
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse på sak`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val saksbehandlerMedRoller = SaksbehandlerMedRoller(Saksbehandler("", "ident", null))

        tilgangService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sakId,
            persongalleri = Persongalleri(
                soeker = "11111",
                innsender = "11111",
                soesken = listOf("11111", "04040", "05050"),
                avdoed = listOf("06060", "11111"),
                gjenlevende = listOf("11111")
            )
        )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandling =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedRoller
            )

        Assertions.assertEquals(false, harTilgangTilBehandling)

        val smokeTestBehandling =
            tilgangService.harTilgangTilBehandling(UUID.randomUUID().toString(), saksbehandlerMedRoller)

        Assertions.assertEquals(true, smokeTestBehandling)
    }

    @Test
    fun `Skal kunne sette strengt fortrolig på sak og se på den med riktig rolle men ikke fortrolig rolle`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()
        val saksbehandlerMedStrengtfortrolig = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims))
        )

        tilgangService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sakId,
            persongalleri = Persongalleri(
                soeker = "11111",
                innsender = "11111",
                soesken = listOf("11111", "04040", "05050"),
                avdoed = listOf("06060", "11111"),
                gjenlevende = listOf("11111")
            )
        )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandlingSomStrengtFortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingSomStrengtFortrolig)

        val jwtclaimsFortrolig = JWTClaimsSet.Builder().claim("groups", fortroligDev).build()
        val saksbehandlerMedFortrolig = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaimsFortrolig))
        )
        val harTilgangTilBehandlingSomfortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedFortrolig
            )

        Assertions.assertEquals(false, harTilgangTilBehandlingSomfortrolig)
    }

    @Test
    fun `Skal kunne se på skjermet sak hvis riktig rolle`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()
        val saksbehandlerMedStrengtfortrolig = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaims))
        )

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sakId,
            persongalleri = Persongalleri(
                soeker = "11111",
                innsender = "11111",
                soesken = listOf("11111", "04040", "05050"),
                avdoed = listOf("06060", "11111"),
                gjenlevende = listOf("11111")
            )
        )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val hartilgangtilvanligsak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig
            )

        Assertions.assertEquals(true, hartilgangtilvanligsak)

        sakRepo.markerSakerMedSkjerming(listOf(sakId), true)

        val hartilgangSomStrengtFortroligMotEgenAnsattSak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig
            )

        Assertions.assertEquals(false, hartilgangSomStrengtFortroligMotEgenAnsattSak)

        val jwtclaimsEgenAnsatt = JWTClaimsSet.Builder().claim("groups", egenAnsattDev).build()
        val saksbehandlerMedEgenansatt = SaksbehandlerMedRoller(
            Saksbehandler("", "ident", JwtTokenClaims(jwtclaimsEgenAnsatt))
        )
        val harTilgangTilBehandlingMedEgenAnsattRolle =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedEgenansatt
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingMedEgenAnsattRolle)
    }
}