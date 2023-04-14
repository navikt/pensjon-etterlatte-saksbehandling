package no.nav.etterlatte.behandling.revurdering

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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
    fun `hvis featuretoggle er av saa kastes NotImplementedError ved opprettelse`() {
        val hendelser = beanFactory.behandlingHendelser().nyHendelse
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns false
        val vilkaarsvurderingKlientMock = mockk<VilkaarsvurderingKlient>(relaxed = true)

        val (_, behandling) = beanFactory.opprettSakMedFoerstegangsbehandling(sakId, fnr)
        inTransaction {
            beanFactory.behandlingDao().lagreStatus(
                behandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        assertThrows<NotImplementedError> {
            RealRevurderingService(
                hendelser,
                featureToggleService,
                beanFactory.behandlingDao(),
                beanFactory.hendelseDao(),
                vilkaarsvurderingKlientMock
            ).opprettManuellRevurdering(
                sakId = behandling.sak.id,
                forrigeBehandling = behandling,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                no.nav.etterlatte.token.Saksbehandler("token", "ident", null),
                kilde = Vedtaksloesning.DOFFEN
            )
        }
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = beanFactory.behandlingHendelser().nyHendelse
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true
        val vilkaarsvurderingKlientMock = mockk<VilkaarsvurderingKlient>(relaxed = true)

        val (_, behandling) = beanFactory.opprettSakMedFoerstegangsbehandling(sakId, fnr)

        inTransaction {
            beanFactory.behandlingDao().lagreStatus(
                behandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        val revurdering =
            RealRevurderingService(
                hendelser,
                featureToggleService,
                beanFactory.behandlingDao(),
                beanFactory.hendelseDao(),
                vilkaarsvurderingKlientMock
            ).opprettManuellRevurdering(
                sakId = behandling.sak.id,
                forrigeBehandling = behandling,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                no.nav.etterlatte.token.Saksbehandler("token", "ident", null),
                kilde = Vedtaksloesning.DOFFEN
            )

        inTransaction {
            Assertions.assertEquals(revurdering, beanFactory.behandlingDao().hentBehandling(revurdering.id))
        }
    }

    @Test
    fun `rollback lagring av behandling og hendelser hvis apikall feiler`() {
        val hendelser = beanFactory.behandlingHendelser().nyHendelse
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true
        val vilkaarsvurderingKlientMock = mockk<VilkaarsvurderingKlient>()
        coEvery {
            vilkaarsvurderingKlientMock.kopierVilkaarsvurderingFraForrigeBehandling(
                any(),
                any(),
                any()
            )
        } throws RuntimeException()

        val (_, behandling) = beanFactory.opprettSakMedFoerstegangsbehandling(sakId, fnr)

        inTransaction {
            beanFactory.behandlingDao().lagreStatus(
                behandling.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        val revurderingService =
            RealRevurderingService(
                hendelser,
                featureToggleService,
                beanFactory.behandlingDao(),
                beanFactory.hendelseDao(),
                vilkaarsvurderingKlientMock
            )

        assertThrows<RuntimeException> {
            revurderingService.opprettManuellRevurdering(
                sakId = behandling.sak.id,
                forrigeBehandling = behandling,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                no.nav.etterlatte.token.Saksbehandler("token", "ident", null),
                kilde = Vedtaksloesning.DOFFEN
            )
        }

        inTransaction {
            Assertions.assertEquals(
                0,
                beanFactory.behandlingDao().alleBehandlingerAvType(BehandlingType.REVURDERING).size
            )
            Assertions.assertEquals(1, beanFactory.behandlingDao().alleBehandlinger().size)
            Assertions.assertEquals(1, beanFactory.hendelseDao().finnHendelserIBehandling(behandling.id).size)
        }
    }
}