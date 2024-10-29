package no.nav.etterlatte.behandling.aktivitetsplikt

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.brev.model.Aktivitetsgrad
import no.nav.etterlatte.brev.model.AktivitetspliktInformasjon10mndBrevdata
import no.nav.etterlatte.brev.model.NasjonalEllerUtland
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class AktivitetspliktBrevDaoTest(
    ds: DataSource,
) {
    private val dao = AktivitetspliktBrevDao(ConnectionAutoclosingTest(ds))
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)) { mockk() })
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))

    @Test
    fun `Kan lagre, hente og oppdatere brevdata for aktivitetsplikt`() {
        val brevdata =
            AktivitetspliktInformasjon10mndBrevdata(
                aktivitetsgrad = Aktivitetsgrad.AKKURAT_100_PROSENT,
                utbetaling = true,
                redusertEtterInntekt = true,
                nasjonalEllerUtland = NasjonalEllerUtland.NASJONAL,
            )
        val sak = sakSkrivDao.opprettSak("person", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }

        dao.lagreBrevdata(oppgave.id, sak.id, brevdata)
        val lagretBrevdata = dao.hentBrevdata(oppgave.id)
        lagretBrevdata shouldBe brevdata

        val oppdatertBrevdata = brevdata.copy(Aktivitetsgrad.IKKE_I_AKTIVITET)
        dao.lagreBrevdata(oppgave.id, sak.id, oppdatertBrevdata)
        val oppdatertBredata = dao.hentBrevdata(oppgave.id)
        oppdatertBredata shouldBe oppdatertBrevdata

        dao.hentBrevdata(UUID.randomUUID()) shouldBe null
    }
}
