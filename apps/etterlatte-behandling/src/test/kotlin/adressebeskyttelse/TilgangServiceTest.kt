package no.nav.etterlatte.adressebeskyttelse

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilgangServiceTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

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

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        tilgangService = TilgangServiceImpl(SakTilgangDao { dataSource.connection })
        sakRepo = SakDao { dataSource.connection }
        behandlingRepo =
            BehandlingDao(
                KommerBarnetTilGodeDao {
                    dataSource.connection
                },
                RevurderingDao { dataSource.connection },
            ) { dataSource.connection }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse på sak`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", null),
                emptyMap(),
            )

        tilgangService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandling =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedRoller,
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

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev),
            )

        tilgangService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandlingSomStrengtFortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingSomStrengtFortrolig)

        val jwtclaimsFortrolig = JWTClaimsSet.Builder().claim("groups", fortroligDev).build()
        val saksbehandlerMedFortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaimsFortrolig)),
                mapOf(AzureGroup.FORTROLIG to fortroligDev),
            )
        val harTilgangTilBehandlingSomfortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedFortrolig,
            )

        Assertions.assertEquals(false, harTilgangTilBehandlingSomfortrolig)
    }

    @Test
    fun `Skal kunne se på skjermet sak hvis riktig rolle`() {
        val fnr = Folkeregisteridentifikator.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr).id
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()
        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev),
            )

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val hartilgangtilvanligsak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(true, hartilgangtilvanligsak)

        sakRepo.markerSakerMedSkjerming(listOf(sakId), true)

        val hartilgangSomStrengtFortroligMotEgenAnsattSak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(false, hartilgangSomStrengtFortroligMotEgenAnsattSak)

        val jwtclaimsEgenAnsatt = JWTClaimsSet.Builder().claim("groups", egenAnsattDev).build()
        val saksbehandlerMedEgenansatt =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaimsEgenAnsatt)),
                mapOf(AzureGroup.EGEN_ANSATT to egenAnsattDev),
            )
        val harTilgangTilBehandlingMedEgenAnsattRolle =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedEgenansatt,
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingMedEgenAnsattRolle)
    }
}
