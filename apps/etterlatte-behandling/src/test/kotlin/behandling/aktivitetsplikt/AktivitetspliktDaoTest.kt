package no.nav.etterlatte.behandling.aktivitetsplikt

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktDao(ConnectionAutoclosingTest(ds))
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)))

    @Test
    fun `skal opprette ny aktivitet for behandling`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))

        dao.opprettAktivitetForBehandling(behandlingId, nyAktivitet, kilde) shouldBe 1
        dao.opprettAktivitetForBehandling(UUID.randomUUID(), nyAktivitet, kilde)

        val hentAktiviteter = dao.hentAktiviteterForBehandling(behandlingId)
        hentAktiviteter.size shouldBe 1

        hentAktiviteter.first().asClue { aktivitet ->
            aktivitet.sakId shouldBe nyAktivitet.sakId
            aktivitet.type shouldBe nyAktivitet.type
            aktivitet.fom shouldBe nyAktivitet.fom
            aktivitet.tom shouldBe nyAktivitet.tom
            aktivitet.opprettet shouldBe kilde
            aktivitet.endret shouldBe kilde
            aktivitet.beskrivelse shouldBe nyAktivitet.beskrivelse
        }
    }

    @Test
    fun `skal opprette ny aktivitet for sak`() {
        val sak =
            sakSkrivDao.opprettSak(
                "Person1",
                SakType.OMSTILLINGSSTOENAD,
                Enheter.defaultEnhet.enhetNr,
            )
        val nyAktivitet = opprettAktivitet(sak)
        dao.opprettAktivitetForSak(sak.id, nyAktivitet, kilde) shouldBe 1
        dao.hentAktiviteterForSak(sak.id).asClue { aktiviteter ->
            aktiviteter.size shouldBe 1
            aktiviteter.first().asClue { aktivitet ->
                aktivitet.sakId shouldBe sak.id
                aktivitet.type shouldBe nyAktivitet.type
                aktivitet.fom shouldBe nyAktivitet.fom
                aktivitet.tom shouldBe nyAktivitet.tom
                aktivitet.opprettet shouldBe kilde
            }
        }
    }

    @Test
    fun `skal opprette ny hendelse`() {
        val behandlingId = UUID.randomUUID()
        val nyHendelse = opprettHendelse(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))

        dao.upsertHendelse(behandlingId, nyHendelse, kilde) shouldBe 1

        val hentHendelser = dao.hentHendelserForBehandling(behandlingId)
        hentHendelser.first().asClue { hendelse ->
            hendelse.sakId shouldBe nyHendelse.sakId
            hendelse.dato shouldBe nyHendelse.dato
            hendelse.opprettet shouldBe kilde
            hendelse.endret shouldBe kilde
            hendelse.beskrivelse shouldBe nyHendelse.beskrivelse
            hendelse.behandlingId shouldBe behandlingId
        }
    }

    @Test
    fun `skal opprette ny hendelse for Sak`() {
        val sak = sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val nyHendelse = opprettHendelse(sak)

        dao.upsertHendelse(null, nyHendelse, kilde) shouldBe 1

        val hentHendelser = dao.hentHendelserForSak(sak.id)
        hentHendelser.first().asClue { hendelse ->
            hendelse.sakId shouldBe nyHendelse.sakId
            hendelse.dato shouldBe nyHendelse.dato
            hendelse.opprettet shouldBe kilde
            hendelse.endret shouldBe kilde
            hendelse.beskrivelse shouldBe nyHendelse.beskrivelse
            hendelse.behandlingId shouldBe null
        }
    }

    @Nested
    inner class OppdaterHendelse {
        @Test
        fun `skal oppdatere eksisterende hendelse`() {
            val behandlingId = UUID.randomUUID()
            val nyHendelse = opprettHendelse(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.upsertHendelse(behandlingId, nyHendelse, kilde)
            val gammelHendelse = dao.hentHendelserForBehandling(behandlingId).first()
            val oppdaterHendelse =
                nyHendelse.copy(
                    id = gammelHendelse.id,
                    dato = gammelHendelse.dato.plusYears(1),
                    beskrivelse = "Ny beskrivelse",
                )
            dao.upsertHendelse(
                behandlingId,
                oppdaterHendelse,
                kilde.copy("Z1111111"),
            ) shouldBe 1

            val hendelser = dao.hentHendelserForBehandling(behandlingId)
            hendelser shouldHaveSize 1
            hendelser.first().asClue { oppdatertAktivitet ->
                oppdatertAktivitet.id shouldBe gammelHendelse.id
                oppdatertAktivitet.sakId shouldBe gammelHendelse.sakId
                oppdatertAktivitet.dato shouldBe oppdaterHendelse.dato
                oppdatertAktivitet.opprettet shouldBe gammelHendelse.opprettet
                oppdatertAktivitet.endret shouldNotBe gammelHendelse.endret
                oppdatertAktivitet.beskrivelse shouldBe oppdaterHendelse.beskrivelse
            }
        }
    }

    @Nested
    inner class OppdaterAktivitet {
        @Test
        fun `skal oppdatere eksisterende aktivitet`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.opprettAktivitetForBehandling(behandlingId, nyAktivitet, kilde)
            val gammelAktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()
            val oppdaterAktivitet =
                nyAktivitet.copy(
                    id = gammelAktivitet.id,
                    type = AktivitetspliktAktivitetType.UTDANNING,
                    fom = gammelAktivitet.fom.plusYears(1),
                    tom = LocalDate.now(),
                    beskrivelse = "Ny beskrivelse",
                )
            dao.oppdaterAktivitetForBehandling(
                behandlingId,
                oppdaterAktivitet,
                kilde.copy("Z1111111"),
            ) shouldBe 1

            val aktiviteter = dao.hentAktiviteterForBehandling(behandlingId)
            aktiviteter shouldHaveSize 1
            aktiviteter.first().asClue { oppdatertAktivitet ->
                oppdatertAktivitet.id shouldBe gammelAktivitet.id
                oppdatertAktivitet.sakId shouldBe gammelAktivitet.sakId
                oppdatertAktivitet.type shouldBe oppdaterAktivitet.type
                oppdatertAktivitet.fom shouldBe oppdaterAktivitet.fom
                oppdatertAktivitet.tom shouldBe oppdaterAktivitet.tom
                oppdatertAktivitet.opprettet shouldBe gammelAktivitet.opprettet
                oppdatertAktivitet.endret shouldNotBe gammelAktivitet.endret
                oppdatertAktivitet.beskrivelse shouldBe oppdaterAktivitet.beskrivelse
            }
        }

        @Test
        fun `skal ikke oppdatere eksisterende aktivitet hvis behandling id ikke stemmer`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.opprettAktivitetForBehandling(behandlingId, nyAktivitet, kilde)
            val gammelAktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()
            val oppdaterAktivitet =
                nyAktivitet.copy(
                    id = gammelAktivitet.id,
                    type = AktivitetspliktAktivitetType.UTDANNING,
                    fom = gammelAktivitet.fom.plusYears(1),
                    tom = LocalDate.now(),
                    beskrivelse = "Ny beskrivelse",
                )

            assertThrows<InternfeilException> {
                dao.oppdaterAktivitetForBehandling(
                    UUID.randomUUID(),
                    oppdaterAktivitet,
                    kilde.copy("Z1111111"),
                )
            }
        }
    }

    @Nested
    inner class SlettAktivitet {
        @Test
        fun `skal slette aktivitet`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.opprettAktivitetForBehandling(behandlingId, nyAktivitet, kilde)
            val aktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()

            dao.slettAktivitetForBehandling(aktivitet.id, behandlingId)

            dao.hentAktiviteterForBehandling(behandlingId) shouldHaveSize 0
        }

        @Test
        fun `skal ikke slette aktivitet hvis behandling id ikke stemmer`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.opprettAktivitetForBehandling(behandlingId, nyAktivitet, kilde)

            dao.hentAktiviteterForBehandling(behandlingId) shouldHaveSize 1
        }
    }

    @Nested
    inner class SlettHendelse {
        @Test
        fun `skal slette hendelse`() {
            val behandlingId = UUID.randomUUID()
            val nyHendelse = opprettHendelse(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.upsertHendelse(behandlingId, nyHendelse, kilde)
            val hendelse = dao.hentHendelserForBehandling(behandlingId).first()

            dao.slettHendelse(hendelse.id)

            dao.hentHendelserForBehandling(behandlingId) shouldHaveSize 0
        }
    }

    @Nested
    inner class KopierAktiviteter {
        @Test
        fun `skal kopiere aktiviteter fra tidligere behandling`() {
            val forrigeBehandling = UUID.randomUUID()
            val nyBehandling = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.opprettAktivitetForBehandling(forrigeBehandling, nyAktivitet, kilde)
            dao.opprettAktivitetForBehandling(forrigeBehandling, nyAktivitet, kilde)
            dao.opprettAktivitetForBehandling(forrigeBehandling, nyAktivitet, kilde)
            dao.hentAktiviteterForBehandling(forrigeBehandling) shouldHaveSize 3
            dao.hentAktiviteterForBehandling(nyBehandling) shouldHaveSize 0

            dao.kopierAktiviteter(forrigeBehandling, nyBehandling) shouldBe 3

            dao.hentAktiviteterForBehandling(nyBehandling).asClue {
                it shouldHaveSize 3
                it.forEach { aktivitet ->
                    aktivitet.sakId shouldBe nyAktivitet.sakId
                    aktivitet.type shouldBe nyAktivitet.type
                    aktivitet.fom shouldBe nyAktivitet.fom
                    aktivitet.tom shouldBe nyAktivitet.tom
                    aktivitet.opprettet shouldBe kilde
                    aktivitet.endret shouldBe kilde
                    aktivitet.beskrivelse shouldBe nyAktivitet.beskrivelse
                }
            }
        }
    }

    @Nested
    inner class KopierHendelser {
        @Test
        fun `skal kopiere hendelser fra tidligere behandling`() {
            val forrigeBehandling = UUID.randomUUID()
            val nyBehandling = UUID.randomUUID()
            val nyHendelse = opprettHendelse(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr))
            dao.upsertHendelse(forrigeBehandling, nyHendelse, kilde)
            dao.upsertHendelse(forrigeBehandling, nyHendelse, kilde)
            dao.upsertHendelse(forrigeBehandling, nyHendelse, kilde)
            dao.hentHendelserForBehandling(forrigeBehandling) shouldHaveSize 3
            dao.hentHendelserForBehandling(nyBehandling) shouldHaveSize 0

            dao.kopierHendelser(forrigeBehandling, nyBehandling) shouldBe 3

            dao.hentHendelserForBehandling(nyBehandling).asClue {
                it shouldHaveSize 3
                it.forEach { aktivitet ->
                    aktivitet.sakId shouldBe nyHendelse.sakId
                    aktivitet.dato shouldBe nyHendelse.dato
                    aktivitet.opprettet shouldBe kilde
                    aktivitet.endret shouldBe kilde
                    aktivitet.beskrivelse shouldBe nyHendelse.beskrivelse
                }
            }
        }
    }

    companion object {
        fun opprettAktivitet(sak: Sak) =
            LagreAktivitetspliktAktivitet(
                sakId = sak.id,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        fun opprettHendelse(sak: Sak) =
            LagreAktivitetspliktHendelse(
                sakId = sak.id,
                dato = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
    }
}
