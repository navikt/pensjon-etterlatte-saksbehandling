package behandling.aktivitetsplikt

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktAktivitetType
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.OpprettAktivitetspliktAktivitet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktDaoTest(ds: DataSource) {
    private val dao = AktivitetspliktDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))

    @Test
    fun `skal hente aktiviteter for behandling`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivtet = opprettAktivitet(sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
        dao.opprettAktivitet(behandlingId, nyAktivtet, kilde)
        dao.opprettAktivitet(UUID.randomUUID(), nyAktivtet, kilde)

        val aktiviteter = dao.hentAktiviteter(behandlingId)

        aktiviteter.size shouldBe 1
    }

    @Test
    fun `skal opprette ny aktivetet`() {
        val behandlingId = UUID.randomUUID()
        val nyAktivitet = opprettAktivitet(sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))

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

    @Nested
    inner class SlettAktivitet {
        @Test
        fun `skal slette aktivetet`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val aktivitet = dao.hentAktiviteter(behandlingId).first()

            dao.slettAktivitet(aktivitet.id, behandlingId)

            dao.hentAktiviteter(behandlingId) shouldHaveSize 0
        }

        @Test
        fun `skal ikke slette aktivetet hvis behandlingId ikke stemmer`() {
            val behandlingId = UUID.randomUUID()
            val nyAktivitet = opprettAktivitet(sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000"))
            dao.opprettAktivitet(behandlingId, nyAktivitet, kilde)
            val aktivitet = dao.hentAktiviteter(behandlingId).first()

            dao.slettAktivitet(aktivitet.id, UUID.randomUUID())

            dao.hentAktiviteter(behandlingId) shouldHaveSize 1
        }
    }

    companion object {
        fun opprettAktivitet(sak: Sak) =
            OpprettAktivitetspliktAktivitet(
                sakId = sak.id,
                type = AktivitetspliktAktivitetType.ARBEIDSTAKER,
                fom = LocalDate.now(),
                beskrivelse = "Beskrivelse",
            )

        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
    }
}
