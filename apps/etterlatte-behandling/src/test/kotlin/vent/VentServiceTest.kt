package no.nav.etterlatte.vent

import io.mockk.mockk
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class VentServiceTest {
    @Test
    fun test() {
        val dataSource = DatabaseExtension.dataSource

        val sak =
            SakDao { dataSource.connection }.opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enheter.STEINKJER.enhetNr,
            )

        val service = VentService(VentDao { dataSource.connection })
        val oppgave = opprettOppgave(sak, dataSource)
        val opprettBehandling = opprettBehandling(sak, dataSource)
        service.settPaaVent(
            oppgaveId = oppgave.id,
            behandlingId = opprettBehandling.id,
            Ventetype.VARSLING,
            Tidspunkt.ofNorskTidssone(
                LocalDate.of(2022, Month.JANUARY, 1),
                LocalTime.NOON,
            ),
        )

        service.settPaaVent(
            oppgaveId = oppgave.id,
            behandlingId = opprettBehandling.id,
            Ventetype.VARSLING,
            Tidspunkt.now().plus(7, ChronoUnit.DAYS),
        )
        Assertions.assertEquals(1, service.haandterVentaFerdig().size)
        Assertions.assertEquals(0, service.haandterVentaFerdig().size)
    }

    private fun opprettBehandling(
        sak: Sak,
        dataSource: DataSource,
    ) = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        .also { BehandlingDao(mockk(), mockk()) { dataSource.connection }.opprettBehandling(it) }

    private fun opprettOppgave(
        sak: Sak,
        dataSource: DataSource,
    ): OppgaveIntern =
        opprettNyOppgaveMedReferanseOgSak(
            "",
            sak,
            OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
        ).also { OppgaveDaoImpl { dataSource.connection }.opprettOppgave(it) }
}
