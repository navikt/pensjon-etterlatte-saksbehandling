package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDaoTest.Companion.kilde
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType.GRADERT_UFOERETRYGD
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))
    private val behandlingDao =
        BehandlingDao(
            KommerBarnetTilGodeDao(ConnectionAutoclosingTest(ds)),
            RevurderingDao(ConnectionAutoclosingTest(ds)),
            (ConnectionAutoclosingTest(ds)),
        )
    private val user =
        mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns this::class.java.simpleName }

    @BeforeEach
    fun setup() {
        nyKontekstMedBruker(user)
    }

    @Test
    fun `skal lagre ned og hente opp et nytt unntak for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.upsertUnntak(unntak, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentUnntakForOppgave(oppgave.id).single().asClue {
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
    fun `Skal slette unntak for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.upsertUnntak(unntak, sak.id, kilde, oppgave.id, behandlingId)

        val hentetUnntakSomSkalSlettes = dao.hentUnntakForOppgave(oppgave.id).single()
        hentetUnntakSomSkalSlettes.asClue {
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

        dao.upsertUnntak(unntak.copy(unntak = MIDLERTIDIG_SYKDOM), sak.id, kilde, oppgave.id, behandlingId)

        dao.slettUnntakForOppgave(oppgave.id, hentetUnntakSomSkalSlettes.id)
        val hentetUnntakForOppgave = dao.hentUnntakForOppgave(oppgave.id)
        hentetUnntakForOppgave.size shouldBe 1
        hentetUnntakForOppgave.first().id shouldNotBe hentetUnntakSomSkalSlettes.id
    }

    @Test
    fun `skal kunne oppdatere et eksisterende unntak for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        dao.upsertUnntak(unntak, sak.id, kilde, oppgave.id, behandlingId)

        val lagretUnntak = dao.hentUnntakForOppgave(oppgave.id).single()

        val oppdatertUnntak =
            unntak.copy(
                id = lagretUnntak.id,
                unntak = MIDLERTIDIG_SYKDOM,
                fom = unntak.fom?.plusMonths(1L),
                tom = unntak.tom?.plusMonths(1L),
                beskrivelse = "Ny beskrivelse",
            )

        dao.upsertUnntak(oppdatertUnntak, sak.id, kilde, oppgave.id, behandlingId)
        val oppdatertLagretUnntak = dao.hentUnntakForOppgave(oppgave.id).single()
        oppdatertLagretUnntak.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.unntak shouldBe oppdatertUnntak.unntak
            it.fom shouldBe oppdatertUnntak.fom
            it.tom shouldBe oppdatertUnntak.tom
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe oppdatertUnntak.beskrivelse
        }
    }

    @Test
    fun `Skal hente alle seneste unntak for sak, selv om det ligger flere unntak i behandling`() {
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val kilde1 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val kilde2 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().plus(1000, ChronoUnit.SECONDS))
        val kilde3 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().plus(2, ChronoUnit.DAYS))
        val unntak =
            LagreAktivitetspliktUnntak(
                unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.now(),
                tom = LocalDate.now().plusMonths(6),
            )

        val behandling =
            OpprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak.id,
                status = BehandlingStatus.OPPRETTET,
                soeknadMottattDato = null,
                virkningstidspunkt = null,
                utlandstilknytning = null,
                boddEllerArbeidetUtlandet = null,
                revurderingsAarsak = null,
                prosesstype = Prosesstype.MANUELL,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                begrunnelse = null,
                relatertBehandlingId = null,
                sendeBrev = true,
                opphoerFraOgMed = null,
                opprinnelse = BehandlingOpprinnelse.UKJENT,
            )
        behandlingDao.opprettBehandling(behandling)

        val oppgave =
            OppgaveIntern(
                id = UUID.randomUUID(),
                status = Status.NY,
                enhet = sak.enhet,
                sakId = sak.id,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.AKTIVITETSPLIKT,
                saksbehandler = null,
                forrigeSaksbehandlerIdent = null,
                referanse = "",
                gruppeId = null,
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                fnr = sak.ident,
                frist = null,
            )
        oppgaveDao.opprettOppgave(oppgave)

        dao.upsertUnntak(unntak.copy(unntak = GRADERT_UFOERETRYGD), sak.id, kilde1, null, behandling.id)
        dao.upsertUnntak(unntak, sak.id, kilde2, null, behandling.id)
        dao.upsertUnntak(unntak.copy(unntak = MIDLERTIDIG_SYKDOM), sak.id, kilde3, oppgave.id, null)

        val nyesteUnntak = dao.hentNyesteUnntak(sak.id)

        nyesteUnntak.size shouldBe 1
        nyesteUnntak[0].asClue {
            it.sakId shouldBe sak.id
            it.unntak shouldBe MIDLERTIDIG_SYKDOM
            it.fom shouldBe unntak.fom
            it.tom shouldBe unntak.tom
            it.opprettet shouldBe kilde3
            it.endret shouldBe kilde3
            it.beskrivelse shouldBe unntak.beskrivelse
            it.oppgaveId shouldBe oppgave.id
            it.behandlingId shouldBe null
        }
    }

    @Test
    fun `skal lagre ned og hente opp et nytt unntak for behandling`() {
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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

        dao.upsertUnntak(unntak, sak.id, kilde, null, opprettBehandling.id)

        dao.hentUnntakForBehandling(opprettBehandling.id).single().asClue {
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
    inner class SlettAktivitet {
        private val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        private val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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

            dao.upsertUnntak(lagreUnntak, sak.id, kilde, null, opprettBehandling.id)

            val unntak = dao.hentUnntakForBehandling(opprettBehandling.id).single()
            dao.slettUnntakForBehandling(unntak.id, opprettBehandling.id)

            dao.hentUnntakForBehandling(opprettBehandling.id) shouldBe emptyList()
        }

        @Test
        fun `skal ikke slette unntak hvis behandling id ikke stemmer`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.upsertUnntak(lagreUnntak, sak.id, kilde, null, opprettBehandling.id)

            val unntak = dao.hentUnntakForBehandling(opprettBehandling.id).single()
            assertThrows<InternfeilException> {
                dao.slettUnntakForBehandling(unntak.id, UUID.randomUUID())
            }

            dao.hentUnntakForBehandling(opprettBehandling.id).single().asClue {
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
        private val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)

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

            dao.upsertUnntak(lagreUnntak, sak.id, kilde, null, forrigeBehandling.id)

            val unntak = dao.hentUnntakForBehandling(forrigeBehandling.id).single()
            dao.hentUnntakForBehandling(nyBehandling.id) shouldBe emptyList()

            dao.kopierUnntakTilBehandling(unntak.id, nyBehandling.id) shouldBe 1

            dao.hentUnntakForBehandling(nyBehandling.id).single().asClue {
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
