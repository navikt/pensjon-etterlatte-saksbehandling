package behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktUnntakDaoTest(ds: DataSource) {
    private val dao = AktivitetspliktUnntakDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))

    @Test
    fun `skal lagre ned og hente opp et nytt unntak`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                tom = LocalDate.now().plusMonths(6),
            )

        dao.opprettUnntak(unntak, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentUnntak(oppgave.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.unntak shouldBe unntak.unntak
            it.tom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe unntak.beskrivelse
        }
    }
}
