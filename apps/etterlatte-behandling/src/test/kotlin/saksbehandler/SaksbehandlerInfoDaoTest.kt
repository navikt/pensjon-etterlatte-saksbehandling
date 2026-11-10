package no.nav.etterlatte.saksbehandler

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.lagNyOppgave
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class SaksbehandlerInfoDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var saksbehandlerInfoDao: SaksbehandlerInfoDao

    @BeforeAll
    fun beforeAll() {
        saksbehandlerInfoDao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))
    }

    @Test
    fun `Kan hente navn for saksbehandler`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        val saksbehandlerNavn = saksbehandlerInfoDao.hentSaksbehandlerNavn(sbporsgrunn.ident)
        saksbehandlerNavn shouldBe sbporsgrunn.navn
    }

    @Test
    fun `Kan hente alle saksbehandlere sine identer`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)

        val sbaalesundfinnes = saksbehandlerInfoDao.saksbehandlerFinnes(sbaalesund.ident)
        sbaalesundfinnes shouldBe true

        val sbporsgrunnfinnes = saksbehandlerInfoDao.saksbehandlerFinnes(sbporsgrunn.ident)
        sbporsgrunnfinnes shouldBe true

        val randomidentfinnesikke = saksbehandlerInfoDao.saksbehandlerFinnes("ident")
        randomidentfinnesikke shouldBe false
    }

    @Test
    fun `Kan hente alle saksbehandlere sine identer og navn`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn2")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)

        val allesaksbehandlere = saksbehandlerInfoDao.hentAlleSaksbehandlere()
        allesaksbehandlere.size shouldBe 2
        allesaksbehandlere shouldContain sbporsgrunn
        allesaksbehandlere shouldContain sbaalesund
    }

    @Test
    fun `Finner saksbehandlere med riktig enhet`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.navn)),
        )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbaalesund.ident to listOf(SaksbehandlerEnhet(Enheter.AALESUND.enhetNr, Enheter.AALESUND.navn)),
        )

        val saksBehandlereMedAalesundEnhet = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enheter.AALESUND.enhetNr)
        Assertions.assertEquals(1, saksBehandlereMedAalesundEnhet.size)
        Assertions.assertEquals(sbaalesund.ident, saksBehandlereMedAalesundEnhet[0].ident)

        val porsgrunn = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enheter.PORSGRUNN.enhetNr)
        Assertions.assertEquals(1, porsgrunn.size)
        Assertions.assertEquals(sbporsgrunn.ident, porsgrunn[0].ident)

        val ingenSteinkjerSaksbehandlere = saksbehandlerInfoDao.hentSaksbehandlereForEnhet(Enheter.STEINKJER.enhetNr)
        Assertions.assertEquals(0, ingenSteinkjerSaksbehandlere.size)
    }

    @Test
    fun `Finner saksbehandlere med aapne oppgaver i enhetene, uavhengig av lagrede enheter`() {
        val oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        val sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakPorsgrunn = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)

        val iPermisjon = SaksbehandlerInfo("identporsgrunn", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(iPermisjon)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(iPermisjon.ident to emptyList())

        fun opprettOppgave(
            sak: Sak,
            saksbehandler: String,
            status: Status,
        ) {
            val oppgave = lagNyOppgave(sak)
            oppgaveDao.opprettOppgave(oppgave)
            oppgaveDao.settNySaksbehandler(oppgave.id, saksbehandler)
            oppgaveDao.oppdaterStatusOgMerknad(oppgave.id, "", status)
        }

        opprettOppgave(sakAalesund, iPermisjon.ident, Status.UNDER_BEHANDLING)
        opprettOppgave(sakAalesund, iPermisjon.ident, Status.PAA_VENT)
        opprettOppgave(sakAalesund, "utennavn", Status.NY)
        opprettOppgave(sakAalesund, "ferdigstilt", Status.FERDIGSTILT)
        opprettOppgave(sakPorsgrunn, "annenenhet", Status.NY)

        val saksbehandlere = saksbehandlerInfoDao.hentSaksbehandlereMedAapneOppgaver(listOf(Enheter.AALESUND.enhetNr))

        saksbehandlere shouldContainExactlyInAnyOrder
            listOf(
                iPermisjon,
                SaksbehandlerInfo("utennavn", "utennavn"),
            )
    }

    @Test
    fun `Kan hente enheter for saksbehandlerident`() {
        val sbporsgrunn = SaksbehandlerInfo("identporsgrunn", "navn")
        val sbaalesund = SaksbehandlerInfo("identaalesund", "navn")
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbporsgrunn)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(sbaalesund)
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(
            sbporsgrunn.ident to listOf(SaksbehandlerEnhet(Enheter.PORSGRUNN.enhetNr, Enheter.PORSGRUNN.navn)),
        )

        val enheterAalesundSaksbehandler =
            listOf(
                SaksbehandlerEnhet(Enheter.AALESUND.enhetNr, Enheter.AALESUND.navn),
                SaksbehandlerEnhet(Enheter.AALESUND_UTLAND.enhetNr, Enheter.AALESUND_UTLAND.navn),
            )
        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(sbaalesund.ident to enheterAalesundSaksbehandler)

        val hentetEnheterForAalesundSB = saksbehandlerInfoDao.hentSaksbehandlerEnheter(sbaalesund.ident)
        hentetEnheterForAalesundSB shouldContainExactly enheterAalesundSaksbehandler
    }
}
