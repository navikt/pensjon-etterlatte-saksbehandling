package no.nav.etterlatte.adressebeskyttelse

import behandling.tilbakekreving.kravgrunnlag
import behandling.tilbakekreving.tilbakekrevingVurdering
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.azureAdEgenAnsattClaim
import no.nav.etterlatte.azureAdFortroligClaim
import no.nav.etterlatte.azureAdStrengtFortroligClaim
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.klage.KlageDao
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.SakTilgangImpl
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.sak.TilgangServiceSjekker
import no.nav.etterlatte.sak.TilgangServiceSjekkerImpl
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class TilgangServiceTest(
    val dataSource: DataSource,
) {
    private lateinit var tilgangService: TilgangServiceSjekker
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var sakLesDao: SakLesDao
    private lateinit var sakendringerDao: SakendringerDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var klageDao: KlageDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao
    private lateinit var sakTilgang: SakTilgang

    @BeforeAll
    fun beforeAll() {
        tilgangService = TilgangServiceSjekkerImpl(SakTilgangDao(dataSource))
        sakLesDao = SakLesDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        sakendringerDao = SakendringerDao(ConnectionAutoclosingTest(dataSource))
        sakTilgang = SakTilgangImpl(sakSkrivDao, sakLesDao)
        behandlingRepo =
            BehandlingDao(
                KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                (ConnectionAutoclosingTest(dataSource)),
            )
        klageDao = KlageDaoImpl(ConnectionAutoclosingTest(dataSource))
        tilbakekrevingDao = TilbakekrevingDao(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse på sak`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val saksbehandlerMedRoller =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(),
                emptyMap(),
            )
        sakTilgang.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandling =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedRoller,
            )

        Assertions.assertEquals(false, harTilgangTilBehandling)
    }

    @Test
    fun `Skal sjekke tilganger til klager med klageId for behandlingId`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sak = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(ident = "ident", claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim),
            )
        sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
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
                simpleSaksbehandler(
                    ident = "annenIdent",
                    claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim),
                ),
                mapOf(),
            )
        val harTilgangStrengtFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                klage.id,
                saksbehandlerMedStrengtfortrolig,
            )
        val harTilgangStrengtFortroligKlage =
            tilgangService.harTilgangTilKlage(klage.id, saksbehandlerMedStrengtfortrolig)

        Assertions.assertTrue(harTilgangStrengtFortroligKlage)
        Assertions.assertTrue(harTilgangStrengtFortroligBehandling)

        val harTilgangIkkeFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                klage.id,
                saksbehandlerUtenStrengtFortrolig,
            )
        val harTilgangIkkeFortroligKlage =
            tilgangService.harTilgangTilKlage(klage.id, saksbehandlerUtenStrengtFortrolig)

        Assertions.assertFalse(harTilgangIkkeFortroligBehandling)
        Assertions.assertFalse(harTilgangIkkeFortroligKlage)
    }

    @Test
    fun `Skal sjekke tilganger til tilbakekreving med tilbakekrevingId for behandlingId`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sak = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(ident = "ident", claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim),
            )
        sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val tilbakekreving =
            TilbakekrevingBehandling(
                UUID.randomUUID(),
                TilbakekrevingStatus.OPPRETTET,
                sak,
                null,
                Tidspunkt.now(),
                aarsakForAvbrytelse = null,
                Tilbakekreving(tilbakekrevingVurdering(), emptyList(), kravgrunnlag(sak), null),
                true,
            )
        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving)

        val saksbehandlerUtenStrengtFortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(
                    ident = "annenIdent",
                    claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim),
                ),
                mapOf(),
            )
        val harTilgangStrengtFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                tilbakekreving.id,
                saksbehandlerMedStrengtfortrolig,
            )
        val harTilgangStrengtFortroligTilbakekreving =
            tilgangService.harTilgangTilTilbakekreving(
                tilbakekreving.id,
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertTrue(harTilgangStrengtFortroligTilbakekreving)
        Assertions.assertTrue(harTilgangStrengtFortroligBehandling)

        val harTilgangIkkeFortroligBehandling =
            tilgangService.harTilgangTilBehandling(
                tilbakekreving.id,
                saksbehandlerUtenStrengtFortrolig,
            )
        val harTilgangIkkeFortroligKlage =
            tilgangService.harTilgangTilTilbakekreving(
                tilbakekreving.id,
                saksbehandlerUtenStrengtFortrolig,
            )

        Assertions.assertFalse(harTilgangIkkeFortroligBehandling)
        Assertions.assertFalse(harTilgangIkkeFortroligKlage)
    }

    @Test
    fun `Skal kunne sette strengt fortrolig på sak og se på den med riktig rolle men ikke fortrolig rolle`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim),
            )
        sakTilgang.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val harTilgangTilBehandlingSomStrengtFortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingSomStrengtFortrolig)

        val saksbehandlerMedFortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdFortroligClaim)),
                mapOf(AzureGroup.FORTROLIG to azureAdFortroligClaim),
            )
        val harTilgangTilBehandlingSomfortrolig =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedFortrolig,
            )

        Assertions.assertEquals(false, harTilgangTilBehandlingSomfortrolig)
    }

    @Test
    fun `Skal kunne se på skjermet sak hvis riktig rolle`() {
        val fnr = AVDOED_FOEDSELSNUMMER.value
        val sakId = sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr).id
        val saksbehandlerMedStrengtfortrolig =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdStrengtFortroligClaim)),
                mapOf(AzureGroup.STRENGT_FORTROLIG to azureAdStrengtFortroligClaim),
            )

        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakId,
            )
        behandlingRepo.opprettBehandling(opprettBehandling)
        val hartilgangtilvanligsak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(true, hartilgangtilvanligsak)

        sakSkrivDao.oppdaterSkjerming(sakId, true)

        val hartilgangSomStrengtFortroligMotEgenAnsattSak =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedStrengtfortrolig,
            )

        Assertions.assertEquals(false, hartilgangSomStrengtFortroligMotEgenAnsattSak)

        val saksbehandlerMedEgenansatt =
            SaksbehandlerMedRoller(
                simpleSaksbehandler(claims = mapOf(Claims.groups to azureAdEgenAnsattClaim)),
                mapOf(AzureGroup.EGEN_ANSATT to azureAdEgenAnsattClaim),
            )
        val harTilgangTilBehandlingMedEgenAnsattRolle =
            tilgangService.harTilgangTilBehandling(
                opprettBehandling.id,
                saksbehandlerMedEgenansatt,
            )

        Assertions.assertEquals(true, harTilgangTilBehandlingMedEgenAnsattRolle)
    }
}
