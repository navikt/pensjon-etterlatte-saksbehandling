package no.nav.etterlatte.behandling.revurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevurderingIntegrationTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        startServer()
        Kontekst.set(Context(mockk(), DatabaseContext(beanFactory.dataSource())))
    }

    @AfterEach
    fun afterEach() {
        beanFactory.resetDatabase()
    }

    @AfterAll
    fun shutdown() = afterAll()

    val sakId: Long = 1
    val fnr: String = "123"

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val revurderingFactory = RevurderingFactory(beanFactory.behandlingDao(), beanFactory.hendelseDao())
        val hendelser = beanFactory.behandlingHendelser().nyHendelse
        val featureToggleService = mockk<FeatureToggleService>()
        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true

        val (_, behandling) = inTransaction { beanFactory.opprettSakMedFoerstegangsbehandling(sakId, fnr) }
        inTransaction {
            beanFactory.behandlingDao().lagreStatus(
                behandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        val revurdering =
            RealRevurderingService(revurderingFactory, hendelser, featureToggleService).opprettRevurdering(
                behandling,
                RevurderingAarsak.REGULERING
            )

        inTransaction {
            Assertions.assertEquals(revurdering, beanFactory.behandlingDao().hentBehandling(revurdering.id))
        }
    }

    @Test
    fun `hvis featuretoggle er av saa kastes NotImplementedError ved opprettelse`() {
        val revurderingFactory = RevurderingFactory(beanFactory.behandlingDao(), beanFactory.hendelseDao())
        val hendelser = beanFactory.behandlingHendelser().nyHendelse
        val featureToggleService = mockk<FeatureToggleService>()
        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns false

        val (_, behandling) = inTransaction { beanFactory.opprettSakMedFoerstegangsbehandling(sakId, fnr) }
        inTransaction {
            beanFactory.behandlingDao().lagreStatus(
                behandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        assertThrows<NotImplementedError> {
            RealRevurderingService(revurderingFactory, hendelser, featureToggleService).opprettRevurdering(
                behandling,
                RevurderingAarsak.REGULERING
            )
        }
    }
}