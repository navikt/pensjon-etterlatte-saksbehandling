package no.nav.etterlatte.behandling.aktivitetsplikt

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(ds)))
    private val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(ds))

    @Test
    fun `Kan lagre, hente og oppdatere brevdata for aktivitetsplikt`() {
        val sak = sakSkrivDao.opprettSak("person", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }

        val brevdata =
            AktivitetspliktInformasjonBrevdata(
                sakid = sak.id,
                oppgaveId = oppgave.id,
                skalSendeBrev = true,
                utbetaling = true,
                redusertEtterInntekt = true,
                kilde = Grunnlagsopplysning.Saksbehandler.create("ident"),
                spraak = Spraak.NB,
            )

        dao.lagreBrevdata(brevdata)
        val lagretBrevdata = dao.hentBrevdata(oppgave.id)
        lagretBrevdata shouldBe brevdata

        val oppdatertBrevdata = brevdata.copy(utbetaling = false)
        dao.lagreBrevdata(oppdatertBrevdata)
        val oppdatertBredata = dao.hentBrevdata(oppgave.id)
        oppdatertBredata shouldBe oppdatertBrevdata

        dao.hentBrevdata(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Kan lagre hente og sette brevid`() {
        val sak = sakSkrivDao.opprettSak("person", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }

        val brevdata =
            AktivitetspliktInformasjonBrevdata(
                sakid = sak.id,
                oppgaveId = oppgave.id,
                skalSendeBrev = true,
                utbetaling = false,
                redusertEtterInntekt = false,
                kilde = Grunnlagsopplysning.Saksbehandler.create("ident"),
                spraak = Spraak.NB,
            )

        dao.lagreBrevdata(brevdata)
        val lagretBrevdata = dao.hentBrevdata(oppgave.id)
        lagretBrevdata shouldBe brevdata

        val brevId = 1L
        dao.lagreBrevId(oppgave.id, brevId)
        val brevdataMedBrevId = dao.hentBrevdata(oppgave.id)
        brevdataMedBrevId?.brevId shouldBe brevId
    }

    @Test
    fun `Skal fungere med nullable for booleans`() {
        val sak = sakSkrivDao.opprettSak("person", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }

        val brevdata =
            AktivitetspliktInformasjonBrevdata(
                sakid = sak.id,
                oppgaveId = oppgave.id,
                skalSendeBrev = false,
                kilde = Grunnlagsopplysning.Saksbehandler.create("ident"),
                spraak = Spraak.NB,
            )

        dao.lagreBrevdata(brevdata)
        val lagretBrevdata = dao.hentBrevdata(oppgave.id)
        lagretBrevdata shouldBe brevdata
    }

    @Test
    fun `Kan fjerne brevid og kilde oppdaterer seg`() {
        val sak = sakSkrivDao.opprettSak("person", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val oppgave = lagNyOppgave(sak).also { oppgaveDao.opprettOppgave(it) }

        val sbIdent = "ident"
        val brevdata =
            AktivitetspliktInformasjonBrevdata(
                sakid = sak.id,
                oppgaveId = oppgave.id,
                skalSendeBrev = true,
                utbetaling = false,
                redusertEtterInntekt = false,
                kilde = Grunnlagsopplysning.Saksbehandler.create(sbIdent),
                spraak = Spraak.NB,
            )

        dao.lagreBrevdata(brevdata)
        val lagretBrevdata = dao.hentBrevdata(oppgave.id)
        lagretBrevdata shouldBe brevdata

        val brevId = 1L
        dao.lagreBrevId(oppgave.id, brevId)
        val brevdataMedBrevId = dao.hentBrevdata(oppgave.id)
        brevdataMedBrevId?.brevId shouldBe brevId

        val nyKilde = Grunnlagsopplysning.Saksbehandler.create(sbIdent)
        dao.fjernBrevId(oppgave.id, nyKilde)

        val dataMedFjernetBrevidOgNyKilde = dao.hentBrevdata(oppgave.id)
        dataMedFjernetBrevidOgNyKilde?.kilde shouldBe nyKilde
        dataMedFjernetBrevidOgNyKilde?.brevId shouldBe null
    }
}
