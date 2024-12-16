package behandling

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.SendManglendeMeldingerDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.testdata.grunnlag.BARN_FOEDSELSNUMMER
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SendManglendeMeldingerDaoTest(
    val dataSource: DataSource,
) {
    private val sakRepo = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })

    @AfterEach
    fun afterEach() {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE behandling_mangler_avbrudd_statistikk;").execute()
        }
    }

    @Test
    fun kanHenteMeldinger() {
        val sak = sakRepo.opprettSak(BARN_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val behandlingId = UUID.randomUUID()

        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO behandling_mangler_avbrudd_statistikk (behandling_id, sak_id, mangler_hendelse) values (?, ?, ?) 
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            statement.setLong(2, sak.id.sakId)
            statement.setBoolean(3, true)
            statement.executeUpdate()
        }

        val repo = SendManglendeMeldingerDao(ConnectionAutoclosingTest(dataSource))
        val manglerBehandling = repo.hentManglendeAvslagBehandling()
        manglerBehandling.single().asClue {
            it.behandlingId shouldBe behandlingId
            it.sakId shouldBe sak.id
            it.manglerHendelse shouldBe true
        }
    }

    @Test
    fun kanOppdatereSendtMelding() {
        val sak = sakRepo.opprettSak(BARN_FOEDSELSNUMMER.value, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        val behandlingId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            val statement =
                connection.prepareStatement(
                    """
                    INSERT INTO behandling_mangler_avbrudd_statistikk (behandling_id, sak_id, mangler_hendelse) values (?, ?, ?) 
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            statement.setLong(2, sak.id.sakId)
            statement.setBoolean(3, true)
            statement.executeUpdate()
        }

        val repo = SendManglendeMeldingerDao(ConnectionAutoclosingTest(dataSource))
        repo.hentManglendeAvslagBehandling().size shouldBe 1
        repo.oppdaterSendtMelding(behandlingId)
        repo.hentManglendeAvslagBehandling().size shouldBe 0
    }
}
