package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.soeker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EtteroppgjoerJobServiceTest : BehandlingIntegrationTest() {
    val defaultPersongalleriGydligeFnr =
        Persongalleri(
            soeker,
            INNSENDER_FOEDSELSNUMMER.value,
            listOf(HELSOESKEN2_FOEDSELSNUMMER.value, HALVSOESKEN_FOEDSELSNUMMER.value),
            listOf(AVDOED_FOEDSELSNUMMER.value),
            listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
        )

    private val saksbehandler = mockSaksbehandler("SB007")
    private val vedtakKlientMock = mockk<VedtakKlient>()
    private val behandlingServiceMock = mockk<BehandlingService>()

    @BeforeAll
    fun start() {
        val dummyFeatureToggleService = DummyFeatureToggleService()
        startServer(vedtakKlient = vedtakKlientMock, featureToggleService = dummyFeatureToggleService)

        nyKontekstMedBrukerOgDatabase(saksbehandler, applicationContext.dataSource)
        dummyFeatureToggleService.settBryter(EtteroppgjoerToggles.ETTEROPPGJOER_PERIODISK_JOBB, true)
    }

    @AfterAll
    fun shutdown() = afterAll()

    @BeforeEach
    fun beforeEach() {
        resetDatabase()
    }

    @Test
    fun `run skal opprette etteroppgjoer for aar hvor saken har utbetalinger`() {
        val currentYear = YearMonth.now().year
        val sak = opprettSak()
        opprettBehandling(sak)

        coEvery { behandlingServiceMock.hentSisteIverksatteBehandling(sak.id) } returns null

        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(currentYear - 1, any())
        } returns listOf(sak.id)
        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(currentYear, any())
        } returns emptyList()

        applicationContext.etteroppgjoerJobService.run()

        inTransaction {
            val etteroppgjoerForForrigeAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear - 1)!!
            etteroppgjoerForForrigeAar.sakId shouldBe sak.id
            etteroppgjoerForForrigeAar.status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
            etteroppgjoerForForrigeAar.inntektsaar shouldBe currentYear - 1

            val etteroppgjoerForIAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear)
            etteroppgjoerForIAar shouldBe null
        }
    }

    @Test
    fun `run skal ikke opprette etteroppgjoer for saker som ikke har utbetalinger`() {
        val currentYear = YearMonth.now().year
        val sak = opprettSak()
        opprettBehandling(sak)

        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(any(), any())
        } returns emptyList()

        applicationContext.etteroppgjoerJobService.run()

        inTransaction {
            val etteroppgjoerForForrigeAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear - 1)
            etteroppgjoerForForrigeAar shouldBe null
            val etteroppgjoerForIAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear)
            etteroppgjoerForIAar shouldBe null
        }
    }

    private fun opprettBehandling(sak: Sak) {
        inTransaction {
            val behandlingFactory = applicationContext.behandlingFactory
            behandlingFactory.opprettBehandling(
                sakId = sak.id,
                persongalleri = defaultPersongalleriGydligeFnr,
                mottattDato = null,
                kilde = Vedtaksloesning.GJENNY,
                request = behandlingFactory.hentDataForOpprettBehandling(sak.id),
                opprinnelse = BehandlingOpprinnelse.JOURNALFOERING,
            )
        }
    }

    private fun opprettSak(): Sak =
        inTransaction {
            applicationContext.sakService.finnEllerOpprettSakMedGrunnlag(
                soeker,
                SakType.OMSTILLINGSSTOENAD,
                Enhetsnummer("1234"),
            )
        }
}
