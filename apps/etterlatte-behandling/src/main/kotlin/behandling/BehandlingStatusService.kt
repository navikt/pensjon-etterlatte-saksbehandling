package no.nav.etterlatte.behandling

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoDao
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.ManuellRevurdering
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.time.Year
import java.util.UUID

interface BehandlingStatusService {
    fun settOpprettet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean = true,
    )

    fun settVilkaarsvurdert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean = true,
    )

    fun settTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean = true,
    )

    fun settBeregnet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean = true,
    )

    fun settAvkortet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean = true,
    )

    fun sjekkOmKanFatteVedtak(behandlingId: UUID)

    fun settFattetVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    )

    fun sjekkOmKanAttestere(behandlingId: UUID)

    fun settAttestertVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    )

    fun sjekkOmKanReturnereVedtak(behandlingId: UUID)

    fun settReturnertVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    )

    fun settTilSamordnetVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    )

    fun settSamordnetVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    )

    suspend fun settIverksattVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    )

    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakslisteDTO: SakslisteDTO): SakIDListe
}

class BehandlingStatusServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val behandlingService: BehandlingService,
    private val behandlingInfoDao: BehandlingInfoDao,
    private val oppgaveService: OppgaveService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val generellBehandlingService: GenerellBehandlingService,
    private val aktivitetspliktService: AktivitetspliktService,
    private val saksbehandlerService: SaksbehandlerService,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val forbehandlingService: EtteroppgjoerForbehandlingService,
    private val grunnlagService: GrunnlagService,
) : BehandlingStatusService {
    private val logger = LoggerFactory.getLogger(BehandlingStatusServiceImpl::class.java)

    override fun settOpprettet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean,
    ) {
        val behandling = hentBehandling(behandlingId).tilOpprettet()

        if (!dryRun) {
            behandlingDao.lagreStatus(behandling)
            behandlingService.registrerBehandlingHendelse(behandling, brukerTokenInfo.ident())
        }
    }

    override fun settVilkaarsvurdert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean,
    ) {
        val behandling = hentBehandling(behandlingId).tilVilkaarsvurdert()

        if (!dryRun) {
            behandlingDao.lagreStatus(behandling)
            behandlingService.registrerBehandlingHendelse(behandling, brukerTokenInfo.ident())
        }
    }

    override fun settTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilTrygdetidOppdatert().lagreEndring(dryRun, brukerTokenInfo.ident())
    }

    override fun settBeregnet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilBeregnet().lagreEndring(dryRun, brukerTokenInfo.ident())
    }

    override fun settAvkortet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilAvkortet().lagreEndring(dryRun, brukerTokenInfo.ident())
    }

    override fun sjekkOmKanFatteVedtak(behandlingId: UUID) {
        val behandling = hentBehandling(behandlingId)

        if (behandling is ManuellRevurdering) {
            behandling.tilFattetVedtakUtvidet()
        } else {
            behandling.tilFattetVedtak()
        }
    }

    override fun settFattetVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (behandling is ManuellRevurdering) {
            lagreNyBehandlingStatus(behandling.tilFattetVedtakUtvidet())
        } else {
            lagreNyBehandlingStatus(behandling.tilFattetVedtak())
        }

        registrerVedtakHendelse(behandling.id, vedtak.vedtakHendelse, HendelseType.FATTET)

        val merknad =
            when (brukerTokenInfo) {
                is Saksbehandler ->
                    when (behandling.revurderingsaarsak()) {
                        Revurderingaarsak.ETTEROPPGJOER -> {
                            val etteroppgjoersAar =
                                forbehandlingService
                                    .hentForbehandling(
                                        UUID.fromString(behandling.relatertBehandlingId!!),
                                    ).aar

                            genererMerknad(
                                prefix = "Etteroppgjør $etteroppgjoersAar",
                                vedtak = vedtak,
                                merknad = "Behandlet av ${brukerTokenInfo.ident}",
                            )
                        }

                        else -> genererMerknad(vedtak = vedtak, merknad = "Behandlet av ${brukerTokenInfo.ident}")
                    }

                is Systembruker ->
                    when (behandling.revurderingsaarsak()) {
                        Revurderingaarsak.INNTEKTSENDRING ->
                            genererMerknad(
                                prefix = "Inntektsendring",
                                merknad = "Automatisk behandlet",
                            )

                        else -> genererMerknad(vedtak = vedtak, merknad = "Behandlet av systemet")
                    }
            }

        oppgaveService.tilAttestering(
            referanse = vedtak.sakIdOgReferanse.referanse,
            type = OppgaveType.fra(behandling.type),
            merknad = merknad,
        )
    }

    override fun sjekkOmKanAttestere(behandlingId: UUID) {
        hentBehandling(behandlingId).tilAttestert()
    }

    override fun settAttestertVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (vedtak.vedtakType == VedtakType.AVSLAG) {
            lagreNyBehandlingStatus(behandling.tilAvslag())
            haandterUtland(behandling)
        } else {
            lagreNyBehandlingStatus(behandling.tilAttestert())
        }

        registrerVedtakHendelse(behandling.id, vedtak.vedtakHendelse, HendelseType.ATTESTERT)
        haandterEtteroppgjoerAttestertVedtak(behandling)

        oppgaveService.ferdigStillOppgaveUnderBehandling(
            referanse = vedtak.sakIdOgReferanse.referanse,
            type = OppgaveType.fra(behandling.type),
            saksbehandler = brukerTokenInfo,
            merknad = "${vedtak.vedtakType.tilLesbarString()}: ${vedtak.vedtakHendelse.kommentar ?: ""}. Attestant: ${
                hentSaksbehandlerNavn(
                    brukerTokenInfo.ident(),
                )
            }",
        )
    }

    private fun hentSaksbehandlerNavn(ident: String): String = saksbehandlerService.hentNavnForIdent(ident) ?: ident

    override fun sjekkOmKanReturnereVedtak(behandlingId: UUID) {
        hentBehandling(behandlingId).tilReturnert()
    }

    override fun settReturnertVedtak(
        behandling: Behandling,
        vedtak: VedtakEndringDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        lagreNyBehandlingStatus(behandling.tilReturnert())
        registrerVedtakHendelse(behandling.id, vedtak.vedtakHendelse, HendelseType.UNDERKJENT)

        oppgaveService.tilUnderkjent(
            referanse = vedtak.sakIdOgReferanse.referanse,
            type = OppgaveType.fra(behandling.type),
            merknad =
                vedtak.vedtakHendelse.let {
                    listOfNotNull(it.valgtBegrunnelse, it.kommentar).joinToString(separator = ": ")
                } + ". Attestant: ${hentSaksbehandlerNavn(brukerTokenInfo.ident())}",
        )
        // Automatisk inntektsendring skal gjøres manuelt hvis returnert fra attestering
        if (
            behandling.revurderingsaarsak() == Revurderingaarsak.INNTEKTSENDRING &&
            behandling.prosesstype == Prosesstype.AUTOMATISK
        ) {
            behandlingService.endreProsesstype(behandling.id, Prosesstype.MANUELL)
        }
    }

    override fun settTilSamordnetVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    ) {
        val behandling = hentBehandling(behandlingId)
        lagreNyBehandlingStatus(behandling.tilTilSamordning())
        registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.TIL_SAMORDNING)
    }

    override fun settSamordnetVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    ) {
        val behandling = hentBehandling(behandlingId)
        lagreNyBehandlingStatus(behandling.tilSamordnet())
        registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.SAMORDNET)
    }

    override suspend fun settIverksattVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    ) {
        val behandling = hentBehandling(behandlingId)
        lagreNyBehandlingStatus(behandling.tilIverksatt())
        registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.IVERKSATT)

        haandterEtteroppgjoerIverksattVedtak(behandling)
        haandterUtland(behandling)
        haandterFeilutbetaling(behandling)
        haandterAktivitetspliktOppgave(behandling)

        if (behandling.type == BehandlingType.REVURDERING) {
            grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId)
        }
    }

    private suspend fun haandterEtteroppgjoerIverksattVedtak(behandling: Behandling) {
        if (behandling.sak.sakType != SakType.OMSTILLINGSSTOENAD) return

        val virk =
            krevIkkeNull(behandling.virkningstidspunkt) {
                "Iverksatt behandling må ha virkningstidspunkt"
            }

        // opprett forventet etteroppgjør hvis virkningstidspunkt er tilbake
        if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
            val virkAar = virk.dato.year
            val aar = Year.now().value

            // TODO: litt for enkelt, må kanskje sjekke mer
            if (aar > virkAar) {
                try {
                    etteroppgjoerService.opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(behandling, virkAar)
                } catch (e: Exception) {
                    logger.error("Kunne ikke opprette etteroppgjør ved iverksettelse av sak", e)
                }
            }
        }

        // oppdater etteroppgjoer status til ferdigstilt
        if (behandling.type == BehandlingType.REVURDERING && behandling.revurderingsaarsak() == Revurderingaarsak.ETTEROPPGJOER) {
            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                behandling.sak.id,
                virk.dato.year,
                EtteroppgjoerStatus.FERDIGSTILT,
            )
        }
    }

    private fun haandterEtteroppgjoerAttestertVedtak(behandling: Behandling) {
        if (behandling.type != BehandlingType.REVURDERING || behandling.revurderingsaarsak() != Revurderingaarsak.ETTEROPPGJOER) {
            return
        }

        val forbehandling = forbehandlingService.hentForbehandling(UUID.fromString(behandling.relatertBehandlingId))
        if (forbehandling.erUnderBehandling()) {
            forbehandlingService.lagreForbehandling(forbehandling.tilFerdigstilt())
        }
    }

    private fun haandterUtland(behandling: Behandling) {
        if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING ||
            (behandling.type == BehandlingType.REVURDERING && behandling.revurderingsaarsak() == Revurderingaarsak.UTSENDELSE_AV_KRAVPAKKE)
        ) {
            if (behandling.boddEllerArbeidetUtlandet?.skalSendeKravpakke == true) {
                generellBehandlingService.opprettBehandling(
                    GenerellBehandling.opprettUtland(
                        behandling.sak.id,
                        behandling.id,
                    ),
                    null,
                )
            } else {
                logger.info("behandling ${behandling.id} har ikke satt skalSendeKravpakke=true")
            }
        } else {
            logger.info("Behandlingtype: ${behandling.type} får ikke utlandsoppgave")
        }
    }

    private fun haandterAktivitetspliktOppgave(behandling: Behandling) {
        if (behandling.sak.sakType != SakType.OMSTILLINGSSTOENAD) {
            return
        }
        if (behandling.type !in listOf(BehandlingType.REVURDERING, BehandlingType.FØRSTEGANGSBEHANDLING)) {
            return
        }
        // Regulering skal ikke lage oppfølgingsoppgaver
        if (behandling.revurderingsaarsak() == Revurderingaarsak.REGULERING) {
            return
        }

        try {
            val doedsdato =
                grunnlagService
                    .hentPersonopplysninger(
                        behandling.id,
                        behandling.sak.sakType,
                    ).avdoede
                    .singleOrNull()
                    ?.opplysning
                    ?.doedsdato

            val unntakIBehandling = aktivitetspliktService.hentVurderingForBehandlingNy(behandling.id).unntak
            aktivitetspliktService.opprettOppfoelgingsoppgaveUnntak(unntakIBehandling, behandling.sak.id, doedsdato)
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke opprette oppfølgingsoppgaver for unntak i behandling med id ${behandling.id}",
                e,
            )
        }
    }

    private fun haandterFeilutbetaling(behandling: Behandling) {
        val brevutfall = behandlingInfoDao.hentBrevutfall(behandling.id)
        if (brevutfall?.feilutbetaling?.valg in
            listOf(
                FeilutbetalingValg.JA_VARSEL,
                FeilutbetalingValg.JA_INGEN_TK,
                FeilutbetalingValg.JA_INGEN_VARSEL_MOTREGNES,
            )
        ) {
            logger.info("Oppretter oppgave av type ${OppgaveType.TILBAKEKREVING} for behandling ${behandling.id}")

            val oppgaveFraBehandlingMedFeilutbetaling =
                oppgaveService
                    .hentOppgaverForSak(behandling.sak.id, OppgaveType.TILBAKEKREVING)
                    .filter { !it.erAvsluttet() }
                    .maxByOrNull { it.opprettet }

            if (oppgaveFraBehandlingMedFeilutbetaling != null) {
                logger.info("Det finnes allerede en oppgave under behandling på tilbakekreving for sak ${behandling.sak.id}")
                oppgaveService.endrePaaVent(
                    oppgaveId = oppgaveFraBehandlingMedFeilutbetaling.id,
                    aarsak = PaaVentAarsak.KRAVGRUNNLAG_SPERRET,
                    merknad = "Venter på oppdatert kravgrunnlag",
                    paavent = true,
                )
            } else {
                oppgaveService.opprettOppgave(
                    referanse = behandling.sak.id.toString(),
                    sakId = behandling.sak.id,
                    kilde = OppgaveKilde.TILBAKEKREVING,
                    type = OppgaveType.TILBAKEKREVING,
                    merknad = "Venter på kravgrunnlag",
                )
            }
        } else {
            logger.info("Behandling ${behandling.id} har ikke feilutbetaling")
        }
    }

    override fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakslisteDTO: SakslisteDTO) =
        inTransaction {
            val tilbakestilte =
                behandlingDao.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(sakslisteDTO.sakIdListe)
            val aapne = behandlingDao.hentAapneBehandlinger(sakslisteDTO.sakIdListe)
            SakIDListe(
                tilbakestilte,
                aapne,
            ).also { tilbakestilteSakIder ->
                oppgaveService.tilbakestillOppgaverUnderAttestering(tilbakestilteSakIder.tilbakestileBehandlinger.map { it.sakId })
            }
        }

    private fun registrerVedtakHendelse(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
        hendelseType: HendelseType,
    ) {
        behandlingService.registrerVedtakHendelse(
            behandlingId,
            vedtakHendelse,
            hendelseType,
        )
    }

    private fun Behandling.lagreEndring(
        dryRun: Boolean,
        saksbehandler: String,
    ) {
        if (dryRun) return
        lagreNyBehandlingStatus(this)
        behandlingService.registrerBehandlingHendelse(this, saksbehandler)
    }

    private fun lagreNyBehandlingStatus(behandling: Behandling) = behandlingDao.lagreStatus(behandling)

    private fun hentBehandling(behandlingId: UUID): Behandling =
        behandlingService.hentBehandling(behandlingId)
            ?: throw NotFoundException("Fant ikke behandling med id=$behandlingId")

    private fun genererMerknad(
        prefix: String? = null,
        vedtak: VedtakEndringDTO? = null,
        merknad: String,
    ): String =
        listOfNotNull(prefix, vedtak?.vedtakType?.tilLesbarString(), merknad, vedtak?.vedtakHendelse?.kommentar)
            .joinToString(separator = ": ")
}
