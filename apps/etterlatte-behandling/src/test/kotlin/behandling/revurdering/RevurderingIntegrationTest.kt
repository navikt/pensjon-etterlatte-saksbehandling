package no.nav.etterlatte.behandling.revurdering

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingServiceFeatureToggle
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BarnepensjonSoeskenjusteringGrunn
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgaveny.OppgaveType
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakServiceFeatureToggle
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

    fun opprettSakMedFoerstegangsbehandling(
        fnr: String,
        behandlingFactory: BehandlingFactory? = null
    ): Pair<Sak, Foerstegangsbehandling?> {
        val sak = inTransaction {
            applicationContext.sakDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
        }
        val behandling = (behandlingFactory ?: applicationContext.behandlingFactory)
            .opprettBehandling(
                sak.id,
                persongalleri(),
                LocalDateTime.now().toString(),
                Vedtaksloesning.GJENNY
            )

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `kan opprette ny revurdering og lagre i db`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val featureToggleService = mockk<FeatureToggleService>()
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveServiceNy)

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
                oppgaveService,
                grunnlagService,
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService
            ).opprettManuellRevurdering(
                sakId = sak.id,
                forrigeBehandling = behandling!!,
                revurderingAarsak = RevurderingAarsak.REGULERING,
                kilde = Vedtaksloesning.GJENNY,
                paaGrunnAvHendelse = null
            )

        verify { grunnlagService.leggInnNyttGrunnlag(revurdering!!) }
        verify {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                revurdering?.id.toString(),
                sak.id,
                OppgaveType.REVUDERING
            )
        }
        inTransaction {
            Assertions.assertEquals(revurdering, applicationContext.behandlingDao.hentBehandling(revurdering!!.id))
            verify { hendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET) }
        }
        confirmVerified(hendelser, grunnlagService, oppgaveService)
    }

    @Test
    fun `kan lagre og oppdatere revurderinginfo på en revurdering`() {
        val hendelser = spyk(applicationContext.behandlingsHendelser)
        val featureToggleService = mockk<FeatureToggleService>()
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveServiceNy)

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
            oppgaveService,
            grunnlagService,
            hendelser,
            featureToggleService,
            applicationContext.behandlingDao,
            applicationContext.hendelseDao,
            applicationContext.grunnlagsendringshendelseDao,
            applicationContext.kommerBarnetTilGodeService
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
            verify { grunnlagService.leggInnNyttGrunnlag(revurdering) }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    revurdering.id.toString(),
                    sak.id,
                    OppgaveType.REVUDERING
                )
            }
            confirmVerified(hendelser, grunnlagService, oppgaveService)
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
                applicationContext.oppgaveServiceNy,
                applicationContext.grunnlagsService,
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService
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
        val grunnlagService = spyk(applicationContext.grunnlagsService)
        val oppgaveService = spyk(applicationContext.oppgaveServiceNy)

        every {
            featureToggleService.isEnabled(
                SakServiceFeatureToggle.FiltrerMedEnhetId,
                false
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                BehandlingServiceFeatureToggle.FiltrerMedEnhetId,
                false
            )
        } returns false

        every {
            featureToggleService.isEnabled(
                RevurderingServiceFeatureToggle.OpprettManuellRevurdering,
                any()
            )
        } returns true

        val revurderingService =
            RevurderingServiceImpl(
                oppgaveService,
                grunnlagService,
                hendelser,
                featureToggleService,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.grunnlagsendringshendelseDao,
                applicationContext.kommerBarnetTilGodeService
            )

        val behandlingFactory =
            BehandlingFactory(
                oppgaveService = oppgaveService,
                grunnlagService = grunnlagService,
                revurderingService = revurderingService,
                sakDao = applicationContext.sakDao,
                behandlingDao = applicationContext.behandlingDao,
                hendelseDao = applicationContext.hendelseDao,
                behandlingHendelser = hendelser,
                featureToggleService = featureToggleService
            )

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)

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

        val revurdering = revurderingService.opprettManuellRevurdering(
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
            verify { grunnlagService.leggInnNyttGrunnlag(behandling) }
            verify { grunnlagService.leggInnNyttGrunnlag(revurdering) }
            verify { hendelser.sendMeldingForHendelse(revurdering, BehandlingHendelseType.OPPRETTET) }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    behandling.id.toString(),
                    sak.id,
                    OppgaveType.FOERSTEGANGSBEHANDLING
                )
            }
            verify {
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    revurdering.id.toString(),
                    sak.id,
                    OppgaveType.REVUDERING
                )
            }
            verify { hendelser.sendMeldingForHendelse(behandling, BehandlingHendelseType.OPPRETTET) }
            confirmVerified(hendelser, grunnlagService, oppgaveService)
        }
    }
}