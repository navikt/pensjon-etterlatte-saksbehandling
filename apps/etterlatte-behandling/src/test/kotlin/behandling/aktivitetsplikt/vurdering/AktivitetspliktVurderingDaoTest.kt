package behandling.aktivitetsplikt.vurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktVurderingDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktVurderingType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktVurdering
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktVurderingDaoTest(ds: DataSource) {
    private val dao = AktivitetspliktVurderingDao(ConnectionAutoclosingTest(ds))
    private val sakDao = SakDao(ConnectionAutoclosingTest(ds))

    @Test
    fun `skal lagre ned og hente opp en ny vurdering`() {
        val behandlingId = UUID.randomUUID()
        val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
        val oppgaveId = UUID.randomUUID()
        val sak = sakDao.opprettSak("Person1", SakType.OMSTILLINGSSTOENAD, "0000")
        val vurdering =
            LagreAktivitetspliktVurdering(
                vurdering = AktivitetspliktVurderingType.AKTIVITET_50,
                beskrivelse = "Beskrivelse",
            )

        dao.opprettVurdering(vurdering, sak.id, kilde, oppgaveId, behandlingId)

        dao.hentVurdering(oppgaveId).asClue {
            it.sakId shouldBe sak.id
            it.behandlingId shouldBe behandlingId
            it.oppgaveId shouldBe oppgaveId
            it.vurdering shouldBe vurdering.vurdering
            it.fom shouldNotBe null
            it.opprettet shouldBe kilde
            it.endret shouldBe kilde
            it.beskrivelse shouldBe vurdering.beskrivelse
        }
    }
}
