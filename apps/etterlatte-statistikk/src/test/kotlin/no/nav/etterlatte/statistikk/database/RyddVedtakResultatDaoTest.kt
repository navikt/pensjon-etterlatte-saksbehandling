package no.nav.etterlatte.statistikk.database

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.statistikk.StatistikkDatabaseExtension
import no.nav.etterlatte.statistikk.domain.BehandlingResultat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(StatistikkDatabaseExtension::class)
class RyddVedtakResultatDaoTest(
    private val dataSource: DataSource,
) {
    private val repo = RyddVedtakResultatDao(dataSource)
    private val sakRepo = SakRepository.using(dataSource)

    @AfterEach
    fun afterEach() {
        dataSource.connection.use { connection ->
            connection
                .prepareStatement("truncate table sak_rader_med_potensielt_feil_resultat")
                .executeUpdate()
        }
    }

    @Test
    fun `kan hente rader med potensielt feil resultat`() {
        val lagretRad =
            sakRepo.lagreRad(
                lagSak(
                    resultat = "IVERKSATT",
                ),
            )

        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO sak_rader_med_potensielt_feil_resultat (id, behandling_id, behandling_type) values (?, ?, ?);
                    """.trimIndent(),
                )
            statement.setLong(1, lagretRad!!.id)
            statement.setObject(2, lagretRad.referanseId)
            statement.setString(3, lagretRad.type)
            statement.executeUpdate()
        }
        val rader = repo.hentRaderMedPotensiellFeil()
        rader.size shouldBe 1
    }

    @Test
    fun `kan oppdatere rader med nytt resultat`() {
        val lagretRad =
            sakRepo.lagreRad(
                lagSak(
                    resultat = "IVERKSATT",
                ),
            )

        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO sak_rader_med_potensielt_feil_resultat (id, behandling_id, behandling_type) values (?, ?, ?);
                    """.trimIndent(),
                )
            statement.setLong(1, lagretRad!!.id)
            statement.setObject(2, lagretRad.referanseId)
            statement.setString(3, lagretRad.type)
            statement.executeUpdate()
        }
        val rad = repo.hentRaderMedPotensiellFeil().single()

        repo.oppdaterResultat(rad, BehandlingResultat.AVSLAG)

        val lagret = sakRepo.hentSisteRad(rad.behandlingId)
        lagret?.resultat shouldBe BehandlingResultat.AVSLAG.name
        repo.hentRaderMedPotensiellFeil() shouldBe emptyList()
    }
}
