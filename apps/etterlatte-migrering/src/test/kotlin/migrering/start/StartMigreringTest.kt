package no.nav.etterlatte.migrering.start

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotliquery.queryOf
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.migrering.DatabaseExtension
import no.nav.helse.rapids_rivers.RapidsConnection
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartMigreringTest(private val dataSource: DataSource) {
    private val repository: StartMigreringRepository = StartMigreringRepository(dataSource)

    @Test
    fun `starter migrering`() {
        dataSource.transaction { tx ->
            queryOf(
                "INSERT INTO ${StartMigreringRepository.Databasetabell.TABELLNAVN}" +
                    " (${StartMigreringRepository.Databasetabell.SAKID})" +
                    "VALUES(123)",
            ).let { query -> tx.run(query.asUpdate) }
        }
        Assertions.assertEquals(1, repository.hentSakerTilMigrering().size)
        val starter =
            StartMigrering(
                repository = repository,
                rapidsConnection = mockk<RapidsConnection>().also { every { it.publish(any(), any()) } just runs },
                featureToggleService = mockk<FeatureToggleService>().also { every { it.isEnabled(any(), any()) } returns true },
            )

        starter.startMigrering()
        Assertions.assertEquals(0, repository.hentSakerTilMigrering().size)
    }
}
