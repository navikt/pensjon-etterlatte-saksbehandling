package no.nav.etterlatte.behandling.aktivitetsplikt

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktDao(ConnectionAutoclosingTest(ds))
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)) { mockk() })

    @Test
    fun `skal hente aktiviteter for behandling`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivtet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
        dao.opprettAktivitet(behandlingId, nyAktivtet, kilde)
        dao.opprettAktivitet(UUID.randomUUID(), nyAktivtet, kilde)

        val aktiviteter = dao.hentAktiviteterForBehandling(behandlingId)

        aktiviteter.size shouldBe 1
    }

    @Test
    fun `skal opprette ny aktivetet`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))

        dao.opprettAktivitet(behandlingId, nyAktivitet, kilde) shouldBe 1

        val hentAktiviteter = dao.hentAktiviteterForBehandling(behandlingId)
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

    @Nested
    inner class OppdaterAktivitet {
        @Test
        fun `skal oppdatere eksisterende aktivitet`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val gammelAktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()
            val oppdaterAktivitet =
                nyAktivitet.copy(
                    id = gammelAktivitet.id,
                    type = AktivitetspliktAktivitetType.UTDANNING,
                    fom = gammelAktivitet.fom.plusYears(1),
                    tom = LocalDate.now(),
                    beskrivelse = "Ny beskrivelse",
                )
            dao.oppdaterAktivitet(
                behandlingId,
                oppdaterAktivitet,
                kilde.copy("Z1111111"),
            ) shouldBe 1

            val aktiviteter = dao.hentAktiviteterForBehandling(behandlingId)
            aktiviteter shouldHaveSize 1
            aktiviteter.first().asClue { oppdatertAktivitet ->
                oppdatertAktivitet.id shouldBe gammelAktivitet.id
                oppdatertAktivitet.sakId shouldBe gammelAktivitet.sakId
                oppdatertAktivitet.behandlingId shouldBe gammelAktivitet.behandlingId
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
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val gammelAktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()
            val oppdaterAktivitet =
                nyAktivitet.copy(
                    id = gammelAktivitet.id,
                    type = AktivitetspliktAktivitetType.UTDANNING,
                    fom = gammelAktivitet.fom.plusYears(1),
                    tom = LocalDate.now(),
                    beskrivelse = "Ny beskrivelse",
                )
            dao.oppdaterAktivitet(
                UUID.randomUUID(),
                oppdaterAktivitet,
                kilde.copy("Z1111111"),
            ) shouldBe 0
        }
    }

    @Nested
    inner class SlettAktivitet {
        @Test
        fun `skal slette aktivitet`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val aktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()

            dao.slettAktivitet(aktivitet.id, behandlingId)

            dao.hentAktiviteterForBehandling(behandlingId) shouldHaveSize 0
        }

        @Test
        fun `skal ikke slette aktivitet hvis behandling id ikke stemmer`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val aktivitet = dao.hentAktiviteterForBehandling(behandlingId).first()

            dao.slettAktivitet(aktivitet.id, UUID.randomUUID())

            dao.hentAktiviteterForBehandling(behandlingId) shouldHaveSize 1
        }
    }

    @Nested
    inner class KopierAktiviteter {
        @Test
        fun `skal kopiere aktiviteter fra tidligere behandling`() {
            val forrigeBehandling = UUID.randomUUID()
            val nyBehandling = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakSkrivDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(forrigeBehandling, nyAktivitet, kilde)
            dao.opprettAktivitet(forrigeBehandling, nyAktivitet, kilde)
            dao.opprettAktivitet(forrigeBehandling, nyAktivitet, kilde)
            dao.hentAktiviteterForBehandling(forrigeBehandling) shouldHaveSize 3
            dao.hentAktiviteterForBehandling(nyBehandling) shouldHaveSize 0

            dao.kopierAktiviteter(forrigeBehandling, nyBehandling) shouldBe 3

            dao.hentAktiviteterForBehandling(nyBehandling).asClue {
                it shouldHaveSize 3
                it.forEach { aktivitet ->
                    aktivitet.sakId shouldBe nyAktivitet.sakId
                    aktivitet.behandlingId shouldBe nyBehandling
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

    companion object {
        fun opprettAktivitet(sak: Sak) =
            LagreAktivitetspliktAktivitet(
                sakId = sak.id,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
    }
}
