package no.nav.etterlatte.migrering.vent

import kotliquery.queryOf
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.migrering.DatabaseExtension
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_ID_FLERE_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVEKILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Date
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class StartTaAvVentTest(private val dataSource: DataSource) {
    private val ventRepository = VentRepository(dataSource)

    @Test
    fun `sender ut`() {
        val rapid = TestRapid()

        val oppgave1 = UUID.randomUUID()
        val oppgave2 = UUID.randomUUID()
        dataSource.transaction { tx ->
            with(VentRepository.Databasetabell) {
                queryOf(
                    "INSERT INTO $TABELLNAVN ($DATO, $OPPGAVER) VALUES (:date, :oppgaver)",
                    paramMap =
                        mapOf(
                            "date" to Date.valueOf(LocalDate.of(2024, Month.MARCH, 5)),
                            "oppgaver" to "$oppgave1;$oppgave2",
                        ),
                )
            }.let { query -> tx.run(query.asUpdate) }
        }

        val start =
            StartAaTaAvVent(
                ventRepository = ventRepository,
                rapidsConnection = rapid,
                featureToggleService = DummyFeatureToggleService().also { it.settBryter(VentFeatureToggle.TaAvVent, true) },
                iTraad = { it() },
                sleep = {},
            )

        val sendt = rapid.inspektør.message(0)
        assertEquals(1, rapid.inspektør.size)
        assertEquals(sendt.get(EVENT_NAME_KEY).textValue(), Ventehendelser.TA_AV_VENT.lagEventnameForType())
        assertEquals(sendt.get(OPPGAVEKILDE_KEY).textValue(), OppgaveKilde.GJENOPPRETTING.name)
        assertEquals(sendt.get(OPPGAVE_ID_FLERE_KEY).toList().map { it.asUUID() }, listOf(oppgave1, oppgave2))

        start.taAvVent()
        assertEquals(1, rapid.inspektør.size)
    }
}
