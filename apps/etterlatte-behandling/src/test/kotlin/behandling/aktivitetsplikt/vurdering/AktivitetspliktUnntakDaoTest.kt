package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDaoTest.Companion.kilde
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType.GRADERT_UFOERETRYGD
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktUnntakDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktUnntakDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))
    private val behandlingDao =
        BehandlingDao(
            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(ds)),
            RevurderingDao(ConnectionAutoclosingTest(ds)),
            (ConnectionAutoclosingTest(ds)),
        )

    @Test
    fun `skal lagre ned og hente opp et nytt unntak for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.opprettUnntak(unntak, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentUnntakForOppgave(oppgave.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.unntak shouldBe unntak.unntak
            it.fom shouldBe unntak.fom
            it.tom shouldBe unntak.tom
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe unntak.beskrivelse
        }
    }

    @Test
    fun `Skal hente seneste unntak`() {
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val kilde1 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val kilde2 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().plus(1000, ChronoUnit.SECONDS))
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.opprettUnntak(unntak, sak.id, kilde1, null, null)
        dao.opprettUnntak(unntak.copy(unntak = GRADERT_UFOERETRYGD), sak.id, kilde2, null, null)

        dao.hentNyesteUnntak(sak.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.unntak shouldBe GRADERT_UFOERETRYGD
            it.fom shouldBe unntak.fom
            it.tom shouldBe unntak.tom
            it.opprettet shouldBe kilde2
            it.endret shouldBe kilde2
            it.beskrivelse shouldBe unntak.beskrivelse
        }
    }

    @Test
    fun `skal lagre ned og hente opp et nytt unntak for behandling`() {
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )
        behandlingDao.opprettBehandling(opprettBehandling)
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.opprettUnntak(unntak, sak.id, kilde, null, opprettBehandling.id)

        dao.hentUnntakForBehandling(opprettBehandling.id)!!.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe opprettBehandling.id
            it.oppgaveId shouldBe null
            it.unntak shouldBe unntak.unntak
            it.fom shouldBe unntak.fom
            it.tom shouldBe unntak.tom
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe unntak.beskrivelse
        }
    }

    @Nested
    inner class OppdaterUnntak {
        @Test
        fun `Oppdatere unntak`() {
            val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
            val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
            val opprettBehandling =
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak.id,
                )
            behandlingDao.opprettBehandling(opprettBehandling)
            val lagreUnntak =
                LagreAktivitetspliktUnntak(
                    unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                    beskrivelse = "Beskrivelse",
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                )

            dao.opprettUnntak(lagreUnntak, sak.id, kilde, null, opprettBehandling.id)

            val unntak = dao.hentUnntakForBehandling(opprettBehandling.id)

            val lagreUnntakMedId =
                LagreAktivitetspliktUnntak(
                    id = unntak!!.id,
                    unntak = AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM,
                    beskrivelse = "Beskrivelse er oppdatert",
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusMonths(6),
                )

            dao.oppdaterUnntak(lagreUnntakMedId, kilde, opprettBehandling.id)

            dao.hentUnntakForBehandling(opprettBehandling.id)!!.asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.unntak shouldBe lagreUnntakMedId.unntak
                it.endret shouldBe kilde
                it.beskrivelse shouldBe lagreUnntakMedId.beskrivelse
            }
        }
    }

    @Nested
    inner class SlettAktivitet {
        private val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        private val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        private val opprettBehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
            )
        private val lagreUnntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        @Test
        fun `skal slette unntak`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.opprettUnntak(lagreUnntak, sak.id, kilde, null, opprettBehandling.id)

            val unntak = dao.hentUnntakForBehandling(opprettBehandling.id)
            dao.slettUnntak(unntak!!.id, opprettBehandling.id)

            dao.hentUnntakForBehandling(opprettBehandling.id) shouldBe null
        }

        @Test
        fun `skal ikke slette unntak hvis behandling id ikke stemmer`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.opprettUnntak(lagreUnntak, sak.id, kilde, null, opprettBehandling.id)

            val unntak = dao.hentUnntakForBehandling(opprettBehandling.id)
            dao.slettUnntak(unntak!!.id, UUID.randomUUID())

            dao.hentUnntakForBehandling(opprettBehandling.id)!!.asClue {
                it.id shouldBe unntak.id
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.unntak shouldBe unntak.unntak
                it.fom shouldBe unntak.fom
                it.tom shouldBe unntak.tom
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe unntak.beskrivelse
            }
        }
    }

    @Nested
    inner class KopierUnntak {
        private val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")

        private val lagreUnntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
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
        fun `skal kopiere nyeste unntak til behandling`() {
            behandlingDao.opprettBehandling(forrigeBehandling)
            behandlingDao.opprettBehandling(nyBehandling)

            dao.opprettUnntak(lagreUnntak, sak.id, kilde, null, forrigeBehandling.id)

            val unntak = dao.hentUnntakForBehandling(forrigeBehandling.id)
            dao.hentUnntakForBehandling(nyBehandling.id) shouldBe null

            dao.kopierUnntak(unntak!!.id, nyBehandling.id) shouldBe 1

            dao.hentUnntakForBehandling(nyBehandling.id)!!.asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe nyBehandling.id
                it.unntak shouldBe unntak.unntak
                it.fom shouldBe unntak.fom
                it.tom shouldBe unntak.tom
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe unntak.beskrivelse
            }
        }
    }
}
