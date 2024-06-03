package behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktAktivitetsgradDaoTest(ds: DataSource) {
    private val dao = AktivitetspliktAktivitetsgradDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))

    @Test
    fun `skal lagre ned og hente opp en ny aktivitetsgrad`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentAktivitetsgrad(oppgave.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
        }
    }

    @Test
    fun `Skal hente nyeste aktivitetsgrad`() {
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val kilde1 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val kilde2 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde1, null, null)
        dao.opprettAktivitetsgrad(aktivitetsgrad.copy(aktivitetsgrad = AKTIVITET_100), sak.id, kilde2, null, null)

        dao.hentNyesteAktivitetsgrad(sak.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.oppgaveId shouldBe null
            it.behandlingId shouldBe null
            it.aktivitetsgrad shouldBe AKTIVITET_100
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde2
            it.endret shouldBe kilde2
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
        }
    }
}
