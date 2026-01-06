package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.domain.AutomatiskRevurdering
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManuellRevurdering
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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
import java.util.UUID

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
        val etteroppgjoerAar = ETTEROPPGJOER_AAR
        val aarEtterEtteroppgjoerAar = etteroppgjoerAar + 1
        val sak = opprettSak()
        val behandlingId = opprettBehandling(sak)

        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(etteroppgjoerAar, any())
        } returns listOf(sak.id)
        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(etteroppgjoerAar, any())
        } returns listOf(sak.id)
        coEvery {
            vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(aarEtterEtteroppgjoerAar, any())
        } returns emptyList()
        coEvery {
            vedtakKlientMock.hentIverksatteVedtak(sak.id, any())
        } returns
            listOf(
                VedtakSammendragDto(
                    id = UUID.randomUUID().toString(),
                    behandlingId = behandlingId,
                    vedtakType = VedtakType.INNVILGELSE,
                    behandlendeSaksbehandler = "",
                    datoFattet = null,
                    attesterendeSaksbehandler = "",
                    datoAttestert = null,
                    virkningstidspunkt = null,
                    opphoerFraOgMed = null,
                    iverksettelsesTidspunkt = null,
                ),
            )

        runBlocking {
            applicationContext.opprettEtteroppgjoerJobService.startEtteroppgjoerKjoering()
        }

        inTransaction {
            val etteroppgjoerForForrigeAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, etteroppgjoerAar)!!
            with(etteroppgjoerForForrigeAar) {
                sakId shouldBe sak.id
                status shouldBe EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
                inntektsaar shouldBe etteroppgjoerAar
            }

            val etteroppgjoerForIAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, aarEtterEtteroppgjoerAar)
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

        runBlocking {
            applicationContext.opprettEtteroppgjoerJobService.startEtteroppgjoerKjoering()
        }

        inTransaction {
            val etteroppgjoerForForrigeAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear - 1)
            etteroppgjoerForForrigeAar shouldBe null
            val etteroppgjoerForIAar =
                applicationContext.etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, currentYear)
            etteroppgjoerForIAar shouldBe null
        }
    }

    private fun opprettBehandling(sak: Sak): UUID =
        inTransaction {
            val behandlingFactory = applicationContext.behandlingFactory
            val behandlingOgOppgave =
                behandlingFactory.opprettBehandling(
                    sakId = sak.id,
                    persongalleri = defaultPersongalleriGydligeFnr,
                    mottattDato = null,
                    kilde = Vedtaksloesning.GJENNY,
                    request = behandlingFactory.hentDataForOpprettBehandling(sak.id),
                    opprinnelse = BehandlingOpprinnelse.JOURNALFOERING,
                )
            iverksett(behandlingOgOppgave.behandling)
            behandlingOgOppgave.behandling.id
        }

    private fun opprettSak(): Sak =
        inTransaction {
            applicationContext.sakService.finnEllerOpprettSakMedGrunnlag(
                soeker,
                SakType.OMSTILLINGSSTOENAD,
                Enhetsnummer("1234"),
            )
        }

    private fun iverksett(behandling: Behandling) {
        val iverksatt =
            when (behandling) {
                is Foerstegangsbehandling -> behandling.copy(status = BehandlingStatus.IVERKSATT)
                is ManuellRevurdering -> behandling.copy(status = BehandlingStatus.IVERKSATT)
                is AutomatiskRevurdering -> behandling.copy(status = BehandlingStatus.IVERKSATT)
            }
        applicationContext.behandlingDao.lagreStatus(iverksatt)
    }
}
