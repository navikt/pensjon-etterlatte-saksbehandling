package no.nav.etterlatte.sanksjon

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.bruker
import no.nav.etterlatte.beregning.regler.lagreSanksjon
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SanksjonRepositoryTest(
    ds: DataSource,
) {
    private val sanksjonRepository = SanksjonRepository(ds)
    private val sakId = randomSakId()

    @Test
    fun `skal returnere null hvis mangler sanksjon`() {
        sanksjonRepository.hentSanksjon(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal opprette sanksjon`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon = lagreSanksjon()

        sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon)

        val lagretSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretSanksjon!!.asClue {
            it[0].asClue { sanksjon ->
                sanksjon.behandlingId shouldBe behandlingId
            }
        }
    }

    @Test
    fun `Skal oppdatere sanksjon`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon = lagreSanksjon()

        sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon)

        val lagretSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretSanksjon!!.asClue {
            it[0].asClue { sanksjonLagret ->
                sanksjonLagret.beskrivelse shouldBe sanksjon.beskrivelse
            }
        }

        val oppdatertSanksjon =
            lagreSanksjon(
                id = lagretSanksjon.first().id,
                beskrivelse = "Er nå i full jobb",
            )

        sanksjonRepository.oppdaterSanksjon(oppdatertSanksjon, bruker.ident)

        val lagretOppdatertSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretOppdatertSanksjon!!.asClue {
            it[0].asClue { sanksjonLagret ->
                sanksjonLagret.beskrivelse shouldNotBe sanksjon.beskrivelse
                sanksjonLagret.beskrivelse shouldBe oppdatertSanksjon.beskrivelse
            }
        }
    }

    @Test
    fun `Skal slette en sanksjon basert på behandling og sanksjonid`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon = lagreSanksjon()

        sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon)

        val lagretSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretSanksjon!!.size shouldBe 1

        sanksjonRepository.slettSanksjon(lagretSanksjon.first().id!!)

        val ingenSanksjoner = sanksjonRepository.hentSanksjon(behandlingId)

        ingenSanksjoner shouldBe null
    }

    @Test
    fun `skal kunne opprette en sanksjon fra kopi`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon = lagreSanksjon()
        sanksjonRepository.opprettSanksjon(behandlingId, sakId, bruker.ident, sanksjon)

        val nyBehandlingId = UUID.randomUUID()
        val gammelSanksjon = sanksjonRepository.hentSanksjon(behandlingId)?.get(0)!!
        sanksjonRepository.opprettSanksjonFraKopi(
            behandlingId = nyBehandlingId,
            sakId = sakId,
            sanksjon = gammelSanksjon,
        )
        sanksjonRepository.hentSanksjon(nyBehandlingId)?.size shouldBe 1
    }
}
