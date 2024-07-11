package no.nav.etterlatte.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class ViderefoertOpphoerTest(
    private val dataSource: DataSource,
) {
    @Test
    fun `lagrer viderefoert opphoer`() {
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        nyKontekstMedBrukerOgDatabaseContext(user, DatabaseContextTest(dataSource))
        val connection = ConnectionAutoclosingTest(dataSource)
        val kommerBarnetTilGodeDao =
            mockk<KommerBarnetTilGodeDao>().also { every { it.hentKommerBarnetTilGode(any()) } returns null }
        val dao = BehandlingDao(kommerBarnetTilGodeDao, mockk(), connection)
        val service =
            BehandlingServiceImpl(
                behandlingDao = dao,
                behandlingHendelser = mockk(),
                grunnlagsendringshendelseDao = mockk(),
                hendelseDao = mockk(),
                grunnlagKlient = mockk(),
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
                oppgaveService = mockk(),
                grunnlagService = mockk(),
                beregningKlient = mockk(),
            )
        val sak =
            SakDao(connection).opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enheter.defaultEnhet.enhetNr,
            )
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        dao.opprettBehandling(behandling = opprettBehandling)
        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
                ViderefoertOpphoer(
                    behandlingId = opprettBehandling.id,
                    dato = YearMonth.of(2024, Month.JUNE),
                    begrunnelse = "for testformål",
                    vilkaar = "BP_FORMAAL_2024",
                    kilde = Grunnlagsopplysning.Saksbehandler.create("A123"),
                    kravdato = null,
                ),
        )
    }
}
