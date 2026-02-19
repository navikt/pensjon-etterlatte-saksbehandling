package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDaoTest.Companion.kilde
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50
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
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktAktivitetsgradDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktAktivitetsgradDao(ConnectionAutoclosingTest(ds))
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)))
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
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentAktivitetsgradForOppgave(oppgave.id).single().asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
            it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
        }
    }

    @Test
    fun `Skal slette aktivitetsgrad for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
            )

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId = behandlingId)

        val hentetAktivitetsgradSomSkalSlettes = dao.hentAktivitetsgradForOppgave(oppgave.id).single()
        hentetAktivitetsgradSomSkalSlettes.asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
            it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
        }
        dao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgrad.copy(beskrivelse = "skal ikke bli slettet"),
            sak.id,
            kilde,
            oppgave.id,
            behandlingId = behandlingId,
        )

        dao.slettAktivitetsgradForOppgave(aktivitetId = hentetAktivitetsgradSomSkalSlettes.id, oppgaveId = oppgave.id)
        val aktivitetsgraderForOppgave = dao.hentAktivitetsgradForOppgave(oppgave.id)
        aktivitetsgraderForOppgave.size shouldBe 1
        aktivitetsgraderForOppgave.first().id shouldNotBe hentetAktivitetsgradSomSkalSlettes.id
    }

    @Test
    fun `skal kunne oppdatere aktivitetsgraden i en eksisterende vurdering for oppgave`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_UNDER_50,
                beskrivelse = "Beskrivelse",
                vurdertFra12Mnd = false,
                skjoennsmessigVurdering = null,
            )

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId)

        val lagretOppgave = dao.hentAktivitetsgradForOppgave(oppgave.id).single()

        val oppdatertAktivitetsgrad =
            aktivitetsgrad.copy(
                id = lagretOppgave.id,
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Ny beskrivelse",
                fom = aktivitetsgrad.fom.plusMonths(2L),
                tom = aktivitetsgrad.fom.plusMonths(5L),
                vurdertFra12Mnd = true,
                skjoennsmessigVurdering = AktivitetspliktSkjoennsmessigVurdering.JA,
            )
        dao.upsertAktivitetsgradForOppgaveEllerBehandling(oppdatertAktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId)
        val oppdatertLagretOppgave = dao.hentAktivitetsgradForOppgave(oppgave.id).single()

        oppdatertLagretOppgave.asClue {
            it.sakId shouldBe sak.id
            it.id shouldBe lagretOppgave.id
            it.aktivitetsgrad shouldBe oppdatertAktivitetsgrad.aktivitetsgrad
            it.beskrivelse shouldBe oppdatertAktivitetsgrad.beskrivelse
            it.fom shouldBe oppdatertAktivitetsgrad.fom
            it.tom shouldBe oppdatertAktivitetsgrad.tom
            it.vurdertFra12Mnd shouldBe oppdatertAktivitetsgrad.vurdertFra12Mnd
            it.skjoennsmessigVurdering shouldBe oppdatertAktivitetsgrad.skjoennsmessigVurdering
        }
    }

    @Test
    fun `skal lagre ned og hente opp en aktivitetsgrad for 12 mnd`() {
        val behandlingId = null
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
                skjoennsmessigVurdering = AktivitetspliktSkjoennsmessigVurdering.JA,
                vurdertFra12Mnd = true,
            )

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, oppgave.id, behandlingId)

        dao.hentAktivitetsgradForOppgave(oppgave.id).single().asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgave.id
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
            it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
        }
    }

    @Test
    fun `Skal hente alle lagrede aktivitetsgrader på siste behandling hvis oppgave er eldre`() {
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val kilde1 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().minus(2, ChronoUnit.HOURS))
        val kilde2 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val kilde3 = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().minus(3, ChronoUnit.DAYS))
        val aktivitetsgrad =
            LagreAktivitetspliktAktivitetsgrad(
                aktivitetsgrad = AKTIVITET_OVER_50,
                beskrivelse = "Beskrivelse",
                fom = LocalDate.of(2024, 4, 1),
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
                opphoer = null,
                opprinnelse = BehandlingOpprinnelse.AUTOMATISK_JOBB,
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
                referanse = behandling.id.toString(),
                gruppeId = null,
                merknad = null,
                opprettet = Tidspunkt.now(),
                sakType = SakType.OMSTILLINGSSTOENAD,
                fnr = sak.ident,
                frist = null,
            )
        oppgaveDao.opprettOppgave(oppgave)

        val behandlingId = behandling.id
        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde1, null, behandlingId)
        dao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgrad.copy(
                aktivitetsgrad = AKTIVITET_100,
                fom = LocalDate.of(2024, 7, 1),
            ),
            sak.id,
            kilde2,
            null,
            behandlingId,
        )
        dao.upsertAktivitetsgradForOppgaveEllerBehandling(
            aktivitetsgrad.copy(
                aktivitetsgrad = AKTIVITET_UNDER_50,
                fom = LocalDate.of(2024, 4, 1),
            ),
            sak.id,
            kilde3,
            oppgave.id,
            null,
        )
        val aktivitetsgrader = dao.hentNyesteAktivitetsgrad(sak.id)

        aktivitetsgrader.size shouldBe 2
        aktivitetsgrader[0].asClue {
            it.sakId shouldBe sak.id
            it.oppgaveId shouldBe null
            it.opprettet shouldBe kilde1
            it.endret shouldBe kilde1
            it.behandlingId shouldBe behandlingId
            it.aktivitetsgrad shouldBe AKTIVITET_OVER_50
            it.fom shouldNotBe null
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
        }
        aktivitetsgrader[1].asClue {
            it.sakId shouldBe sak.id
            it.oppgaveId shouldBe null
            it.behandlingId shouldBe behandlingId
            it.opprettet shouldBe kilde2
            it.endret shouldBe kilde2
            it.aktivitetsgrad shouldBe AKTIVITET_100
            it.fom shouldNotBe null
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
        }
    }

    @Test
    fun `skal lagre ned og hente opp en ny aktivitetsgrad for behandling`() {
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

        dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single().asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe opprettBehandling.id
            it.oppgaveId shouldBe null
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
            it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
        }
    }

    @Test
    fun `skal lagre ned og hente opp en ny aktivitetsgrad 12mnd for behandling`() {
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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
                skjoennsmessigVurdering = AktivitetspliktSkjoennsmessigVurdering.NEI,
                vurdertFra12Mnd = true,
            )

        dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

        dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single().asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe opprettBehandling.id
            it.oppgaveId shouldBe null
            it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
            it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
            it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
        }
    }

    @Nested
    inner class OppdaterAktivitetsgrad {
        @Test
        fun `Oppdatere aktivitetsgrad`() {
            val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
            val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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

            dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single()

            val aktivitetsgradMedId =
                LagreAktivitetspliktAktivitetsgrad(
                    id = aktivitet.id,
                    aktivitetsgrad = AKTIVITET_100,
                    beskrivelse = "Beskrivelse er oppdatert",
                )

            dao.oppdaterAktivitetsgrad(aktivitetsgradMedId, kilde, opprettBehandling.id)

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single().asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.aktivitetsgrad shouldBe AKTIVITET_100
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgradMedId.beskrivelse
                it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
                it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
            }
        }

        @Test
        fun `oppdaterer med felter for 12 mnd`() {
            val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
            val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
            val opprettBehandling =
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak.id,
                )
            behandlingDao.opprettBehandling(opprettBehandling)
            val aktivitetsgrad =
                LagreAktivitetspliktAktivitetsgrad(
                    aktivitetsgrad = AKTIVITET_UNDER_50,
                    beskrivelse = "Beskrivelse",
                )

            dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single()

            val aktivitetsgradMedId =
                LagreAktivitetspliktAktivitetsgrad(
                    id = aktivitet.id,
                    aktivitetsgrad = AKTIVITET_100,
                    beskrivelse = "Beskrivelse er oppdatert",
                    skjoennsmessigVurdering = null,
                    vurdertFra12Mnd = true,
                )

            dao.oppdaterAktivitetsgrad(aktivitetsgradMedId, kilde, opprettBehandling.id)

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single().asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe opprettBehandling.id
                it.aktivitetsgrad shouldBe AKTIVITET_100
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgradMedId.beskrivelse
                it.vurdertFra12Mnd shouldBe aktivitetsgradMedId.vurdertFra12Mnd
                it.skjoennsmessigVurdering shouldBe aktivitetsgradMedId.skjoennsmessigVurdering
            }
        }
    }

    @Nested
    inner class SlettAktivitetsgrad {
        private val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        private val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
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

            dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single()
            dao.slettAktivitetsgradForBehandling(aktivitet.id, opprettBehandling.id)

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id) shouldBe emptyList()
        }

        @Test
        fun `skal ikke slette aktivitetsgrad hvis behandling id ikke stemmer`() {
            behandlingDao.opprettBehandling(opprettBehandling)

            dao.upsertAktivitetsgradForOppgaveEllerBehandling(aktivitetsgrad, sak.id, kilde, null, opprettBehandling.id)

            val aktivitet = dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single()
            dao.slettAktivitetsgradForBehandling(aktivitet.id, UUID.randomUUID())

            dao.hentAktivitetsgradForBehandling(opprettBehandling.id).single().asClue {
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
        private val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)

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

            dao.upsertAktivitetsgradForOppgaveEllerBehandling(lagreAktivitetsgrad, sak.id, kilde, null, forrigeBehandling.id)

            val aktivitetsgrad = dao.hentAktivitetsgradForBehandling(forrigeBehandling.id).single()
            dao.hentAktivitetsgradForBehandling(nyBehandling.id) shouldBe emptyList()

            dao.kopierAktivitetsgradTilBehandling(aktivitetsgrad.id, nyBehandling.id) shouldBe 1

            dao.hentAktivitetsgradForBehandling(nyBehandling.id).single().asClue {
                it.sakId shouldBe sak.id
                it.behandlingId shouldBe nyBehandling.id
                it.aktivitetsgrad shouldBe aktivitetsgrad.aktivitetsgrad
                it.fom shouldBe aktivitetsgrad.fom
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
                it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
                it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
            }
        }

        @Test
        fun `skal kopiere nyeste aktivitetsgrad til oppgave`() {
            behandlingDao.opprettBehandling(forrigeBehandling)
            dao.upsertAktivitetsgradForOppgaveEllerBehandling(lagreAktivitetsgrad, sak.id, kilde, null, forrigeBehandling.id)

            val aktivitetsgrad = dao.hentAktivitetsgradForBehandling(forrigeBehandling.id).single()
            val oppgave =
                lagNyOppgave(
                    sak =
                        Sak(
                            ident = "",
                            sakType = SakType.OMSTILLINGSSTOENAD,
                            id = forrigeBehandling.sakId,
                            enhet = Enheter.defaultEnhet.enhetNr,
                            adressebeskyttelse = null,
                            erSkjermet = false,
                        ),
                    oppgaveType = OppgaveType.AKTIVITETSPLIKT,
                )
            oppgaveDao.opprettOppgave(oppgave)
            dao.kopierAktivitetsgradTilOppgave(aktivitetsgrad.id, oppgaveId = oppgave.id)
            dao.hentAktivitetsgradForOppgave(oppgave.id).single().asClue {
                it.sakId shouldBe aktivitetsgrad.sakId
                it.behandlingId shouldBe null
                it.oppgaveId shouldBe oppgave.id
                it.fom shouldBe aktivitetsgrad.fom
                it.opprettet shouldBe kilde
                it.endret shouldBe kilde
                it.beskrivelse shouldBe aktivitetsgrad.beskrivelse
                it.vurdertFra12Mnd shouldBe aktivitetsgrad.vurdertFra12Mnd
                it.skjoennsmessigVurdering shouldBe aktivitetsgrad.skjoennsmessigVurdering
            }
        }
    }
}
