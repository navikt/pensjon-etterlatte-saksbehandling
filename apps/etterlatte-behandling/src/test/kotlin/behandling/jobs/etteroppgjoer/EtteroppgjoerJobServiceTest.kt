package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.soeker
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EtteroppgjoerJobServiceTest(
    private val dataSource: DataSource,
) : BehandlingIntegrationTest() {
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

    @Test
    fun name() {
        inTransaction {
            val sak =
                applicationContext.sakService.finnEllerOpprettSakMedGrunnlag(
                    soeker,
                    SakType.OMSTILLINGSSTOENAD,
                    Enhetsnummer("1234"),
                )
            coEvery {
                vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(2024, any())
            } returns listOf(sak.id)
            coEvery {
                vedtakKlientMock.hentSakerMedUtbetalingForInntektsaar(2025, any())
            } returns emptyList()

            val behandlingFactory = applicationContext.behandlingFactory
            behandlingFactory.opprettBehandling(
                sak.id,
                defaultPersongalleriGydligeFnr,
                null,
                Vedtaksloesning.GJENNY,
                behandlingFactory.hentDataForOpprettBehandling(sak.id),
                BehandlingOpprinnelse.JOURNALFOERING,
            )
        }

        testApplication {
            applicationContext.etteroppgjoerJobService.run()
        }
    }
}
