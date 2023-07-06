package no.nav.etterlatte.behandling.revurdering

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingServiceFeatureToggle
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
        val hendelser = spyk(applicationContext.behandlingsHendelser)
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
            RevurderingServiceImpl(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.GJENNY,
                paaGrunnAvHendelse = null
            )

        inTransaction {
            Assertions.assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            verify { hendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET) }
            confirmVerified(hendelser)
        }
    }

    @Test
    fun `kan lagre og oppdatere revurderinginfo på en revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
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
        val revurderingService = RevurderingServiceImpl(
            hendelser,
            featureToggleService,
            applicationContext.behandlingDao,
            applicationContext.hendelseDao,
            applicationContext.grunnlagsendringshendelseDao
        )
        val revurdering = revurderingService.opprettManuellRevurdering(
            sakId = sak.id,
            forrigeBehandling = behandling!!,
            revurderingAarsak = RevurderingAarsak.SOESKENJUSTERING,
            kilde = Vedtaksloesning.GJENNY,
            paaGrunnAvHendelse = null
        )
        val revurderingInfo = RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.SOESKEN_DOER)
        val fikkLagret = revurderingService.lagreRevurderingInfo(
            revurdering!!.id,
            revurderingInfo,
            "saksbehandler"
        )
        assertTrue(fikkLagret)
        inTransaction {
            val lagretRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            Assertions.assertEquals(revurderingInfo, lagretRevurdering.revurderingInfo)
        }

        // kan oppdatere
        val nyRevurderingInfo =
            RevurderingInfo.Soeskenjustering(BarnepensjonSoeskenjusteringGrunn.FORPLEID_ETTER_BARNEVERNSLOVEN)
        assertTrue(
            revurderingService.lagreRevurderingInfo(
                revurdering.id,
                nyRevurderingInfo,
                "saksbehandler"
            )
        )
        inTransaction {
            val oppdatert = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            Assertions.assertEquals(nyRevurderingInfo, oppdatert.revurderingInfo)
        }

        inTransaction {
            applicationContext.behandlingDao.lagreStatus(
                revurdering.id,
                BehandlingStatus.IVERKSATT,
                LocalDateTime.now()
            )
        }

        // kan ikke oppdatere en ferdig revurdering
        assertFalse(
            revurderingService.lagreRevurderingInfo(
                revurdering.id,
                revurderingInfo,
                "saksbehandler"
            )
        )
        inTransaction {
            val ferdigRevurdering = applicationContext.behandlingDao.hentBehandling(revurdering.id) as Revurdering
            Assertions.assertEquals(nyRevurderingInfo, ferdigRevurdering.revurderingInfo)
            verify { hendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET) }
            verify { hendelser.sendBehovForNyttGrunnlag(revurdering) }
            confirmVerified(hendelser)
        }
    }

    @Test
    fun `hvis featuretoggle er av saa opprettes ikke revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
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
            RevurderingServiceImpl(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.GJENNY,
                paaGrunnAvHendelse = null
            )
        )

        confirmVerified(hendelser)
    }

    @Test
    fun `Ny regulering skal håndtere hendelser om nytt grunnbeløp`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
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
            RevurderingServiceImpl(
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.GJENNY,
                paaGrunnAvHendelse = hendelse.id
            )

        inTransaction {
            Assertions.assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            val grunnlaghendelse = applicationContext.grunnlagsendringshendelseDao.hentGrunnlagsendringshendelse(
                hendelse.id
            )
            Assertions.assertEquals(revurdering.id, grunnlaghendelse?.behandlingId)

            verify { hendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET) }
            confirmVerified(hendelser)
        }
    }
}