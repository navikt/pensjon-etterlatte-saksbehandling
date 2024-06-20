package behandling.aktivitetsplikt.vurdering

import behandling.aktivitetsplikt.AktivitetspliktDaoTest.Companion.kilde
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktAktivitetsgradDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktAktivitetsgradDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))
    private val behandlingDao =
        BehandlingDao(
            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(ds)),
            RevurderingDao(ConnectionAutoclosingTest(ds)),
            (ConnectionAutoclosingTest(ds)),
        )

    @Test
    fun `skal lagre ned og hente opp en ny aktivitetsgrad for oppgave`() {
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

        dao.hentAktivitetsgradForOppgave(oppgave.id)!!.asClue {
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

    @Test
    fun `skal lagre ned og hente opp en ny aktivitetsgrad for behandling`() {
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )
        behandlingDao.opprettBehandling(opprettBehandling)
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

        dao.hentAktivitetsgradForBehandling(opprettBehandling.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe opprettBehandling.id
            it.oppgaveId shouldBe null
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
        }
    }

    @Nested
    inner class OppdaterAktivitetsgrad {
        @Test
        fun `Oppdatere aktivitetsgrad`() {
            val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
            val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
            val opprettBehandling =
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak.id,
                )
            behandlingDao.opprettBehandling(opprettBehandling)
            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AKTIVITET_OVER_50,
                    beskrivelse = "Beskrivelse",
                )

            dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id)

            val aktivitetsgradMedId =
                LagreAktivitetspliktAktivitetsgrad(
                    id = aktivitet!!.id,
                    aktivitetsgrad = AKTIVITET_100,
                    beskrivelse = "Beskrivelse er oppdatert",
                )

            dao.oppdaterAktivitetsgrad(aktivitetsgradMedId, kilde, opprettBehandling.id)

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id)!!.asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.aktivitetsgrad shouldBe AKTIVITET_100
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgradMedId.beskrivelse
            }
        }
    }

    @Nested
    inner class SlettAktivitetsgrad {
        private val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        private val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        private val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )
        private val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        @Test
        fun `skal slette aktivitetsgrad`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id)
            dao.slettAktivitetsgrad(aktivitet!!.id, opprettBehandling.id)

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id) shouldBe null
        }

        @Test
        fun `skal ikke slette aktivitetsgrad hvis behandling id ikke stemmer`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.opprettAktivitetsgrad(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id)
            dao.slettAktivitetsgrad(aktivitet!!.id, UUID.randomUUID())

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id)!!.asClue {
                it.id shouldBe aktivitet.id
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            }
        }
    }

    @Nested
    inner class KopierAktivitetsgrad {
        private val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")

        val lagreAktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        private val forrigeBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )

        private val nyBehandling =
            opprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak.id,
            )

        @Test
        fun `skal kopiere nyeste aktivitetsgrad til behandling`() {
            behandlingDao.opprettBehandling(forrigeBehandling)
            behandlingDao.opprettBehandling(nyBehandling)

            dao.opprettAktivitetsgrad(lagreAktivitetsgrad, sak.id, kilde, null, forrigeBehandling.id)

            val aktivitetsgrad = dao.hentAktivitetsgradForBehandling(forrigeBehandling.id)
            dao.hentAktivitetsgradForBehandling(nyBehandling.id) shouldBe null

            dao.kopierAktivitetsgrad(aktivitetsgrad!!.id, nyBehandling.id) shouldBe 1

            dao.hentAktivitetsgradForBehandling(nyBehandling.id)!!.asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe nyBehandling.id
                it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
                it.fom shouldBe aktivitetsgrad.fom
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            }
        }
    }
}
