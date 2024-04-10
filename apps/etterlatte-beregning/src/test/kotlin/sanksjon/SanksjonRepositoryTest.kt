package no.nav.etterlatte.sanksjon

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SanksjonRepositoryTest(ds: DataSource) {
    private val sanksjonRepository = SanksjonRepository(ds)

    @Test
    fun `skal returnere null hvis mangler sanksjon`() {
        sanksjonRepository.hentSanksjon(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal opprette sanksjon`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon =
            Sanksjon(
                id = null,
                behandlingId = behandlingId,
                sakId = 123,
                fom = YearMonth.of(2024, 1),
                tom = YearMonth.of(2024, 2),
                saksbehandler = "A12345",
                opprettet = Tidspunkt.now(),
                endret = Tidspunkt.now(),
                beskrivelse = "Ikke i jobb",
            )

        sanksjonRepository.opprettSanksjon(behandlingId, sanksjon)

        val lagretSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretSanksjon!!.asClue {
            it[0].asClue { sanksjon ->
                behandlingId shouldBe behandlingId
            }
        }
    }

    @Test
    fun `Skal oppdatere sanksjon`() {
        val behandlingId: UUID = UUID.randomUUID()
        val sanksjon =
            Sanksjon(
                id = null,
                behandlingId = behandlingId,
                sakId = 123,
                fom = YearMonth.of(2024, 1),
                tom = YearMonth.of(2024, 2),
                saksbehandler = "A12345",
                opprettet = Tidspunkt.now(),
                endret = Tidspunkt.now(),
                beskrivelse = "Ikke i jobb",
            )

        sanksjonRepository.opprettSanksjon(behandlingId, sanksjon)

        val lagretSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretSanksjon!!.asClue {
            it[0].asClue { sanksjonLagret ->
                sanksjonLagret.beskrivelse shouldBe sanksjon.beskrivelse
            }
        }

        val oppdatertSanksjon =
            Sanksjon(
                id = lagretSanksjon.first().id,
                behandlingId = lagretSanksjon.first().behandlingId,
                sakId = lagretSanksjon.first().sakId,
                fom = lagretSanksjon.first().fom,
                tom = lagretSanksjon.first().tom,
                saksbehandler = lagretSanksjon.first().saksbehandler,
                opprettet = lagretSanksjon.first().opprettet,
                endret = lagretSanksjon.first().endret,
                beskrivelse = "Er nå i full jobb",
            )

        sanksjonRepository.oppdaterSanksjon(oppdatertSanksjon)

        val lagretOppdatertSanksjon = sanksjonRepository.hentSanksjon(behandlingId)

        lagretOppdatertSanksjon!!.asClue {
            it[0].asClue { sanksjonLagret ->
                sanksjonLagret.beskrivelse shouldNotBe sanksjon.beskrivelse
                sanksjonLagret.beskrivelse shouldBe oppdatertSanksjon.beskrivelse
            }
        }
    }
}
