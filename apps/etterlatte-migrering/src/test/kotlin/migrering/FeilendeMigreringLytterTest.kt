package migrering

import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.spyk
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.Pesyskopling
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PESYS_ID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.HENDELSE_DATA_KEY
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class FeilendeMigreringLytterTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var datasource: DataSource

    @BeforeEach
    fun start() {
        datasource = opprettInMemoryDatabase(postgreSQLContainer).dataSource
    }

    @AfterEach
    fun stop() = postgreSQLContainer.stop()

    @Test
    fun `logger og lagrer feilmelding`() {
        testApplication {
            val behandlingId = UUID.randomUUID()
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                        )
                }
            val pesyssak =
                Pesyssak(
                    id = pesysid.id,
                    enhet = Enhet("1"),
                    soeker = Folkeregisteridentifikator.of("08071272487"),
                    gjenlevendeForelder = null,
                    avdoedForelder = listOf(),
                    virkningstidspunkt = YearMonth.now(),
                    foersteVirkningstidspunkt = YearMonth.now(),
                    beregning =
                        Beregning(
                            brutto = BigDecimal.ZERO,
                            netto = BigDecimal.ZERO,
                            anvendtTrygdetid = BigDecimal.ZERO,
                            datoVirkFom = Tidspunkt.now(),
                            g = BigDecimal.ZERO,
                        ),
                    trygdetid = Trygdetid(perioder = listOf()),
                    flyktningStatus = false,
                )
            repository.lagrePesyssak(pesyssak)

            TestRapid()
                .apply {
                    FeilendeMigreringLytter(rapidsConnection = this, repository = repository)
                }.sendTestMessage(
                    JsonMessage.newMessage(
                        mapOf(
                            EVENT_NAME_KEY to EventNames.FEILA,
                            KILDE_KEY to Vedtaksloesning.PESYS,
                            PESYS_ID_KEY to pesysid,
                            HENDELSE_DATA_KEY to pesyssak.tilMigreringsrequest(),
                            FEILMELDING_KEY to IllegalStateException("her feiler det"),
                            FEILENDE_STEG to Migreringshendelser.TRYGDETID,
                        ),
                    ).toJson(),
                )
            Assertions.assertEquals(Migreringsstatus.MIGRERING_FEILA, repository.hentStatus(pesysid.id))
        }
    }
}
