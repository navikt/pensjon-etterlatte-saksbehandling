package no.nav.etterlatte.soeknadsmottak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.vedtaksvurdering.routes.withSerializableRetry
import no.nav.etterlatte.common.ConnectionAutoclosingImpl
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.MottattSoeknadType
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringResponse
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
internal class SoeknadsmottakServiceTest(
    private val dataSource: DataSource,
) {
    private val autoclosing = ConnectionAutoclosingImpl(dataSource)
    private val sakLesDao = SakLesDao(autoclosing)
    private val sakSkrivDao = SakSkrivDao(SakendringerDao(autoclosing))
    private val sakService = mockk<SakService>()
    private val oppgaveService =
        OppgaveService(
            OppgaveDaoMedEndringssporingImpl(OppgaveDaoImpl(autoclosing), autoclosing),
            sakLesDao,
            HendelseDao(autoclosing),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    private val service = SoeknadsmottakService(sakService, oppgaveService, SoeknadOverleveringDao(autoclosing))

    private lateinit var sak: Sak

    // Databasen tømmes bare mellom testklasser, så hver test får egne nøkler.
    private val noekkel = "innsendt-soeknad:${java.util.UUID.randomUUID()}"
    private val jp1 = "jp-${java.util.UUID.randomUUID()}"
    private val jp2 = "jp-${java.util.UUID.randomUUID()}"

    @BeforeEach
    fun setUp() {
        brukKontekst()
        sak = inTransaction { sakSkrivDao.opprettSak("24876696580", SakType.OMSTILLINGSSTOENAD, Enheter.AALESUND.enhetNr) }
        every { sakService.finnSak(sak.id) } returns sak
    }

    @AfterEach
    fun tearDown() = Kontekst.remove()

    @Test
    fun `ugyldig og manglende fnr gir ukjent bruker paa standardenhet uten sak`() {
        listOf(null, "123", "00000000000").forEach { fnr ->
            val svar = service.journalfoeringskontekst(JournalfoeringskontekstRequest(MottattSoeknadType.UTVIDET_OMSTILLINGSSTOENAD, fnr))
            svar.ukjentBruker shouldBe true
            svar.sakId shouldBe null
            svar.tema shouldBe "EYO"
            svar.journalfoerendeEnhet shouldBe Enheter.defaultEnhet.enhetNr
        }
        verify(exactly = 0) { sakService.finnEllerOpprettSakMedGrunnlag(any(), any(), any()) }
    }

    @Test
    fun `gyldig fnr finner eller oppretter OMS-sak og bruker sakens enhet`() {
        every { sakService.finnEllerOpprettSakMedGrunnlag(sak.ident, SakType.OMSTILLINGSSTOENAD, null) } returns sak

        val svar =
            service.journalfoeringskontekst(JournalfoeringskontekstRequest(MottattSoeknadType.UTVIDET_OMSTILLINGSSTOENAD, sak.ident))

        svar.ukjentBruker shouldBe false
        svar.sakId shouldBe sak.id
        svar.sakType shouldBe SakType.OMSTILLINGSSTOENAD
        svar.journalfoerendeEnhet shouldBe Enheter.AALESUND.enhetNr
    }

    @Test
    fun `overlevering oppretter en TILLEGGSINFORMASJON-oppgave og gir samme svar ved retry`() {
        val foerste = inTransaction { service.overlever(request()) }
        val andre = inTransaction { service.overlever(request()) }

        andre shouldBe foerste
        val opprettet = oppgaver(jp1)
        opprettet.size shouldBe 1
        with(opprettet.single()) {
            id.toString() shouldBe foerste.oppgaveId
            type shouldBe OppgaveType.TILLEGGSINFORMASJON
            kilde shouldBe OppgaveKilde.BRUKERDIALOG
            merknad shouldBe SoeknadsmottakService.MERKNAD_UTVIDET_OMS
            sakId shouldBe sak.id
        }
    }

    @Test
    fun `samme noekkel med annet innhold avvises`() {
        inTransaction { service.overlever(request()) }

        assertThrows<MotstridendeOverlevering> { inTransaction { service.overlever(request(journalpostId = jp2)) } }
        assertThrows<MotstridendeOverlevering> { inTransaction { service.overlever(request(dokumentInfoIder = listOf("9"))) } }
        oppgaver(jp2) shouldBe emptyList()
    }

    @Test
    fun `parallelle like overleveringer gir en oppgave og samme svar`() {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(4)
        val svar =
            (1..4)
                .map {
                    pool.submit(
                        Callable<SoeknadOverleveringResponse> {
                            brukKontekst()
                            start.await()
                            try {
                                withSerializableRetry { inTransaction { service.overlever(request()) } }
                            } finally {
                                Kontekst.remove()
                            }
                        },
                    )
                }.also { start.countDown() }
                .map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        svar.distinct().size shouldBe 1
        oppgaver(jp1).size shouldBe 1
    }

    @Test
    fun `feil etter at oppgaven er opprettet ruller tilbake noekkel og oppgave`() {
        val feilendeDao =
            spyk(SoeknadOverleveringDao(autoclosing)) {
                every { lagreOppgave(any(), any()) } throws IllegalStateException("feil etter oppgave")
            }
        val feilende = SoeknadsmottakService(sakService, oppgaveService, feilendeDao)

        assertThrows<IllegalStateException> { inTransaction { feilende.overlever(request()) } }
        oppgaver(jp1) shouldBe emptyList()

        inTransaction { service.overlever(request()) }
        oppgaver(jp1).size shouldBe 1
    }

    @Test
    fun `sak med feil saktype eller ukjent sak avvises`() {
        val bp = inTransaction { sakSkrivDao.opprettSak("24876696580", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr) }
        every { sakService.finnSak(bp.id) } returns bp
        every {
            sakService.finnSak(
                no.nav.etterlatte.libs.common.sak
                    .SakId(999),
            )
        } returns null

        assertThrows<UgyldigForespoerselException> { inTransaction { service.overlever(request(sakId = bp.id)) } }
        assertThrows<UgyldigForespoerselException> {
            inTransaction {
                service.overlever(
                    request(
                        sakId =
                            no.nav.etterlatte.libs.common.sak
                                .SakId(999),
                    ),
                )
            }
        }
    }

    private fun oppgaver(referanse: String) = inTransaction { oppgaveService.hentOppgaverForReferanse(referanse) }

    private fun brukKontekst() = nyKontekstMedBrukerOgDatabaseContext(Self("soeknadsmottak-test"), DatabaseContext(dataSource))

    private fun request(
        journalpostId: String = jp1,
        dokumentInfoIder: List<String> = listOf("111"),
        sakId: no.nav.etterlatte.libs.common.sak.SakId = sak.id,
    ) = SoeknadOverleveringRequest(
        mottaksnoekkel = noekkel,
        soeknadType = MottattSoeknadType.UTVIDET_OMSTILLINGSSTOENAD,
        sakId = sakId,
        journalpostId = journalpostId,
        dokumentInfoIder = dokumentInfoIder,
    )
}
