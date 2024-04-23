package behandling.aktivitetsplikt

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitetType
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.OpprettAktivitetspliktAktivitet
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktDaoTest(ds: DataSource) {
    private val dao = AktivitetspliktDao(ConnectionAutoclosingTest(ds))

    @Test
    fun `skal hente aktiviteter for behandling`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivtet = opprettAktivitet()
        dao.opprettAktivitet(behandlingId, nyAktivtet, kilde)
        dao.opprettAktivitet(UUID.randomUUID(), nyAktivtet, kilde)

        val aktiviteter = dao.hentAktiviteter(behandlingId)

        aktiviteter.size shouldBe 1
    }

    @Test
    fun `skal opprette ny aktivetet`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivitet = opprettAktivitet()

        dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)

        val hentAktiviteter = dao.hentAktiviteter(behandlingId)
        hentAktiviteter.first().asClue { aktivitet ->
            aktivitet.sakId shouldBe nyAktivitet.sakId
            aktivitet.behandlingId shouldBe behandlingId
            aktivitet.type shouldBe nyAktivitet.type
            aktivitet.fom shouldBe nyAktivitet.fom
            aktivitet.tom shouldBe nyAktivitet.tom
            aktivitet.opprettet shouldBe kilde
            aktivitet.endret shouldBe kilde
            aktivitet.beskrivelse shouldBe nyAktivitet.beskrivelse
        }
    }

    companion object {
        fun opprettAktivitet() =
            OpprettAktivitetspliktAktivitet(
                sakId = 1L,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
    }
}
