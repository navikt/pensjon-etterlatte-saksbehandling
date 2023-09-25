package no.nav.etterlatte.behandling

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.generellbehandling.GenerellBehandlingToggle
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

enum class BehandlingStatusServiceFeatureToggle(private val key: String) : FeatureToggle {
    BrukFaktiskTrygdetid("pensjon-etterlatte.bp-bruk-faktisk-trygdetid"),
    ;

    override fun key() = key
}

interface BehandlingStatusService {
    fun settOpprettet(
        behandlingId: UUID,
        dryRun: Boolean = true,
    )

    fun settVilkaarsvurdert(
        behandlingId: UUID,
        dryRun: Boolean = true,
    )

    fun settTrygdetidOppdatert(
        behandlingId: UUID,
        dryRun: Boolean = true,
    )

    fun settBeregnet(
        behandlingId: UUID,
        dryRun: Boolean = true,
    )

    fun settAvkortet(
        behandlingId: UUID,
        dryRun: Boolean = true,
    )

    fun sjekkOmKanFatteVedtak(behandlingId: UUID)

    fun settFattetVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    )

    fun sjekkOmKanAttestere(behandlingId: UUID)

    fun settAttestertVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    )

    fun sjekkOmKanReturnereVedtak(behandlingId: UUID)

    fun settReturnertVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    )

    fun settIverksattVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    )

    fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning(): SakIDListe
}

class BehandlingStatusServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val behandlingService: BehandlingService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
    private val generellBehandlingService: GenerellBehandlingService,
) : BehandlingStatusService {
    private val logger = LoggerFactory.getLogger(BehandlingStatusServiceImpl::class.java)

    override fun settOpprettet(
        behandlingId: UUID,
        dryRun: Boolean,
    ) {
        val behandling = hentBehandling(behandlingId).tilOpprettet()

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
            }
        }
    }

    override fun settVilkaarsvurdert(
        behandlingId: UUID,
        dryRun: Boolean,
    ) {
        val behandling = hentBehandling(behandlingId).tilVilkaarsvurdert()

        if (!dryRun) {
            inTransaction {
                behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)
            }
        }
    }

    override fun settTrygdetidOppdatert(
        behandlingId: UUID,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilTrygdetidOppdatert().lagreEndring(dryRun)
    }

    override fun settBeregnet(
        behandlingId: UUID,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilBeregnet(
            !featureToggleService.isEnabled(
                BehandlingStatusServiceFeatureToggle.BrukFaktiskTrygdetid,
                false,
            ),
        ).lagreEndring(dryRun)
    }

    override fun settAvkortet(
        behandlingId: UUID,
        dryRun: Boolean,
    ) {
        hentBehandling(behandlingId).tilAvkortet().lagreEndring(dryRun)
    }

    override fun sjekkOmKanFatteVedtak(behandlingId: UUID) {
        hentBehandling(behandlingId).tilFattetVedtak()
    }

    override fun settFattetVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    ) {
        lagreNyBehandlingStatus(behandling.tilFattetVedtak())
        registrerVedtakHendelse(behandling.id, vedtakHendelse, HendelseType.FATTET)
    }

    override fun sjekkOmKanAttestere(behandlingId: UUID) {
        hentBehandling(behandlingId).tilAttestert()
    }

    override fun settAttestertVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    ) {
        lagreNyBehandlingStatus(behandling.tilAttestert())
        registrerVedtakHendelse(behandling.id, vedtakHendelse, HendelseType.ATTESTERT)
    }

    override fun sjekkOmKanReturnereVedtak(behandlingId: UUID) {
        hentBehandling(behandlingId).tilReturnert()
    }

    override fun settReturnertVedtak(
        behandling: Behandling,
        vedtakHendelse: VedtakHendelse,
    ) {
        lagreNyBehandlingStatus(behandling.tilReturnert())
        registrerVedtakHendelse(behandling.id, vedtakHendelse, HendelseType.UNDERKJENT)
    }

    override fun settIverksattVedtak(
        behandlingId: UUID,
        vedtakHendelse: VedtakHendelse,
    ) {
        val behandling = hentBehandling(behandlingId)
        inTransaction {
            lagreNyBehandlingStatus(behandling.tilIverksatt(), Tidspunkt.now().toLocalDatetimeUTC())
            registrerVedtakHendelse(behandlingId, vedtakHendelse, HendelseType.IVERKSATT)
            haandterUtland(behandling)
        }
        if (behandling.type == BehandlingType.REVURDERING) {
            grunnlagsendringshendelseService.settHendelseTilHistorisk(behandlingId)
        }
    }

    private fun haandterUtland(behandling: Behandling) {
        if (featureToggleService.isEnabled(GenerellBehandlingToggle.KanBrukeGenerellBehandlingToggle, false)) {
            if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
                if (behandling.boddEllerArbeidetUtlandet?.skalSendeKravpakke == true) {
                    val oppgaveUtland =
                        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                            behandling.id.toString(),
                            behandling.sak.id,
                            OppgaveKilde.BEHANDLING,
                            OppgaveType.UTLAND,
                            null,
                        )
                    generellBehandlingService.opprettBehandling(
                        GenerellBehandling.opprettFraType(
                            GenerellBehandling.GenerellBehandlingType.UTLAND,
                            behandling.sak.id,
                        ),
                    )
                    val saksbehandlerFoerstegangsbehandling =
                        oppgaveService.hentSaksbehandlerFraFoerstegangsbehandling(behandlingsId = behandling.id)
                    if (saksbehandlerFoerstegangsbehandling != null) {
                        oppgaveService.tildelSaksbehandler(oppgaveUtland.id, saksbehandlerFoerstegangsbehandling)
                        logger.info(
                            "Opprettet generell behandling for utland for sak: ${behandling.sak.id} " +
                                "og behandling: ${behandling.id}. Gjelder oppgave: ${oppgaveUtland.id}",
                        )
                    } else {
                        logger.error("Fant ingen saksbehandler for behandling oppgave fatting, id: ${behandling.id}")
                    }
                } else {
                    logger.info("behandling ${behandling.id} har ikke satt skalSendeKravpakke=true")
                }
            } else {
                logger.info("Behandlingtype: ${behandling.type} får ikke utlandsoppgave")
            }
        } else {
            logger.info("Håndterer ikke utland for behandling ${behandling.id}")
        }
    }

    override fun migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning() =
        inTransaction {
            behandlingDao.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()
        }

    fun registrerVedtakHendelse(
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

    private fun Behandling.lagreEndring(dryRun: Boolean) {
        if (dryRun) return
        inTransaction {
            lagreNyBehandlingStatus(this)
        }
    }

    private fun lagreNyBehandlingStatus(
        behandling: Behandling,
        sistEndret: LocalDateTime,
    ) = behandlingDao.lagreStatus(behandling.id, behandling.status, sistEndret)

    private fun lagreNyBehandlingStatus(behandling: Behandling) =
        behandlingDao.lagreStatus(behandling.id, behandling.status, behandling.sistEndret)

    private fun hentBehandling(behandlingId: UUID): Behandling =
        behandlingService.hentBehandling(behandlingId)
            ?: throw NotFoundException("Fant ikke behandling med id=$behandlingId")
}
