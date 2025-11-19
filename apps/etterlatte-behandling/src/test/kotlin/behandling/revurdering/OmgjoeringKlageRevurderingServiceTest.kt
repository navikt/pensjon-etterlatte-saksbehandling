package no.nav.etterlatte.behandling.revurdering

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.defaultPersongalleriGydligeFnr
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.GrunnForOmgjoering
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KlageOmgjoering
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmgjoeringKlageRevurderingServiceTest : BehandlingIntegrationTest() {
    val user = mockk<SaksbehandlerMedEnheterOgRoller>(relaxed = true)

    @BeforeAll
    fun start() {
        val saksbehandlerMedRoller =
            mockk<SaksbehandlerMedRoller> {
                every { harRolleStrengtFortrolig() } returns false
                every { harRolleEgenAnsatt() } returns false
                every { harRolleAttestant() } returns true
            }
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        every { user.enheter() } returns listOf(Enheter.defaultEnhet.enhetNr)

        startServer()
        nyKontekstMedBrukerOgDatabase(user, applicationContext.dataSource)
    }

    @AfterAll
    fun shutdown() = afterAll()

    val fnr: String = "123"

    private fun opprettSakMedFoerstegangsbehandling(
        fnr: String,
        behandlingFactory: BehandlingFactory? = null,
    ): Pair<Sak, Foerstegangsbehandling?> {
        val sak =
            inTransaction {
                applicationContext.sakSkrivDao.opprettSak(fnr, SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)
            }
        val factory = behandlingFactory ?: applicationContext.behandlingFactory
        val behandling =
            inTransaction {
                factory
                    .opprettBehandling(
                        sak.id,
                        defaultPersongalleriGydligeFnr,
                        LocalDateTime.now().toString(),
                        Vedtaksloesning.GJENNY,
                        factory.hentDataForOpprettBehandling(sak.id),
                        BehandlingOpprinnelse.UKJENT,
                    )
            }.also { it.sendMeldingForHendelse() }.behandling

        return Pair(sak, behandling as Foerstegangsbehandling)
    }

    @Test
    fun `Kan opprette revurdering for omgjoering av klage`() {
        val revurderingService = revurderingService()
        val behandlingFactory = behandlingFactory()

        val (sak, behandling) = opprettSakMedFoerstegangsbehandling(fnr, behandlingFactory)
        val hentOppgaverForSak = inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }
        val oppgaveForFoerstegangsbehandling = hentOppgaverForSak.single { it.status == Status.NY }

        val saksbehandlerIdent = "saksbehandler"
        val saksbehandler = simpleSaksbehandler()
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveForFoerstegangsbehandling.id,
                saksbehandlerIdent,
            )
        }
        inTransaction {
            applicationContext.oppgaveService.tilAttestering(
                referanse = behandling!!.id.toString(),
                type = OppgaveType.FOERSTEGANGSBEHANDLING,
                merknad = null,
            )
        }
        inTransaction {
            val iverksatt = behandling!!.copy(status = BehandlingStatus.IVERKSATT)
            applicationContext.behandlingDao.lagreStatus(iverksatt)
        }
        val klage =
            inTransaction {
                applicationContext.klageService.opprettKlage(
                    sakId = sak.id,
                    innkommendeKlage =
                        InnkommendeKlage(
                            mottattDato = LocalDate.of(2024, 1, 1),
                            journalpostId = "En id for journalpost",
                            innsender = SOEKER_FOEDSELSNUMMER.value,
                        ),
                    saksbehandler = saksbehandler,
                )
            }
        val klageOppgave =
            inTransaction { applicationContext.oppgaveService.hentOppgaverForSak(sak.id) }.filter { it.referanse == klage.id.toString() }[0]
        inTransaction { applicationContext.oppgaveService.tildelSaksbehandler(klageOppgave.id, saksbehandlerIdent) }
        inTransaction {
            applicationContext.klageService.lagreFormkravIKlage(
                klageId = klage.id,
                formkrav =
                    Formkrav(
                        vedtaketKlagenGjelder =
                            VedtaketKlagenGjelder(
                                behandlingId = behandling!!.id.toString(),
                                datoAttestert =
                                    ZonedDateTime.of(
                                        LocalDate.of(2023, 10, 10),
                                        LocalTime.MIDNIGHT,
                                        ZoneId.systemDefault(),
                                    ),
                                vedtakType = VedtakType.INNVILGELSE,
                                id = "123",
                            ),
                        erKlagerPartISaken = JaNei.JA,
                        erKlagenSignert = JaNei.JA,
                        gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                        erKlagenFramsattInnenFrist = JaNei.JA,
                        erFormkraveneOppfylt = JaNei.JA,
                        begrunnelse = "Jeg er enig",
                    ),
                saksbehandler = saksbehandler,
            )
        }

        val oppgaveForOmgjoering =
            inTransaction {
                applicationContext.klageService.lagreInitieltUtfallMedBegrunnelseAvKlage(
                    klageId = klage.id,
                    utfall = InitieltUtfallMedBegrunnelseDto(KlageUtfall.OMGJOERING, "Vi må endre vedtak"),
                    saksbehandler = saksbehandler,
                )
                applicationContext.klageService.lagreUtfallAvKlage(
                    klageId = klage.id,
                    utfall =
                        KlageUtfallUtenBrev.Omgjoering(
                            omgjoering =
                                KlageOmgjoering(
                                    grunnForOmgjoering = GrunnForOmgjoering.FEIL_REGELVERKSFORSTAAELSE,
                                    begrunnelse = "Vi må endre vedtak",
                                ),
                        ),
                    saksbehandler = saksbehandler,
                )
                applicationContext.klageService.ferdigstillKlage(klageId = klage.id, saksbehandler = saksbehandler)
                applicationContext.oppgaveService
                    .hentOppgaverForSak(sak.id)
                    .first { it.type == OppgaveType.OMGJOERING }
            }
        inTransaction {
            applicationContext.oppgaveService.tildelSaksbehandler(
                oppgaveId = oppgaveForOmgjoering.id,
                saksbehandlerIdent,
            )
        }
        val revurderingOmgjoering =
            inTransaction { revurderingService.opprettOmgjoeringKlage(sak.id, oppgaveForOmgjoering.id, saksbehandler) }

        assertEquals(revurderingOmgjoering.relatertBehandlingId, klage.id.toString())
        assertEquals(revurderingOmgjoering.revurderingsaarsak, Revurderingaarsak.OMGJOERING_ETTER_KLAGE)

        inTransaction { applicationContext.behandlingService.avbrytBehandling(revurderingOmgjoering.id, saksbehandler) }

        val oppgaverISakEtterAlt =
            inTransaction {
                applicationContext.oppgaveService.hentOppgaverForSak(sak.id)
            }
        val antallOmgjoeringsoppgaver = oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING }
        val antallAapneOmgjoeringsoppgaver =
            oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING && !it.status.erAvsluttet() }
        val antallOmgjoeringsoppgaverSomPekerPaaKlage =
            oppgaverISakEtterAlt.count { it.type == OppgaveType.OMGJOERING && it.referanse == klage.id.toString() }
        assertEquals(2, antallOmgjoeringsoppgaver)
        assertEquals(1, antallAapneOmgjoeringsoppgaver)
        assertEquals(2, antallOmgjoeringsoppgaverSomPekerPaaKlage)
    }

    private fun revurderingService(
        oppgaveService: OppgaveService = applicationContext.oppgaveService,
        grunnlagService: GrunnlagService = applicationContext.grunnlagService,
        behandlingsHendelser: BehandlingsHendelserKafkaProducerImpl = applicationContext.behandlingsHendelser,
        aktivitetspliktDao: AktivitetspliktDao = applicationContext.aktivitetspliktDao,
        aktivitetspliktKopierService: AktivitetspliktKopierService = applicationContext.aktivitetspliktKopierService,
    ) = OmgjoeringKlageRevurderingService(
        revurderingService =
            RevurderingService(
                oppgaveService,
                grunnlagService,
                behandlingsHendelser,
                applicationContext.behandlingDao,
                applicationContext.hendelseDao,
                applicationContext.kommerBarnetTilGodeService,
                applicationContext.revurderingDao,
                aktivitetspliktDao,
                aktivitetspliktKopierService,
            ),
        oppgaveService = oppgaveService,
        klageService = applicationContext.klageService,
        behandlingDao = applicationContext.behandlingDao,
        grunnlagService = grunnlagService,
    )

    private fun behandlingFactory() =
        BehandlingFactory(
            oppgaveService = applicationContext.oppgaveService,
            grunnlagService = applicationContext.grunnlagService,
            revurderingService = applicationContext.revurderingService,
            gyldighetsproevingService = applicationContext.gyldighetsproevingService,
            sakService = applicationContext.sakService,
            behandlingDao = applicationContext.behandlingDao,
            hendelseDao = applicationContext.hendelseDao,
            behandlingHendelser = applicationContext.behandlingsHendelser,
            vilkaarsvurderingService = applicationContext.vilkaarsvurderingService,
            kommerBarnetTilGodeService = applicationContext.kommerBarnetTilGodeService,
            behandlingInfoService = mockk(),
            tilgangsService =
                OppdaterTilgangService(
                    applicationContext.skjermingKlient,
                    applicationContext.pdlTjenesterKlient,
                    applicationContext.brukerService,
                    applicationContext.oppgaveService,
                    applicationContext.sakSkrivDao,
                    applicationContext.sakTilgang,
                    applicationContext.sakLesDao,
                    applicationContext.featureToggleService,
                ),
        )
}
