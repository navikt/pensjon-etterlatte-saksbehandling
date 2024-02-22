package no.nav.etterlatte.adressebeskyttelse

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.klage.KlageDao
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakServiceImpl
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sak.TilgangServiceImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.Saksbehandler
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class TilgangServiceTest(val dataSource: DataSource) {
    private lateinit var tilgangService: TilgangService
    private lateinit var sakService: SakService
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var klageDao: KlageDao
    private val strengtfortroligDev = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
    private val fortroligDev = "ea930b6b-9397-44d9-b9e6-f4cf527a632a"
    private val egenAnsattDev = "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d"
    private val brukerService = mockk<BrukerService>()
    private val skjermingKlient = mockk<SkjermingKlient>()

    @BeforeAll
    fun beforeAll() {
        tilgangService = TilgangServiceImpl(SakTilgangDao(dataSource))
        sakRepo = SakDao(ConnectionAutoclosingTest(dataSource))

        sakService = SakServiceImpl(sakRepo, skjermingKlient, brukerService)
        behandlingRepo =
            BehandlingDao(
                KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                (ConnectionAutoclosingTest(dataSource)),
            )
        klageDao = KlageDaoImpl(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse på sak`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", null),
                emptyMap(),
            )
        sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FOERSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandling =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id.toString(),
                saksbehandlerMedRoller,
            )

        Assertions.assertEquals(false, harTilgangTilBehandling)
    }

    @Test
    fun `Skal sjekke tilganger til klager med klageId for behandlingId`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sak = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev),
            )
        sakService.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val klage =
            Klage.ny(
                sak,
                InnkommendeKlage(
                    mottattDato = LocalDate.of(2024, 2, 1),
                    journalpostId = "123",
                    innsender = null,
                ),
            )
        klageDao.lagreKlage(klage)

        val saksbehandlerUtenStrengtFortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "annenIdent", JwtTokenClaims(jwtclaims)),
                mapOf(),
            )
        val harTilgangStrengtFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                klage.id.toString(),
                saksbehandlerMedStrengtfortrolig,
            )
        val harTilgangStrengtFortroligKlage = tilgangService.harTilgangTilKlage(klage.id.toString(), saksbehandlerMedStrengtfortrolig)

        Assertions.assertTrue(harTilgangStrengtFortroligKlage)
        Assertions.assertTrue(harTilgangStrengtFortroligBehandling)

        val harTilgangIkkeFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                klage.id.toString(),
                saksbehandlerUtenStrengtFortrolig,
            )
        val harTilgangIkkeFortroligKlage = tilgangService.harTilgangTilKlage(klage.id.toString(), saksbehandlerUtenStrengtFortrolig)

        Assertions.assertFalse(harTilgangIkkeFortroligBehandling)
        Assertions.assertFalse(harTilgangIkkeFortroligKlage)
    }

    @Test
    fun `Skal kunne sette strengt fortrolig på sak og se på den med riktig rolle men ikke fortrolig rolle`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev),
            )
        sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FOERSTEGANGSBEHANDLING,
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
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr).id
        val jwtclaims = JWTClaimsSet.Builder().claim("groups", strengtfortroligDev).build()
        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                Saksbehandler("", "ident", JwtTokenClaims(jwtclaims)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to strengtfortroligDev),
            )

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FOERSTEGANGSBEHANDLING,
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
