package no.nav.etterlatte.migrering

import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.spyk
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.opprettInMemoryDatabase
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
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
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

class FeilendeMigreringLytterRiverTestRiver {
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
                            sakId = 321L,
                        )
                }
            val pesyssak =
                Pesyssak(
                    id = pesysid.id,
                    enhet = Enhet("1"),
                    soeker = SOEKER_FOEDSELSNUMMER,
                    gjenlevendeForelder = null,
                    avdoedForelder = listOf(),
                    dodAvYrkesskade = false,
                    foersteVirkningstidspunkt = YearMonth.now(),
                    beregning =
                        Beregning(
                            brutto = 3500,
                            netto = 3500,
                            anvendtTrygdetid = 40,
                            datoVirkFom = Tidspunkt.now(),
                            prorataBroek = null,
                            g = 100_000,
                        ),
                    trygdetid = Trygdetid(perioder = listOf()),
                    flyktningStatus = false,
                    spraak = Spraak.NN,
                )
            repository.lagrePesyssak(pesyssak)
            repository.lagreKoplingTilBehandling(behandlingId, pesysid, 123L)

            TestRapid()
                .apply {
                    FeilendeMigreringLytterRiver(rapidsConnection = this, repository = repository)
                }.sendTestMessage(
                    JsonMessage.newMessage(
                        mapOf(
                            EventNames.FEILA.lagParMedEventNameKey(),
                            KILDE_KEY to Vedtaksloesning.PESYS,
                            PESYS_ID_KEY to pesysid,
                            HENDELSE_DATA_KEY to pesyssak.tilMigreringsrequest(),
                            FEILMELDING_KEY to IllegalStateException("her feiler det"),
                            FEILENDE_STEG to Migreringshendelser.TRYGDETID.lagEventnameForType(),
                        ),
                    ).toJson(),
                )
            Assertions.assertEquals(Migreringsstatus.MIGRERING_FEILA, repository.hentStatus(pesysid.id))
        }
    }

    @Test
    fun `logger og lagrer feilmelding for feil fra attestering`() {
        testApplication {
            val fil = this::class.java.getResource("/attesteringsfeil.json")!!.readText()

            val behandlingId = UUID.fromString("bd8e7578-01fe-4e3e-b560-daf859c45c5f")
            val pesysid = PesysId(1L)
            val repository =
                spyk(PesysRepository(datasource)).also {
                    every { it.hentPesysId(behandlingId) } returns
                        Pesyskopling(
                            behandlingId = behandlingId,
                            pesysId = pesysid,
                            sakId = 321L,
                        )
                }
            val pesyssak =
                Pesyssak(
                    id = pesysid.id,
                    enhet = Enhet("1"),
                    soeker = SOEKER_FOEDSELSNUMMER,
                    gjenlevendeForelder = null,
                    avdoedForelder = listOf(),
                    dodAvYrkesskade = false,
                    foersteVirkningstidspunkt = YearMonth.now(),
                    beregning =
                        Beregning(
                            brutto = 3500,
                            netto = 3500,
                            anvendtTrygdetid = 40,
                            datoVirkFom = Tidspunkt.now(),
                            prorataBroek = null,
                            g = 100_000,
                        ),
                    trygdetid = Trygdetid(perioder = listOf()),
                    flyktningStatus = false,
                    spraak = Spraak.NN,
                )
            repository.lagrePesyssak(pesyssak)
            repository.lagreKoplingTilBehandling(behandlingId, pesysid, 123L)

            TestRapid().apply {
                FeilendeMigreringLytterRiver(rapidsConnection = this, repository = repository)
            }.sendTestMessage(fil)
            Assertions.assertEquals(Migreringsstatus.MIGRERING_FEILA, repository.hentStatus(pesysid.id))
        }
    }
}
