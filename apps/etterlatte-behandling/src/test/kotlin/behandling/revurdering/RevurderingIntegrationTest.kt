package no.nav.etterlatte.behandling.revurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingServiceFeatureToggle
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevurderingIntegrationTest : BehandlingIntegrationTest() {
    @BeforeAll
    fun start() {
        startServer()
        Kontekst.set(Context(mockk(), DatabaseContext(applicationContext.dataSource)))
    }

    @AfterEach
    fun afterEach() {
        resetDatabase()
    }

    @AfterAll
    fun shutdown() = afterAll()

    val fnr: String = "123"

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = applicationContext.behandlingsHendelser.hendelserKanal
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false
            )
        } returns false

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        val revurdering =
            RealRevurderingService(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.DOFFEN
            )

        inTransaction {
            Assertions.assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
        }
    }

    @Test
    fun `hvis featuretoggle er av saa opprettes ikke revurdering`() {
        val hendelser = applicationContext.behandlingsHendelser.hendelserKanal
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false
            )
        } returns false

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }

        assertNull(
            RealRevurderingService(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.DOFFEN
            )
        )
    }

    @Test
    fun `Ny regulering skal håndtere hendelser om nytt grunnbeløp`() {
        val hendelser = applicationContext.behandlingsHendelser.hendelserKanal
        val featureToggleService = mockk<FeatureToggleService>()

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false
            )
        } returns false

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr)

        assertNotNull(behandling)

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                behandling!!.id,
                BehandlingStatus.IVERKSATT,
                Tidspunkt.now().toLocalDatetimeUTC()
            )
        }
        val hendelse = inTransaction {
            applicationContext.grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                Grunnlagsendringshendelse(
                    UUID.randomUUID(),
                    sak.id,
                    GrunnlagsendringsType.GRUNNBELOEP,
                    LocalDateTime.now(),
                    GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                    null,
                    Saksrolle.SOEKER,
                    sak.ident,
                    SamsvarMellomKildeOgGrunnlag.Grunnbeloep(false)
                )
            )
        }

        val revurdering =
            RealRevurderingService(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.DOFFEN
            )

        inTransaction {
            Assertions.assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            val grunnlaghendelse = applicationContext.grunnlagsendringshendelseDao.hentGrunnlagsendringshendelse(
                hendelse.id
            )
            Assertions.assertEquals(revurdering.id, grunnlaghendelse?.behandlingId)
        }
    }
}