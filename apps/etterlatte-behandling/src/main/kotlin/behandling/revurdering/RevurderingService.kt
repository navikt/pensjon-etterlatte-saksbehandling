package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class MaksEnAktivOppgavePaaBehandling(
    sakId: SakId,
) : UgyldigForespoerselException(
        code = "MAKS_EN_AKTIV_OPPGAVE_PAA_BEHANDLING",
        detail = "Sak $sakId har allerede en oppgave under behandling",
    )

class RevurderingaarsakIkkeStoettet(
    aarsak: Revurderingaarsak,
) : UgyldigForespoerselException(
        code = "REVURDERINGAARSAK_IKKE_STOETTET",
        detail = "Revurderingsårsak \"$aarsak\" er foreløpig ikke støttet",
    )

class RevurderingManglerIverksattBehandling(
    sakId: SakId,
) : UgyldigForespoerselException(
        code = "REVURDERING_MANGLER_IVERKSATT_BEHANDLING",
        detail = "Sak $sakId kan ikke revurderes uten en iverksatt behandling",
    )

class UgyldigBehandlingTypeForRevurdering :
    IkkeTillattException(
        code = "BEHANDLING_MAA_VAERE_REVURDERING",
        detail = "Behandlingstypen må være revudering",
    )

class BehandlingKanIkkeEndres :
    IkkeTillattException(
        code = "BEHANDLINGEN_KAN_IKKE_ENDRES",
        detail = "Behandlingen kan ikke endres",
    )

data class RevurderingsinfoMedIdOgOpprettetDato(
    val revurderingInfo: RevurderingInfo,
    val behandlingOpprettet: LocalDateTime,
    val id: UUID,
)

class RevurderingService(
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagService,
    private val behandlingHendelser: BehandlingHendelserKafkaProducer,
    private val behandlingDao: BehandlingDao,
    private val hendelseDao: HendelseDao,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val revurderingDao: RevurderingDao,
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktKopierService: AktivitetspliktKopierService,
) {
    private val logger = LoggerFactory.getLogger(RevurderingService::class.java)

    fun hentBehandling(id: UUID): Revurdering? = (behandlingDao.hentBehandling(id) as? Revurdering)

    fun hentRevurderingsinfoForSakMedAarsak(
        sakId: SakId,
        revurderingAarsak: Revurderingaarsak,
    ): List<RevurderingsinfoMedIdOgOpprettetDato> {
        val hentAlleRevurderingerISakMedAarsak =
            behandlingDao.hentAlleRevurderingerISakMedAarsak(sakId, revurderingAarsak)

        return hentAlleRevurderingerISakMedAarsak
            .mapNotNull { if (it.revurderingInfo?.revurderingInfo != null) it else null }
            .map {
                RevurderingsinfoMedIdOgOpprettetDato(
                    it.revurderingInfo!!.revurderingInfo!!,
                    it.behandlingOpprettet,
                    it.id,
                )
            }
    }

    fun maksEnOppgaveUnderbehandlingForKildeBehandling(sakId: SakId) {
        val oppgaverForSak = oppgaveService.hentOppgaverForSak(sakId)
        if (oppgaverForSak
                .filter {
                    it.kilde == OppgaveKilde.BEHANDLING
                }.any { !it.erAvsluttet() }
        ) {
            throw MaksEnAktivOppgavePaaBehandling(sakId)
        }
    }

    private fun behandlingErAvTypenRevurderingOgKanEndres(behandlingId: UUID) {
        val behandling =
            hentBehandling(behandlingId)
        if (behandling?.type != BehandlingType.REVURDERING) {
            throw UgyldigBehandlingTypeForRevurdering()
        }
        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }
    }

    fun lagreRevurderingInfo(
        behandlingId: UUID,
        revurderingInfoMedBegrunnelse: RevurderingInfoMedBegrunnelse,
        navIdent: String,
    ) {
        behandlingErAvTypenRevurderingOgKanEndres(behandlingId)
        val kilde = Grunnlagsopplysning.Saksbehandler.create(navIdent)
        revurderingDao.lagreRevurderingInfo(behandlingId, revurderingInfoMedBegrunnelse, kilde)
    }

    internal fun opprettRevurdering(
        sakId: SakId,
        persongalleri: Persongalleri,
        forrigeBehandling: UUID?,
        mottattDato: String?,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: Virkningstidspunkt?,
        utlandstilknytning: Utlandstilknytning?,
        boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
        begrunnelse: String?,
        fritekstAarsak: String? = null,
        saksbehandlerIdent: String,
        relatertBehandlingId: String? = null,
        frist: Tidspunkt? = null,
        paaGrunnAvOppgave: UUID? = null,
        opphoerFraOgMed: YearMonth? = null,
        tidligereFamiliepleier: TidligereFamiliepleier? = null,
    ): RevurderingOgOppfoelging =
        OpprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sakId,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
            revurderingsAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            utlandstilknytning = utlandstilknytning,
            boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
            kilde = kilde,
            prosesstype = prosessType,
            begrunnelse = begrunnelse,
            fritekstAarsak = fritekstAarsak,
            relatertBehandlingId = relatertBehandlingId,
            sendeBrev = revurderingAarsak.skalSendeBrev,
            opphoerFraOgMed = opphoerFraOgMed,
            tidligereFamiliepleier = tidligereFamiliepleier,
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)

            if (!fritekstAarsak.isNullOrEmpty()) {
                lagreRevurderingsaarsakFritekst(fritekstAarsak, opprettBehandling.id, saksbehandlerIdent)
            }

            forrigeBehandling?.let { behandlingId ->
                kommerBarnetTilGodeService
                    .hentKommerBarnetTilGode(behandlingId)
                    ?.copy(behandlingId = opprettBehandling.id)
                    ?.let { kopiert -> kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kopiert) }
                aktivitetspliktDao.kopierAktiviteter(behandlingId, opprettBehandling.id)
                aktivitetspliktKopierService.kopierVurderingTilBehandling(sakId, opprettBehandling.id)
            }
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)!! as Revurdering
        }.let {
            RevurderingOgOppfoelging(
                it,
                leggInnGrunnlag = {
                    when (revurderingAarsak) {
                        Revurderingaarsak.REGULERING ->
                            runBlocking {
                                grunnlagService.laasTilGrunnlagIBehandling(
                                    it,
                                    checkNotNull(forrigeBehandling) {
                                        "Har en regulering som ikke sender med behandlingId for sist iverksatt. " +
                                            "Da kan vi ikke legge inn riktig grunnlag. regulering id=${it.id}"
                                    },
                                    HardkodaSystembruker.opprettGrunnlag,
                                )
                            }

                        else ->
                            runBlocking {
                                grunnlagService.leggInnNyttGrunnlag(
                                    it,
                                    persongalleri,
                                    HardkodaSystembruker.opprettGrunnlag,
                                )
                            }
                    }
                },
                sendMeldingForHendelse = {
                    behandlingHendelser.sendMeldingForHendelseStatisitkk(
                        it.toStatistikkBehandling(persongalleri),
                        BehandlingHendelseType.OPPRETTET,
                    )
                },
                opprettOgTildelOppgave = {
                    if (paaGrunnAvOppgave != null) {
                        (
                            oppgaveService.endreTilKildeBehandlingOgOppdaterReferanse(
                                paaGrunnAvOppgave,
                                it.id.toString(),
                            )
                        )
                    } else {
                        val oppgave =
                            oppgaveService.opprettOppgave(
                                referanse = it.id.toString(),
                                sakId = sakId,
                                kilde = OppgaveKilde.BEHANDLING,
                                type = OppgaveType.REVURDERING,
                                merknad = begrunnelse,
                                frist = frist,
                            )
                        if ((prosessType == Prosesstype.MANUELL && saksbehandlerIdent != Fagsaksystem.EY.navn) ||
                            (prosessType == Prosesstype.AUTOMATISK && saksbehandlerIdent == Fagsaksystem.EY.navn)
                        ) {
                            oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
                        } else {
                            oppgaveService.oppdaterStatusOgMerknad(oppgave.id, "", Status.UNDER_BEHANDLING)
                        }
                    }
                },
            )
        }

    private fun lagreRevurderingsaarsakFritekst(
        fritekstAarsak: String,
        behandlingId: UUID,
        saksbehandlerIdent: String,
    ) {
        val revurderingInfo = RevurderingInfo.RevurderingAarsakAnnen(fritekstAarsak)
        lagreRevurderingInfo(behandlingId, RevurderingInfoMedBegrunnelse(revurderingInfo, null), saksbehandlerIdent)
    }
}

data class RevurderingOgOppfoelging(
    val revurdering: Revurdering,
    val leggInnGrunnlag: () -> Unit,
    val opprettOgTildelOppgave: () -> Unit,
    val sendMeldingForHendelse: () -> Unit,
) {
    fun oppdater(): Revurdering {
        leggInnGrunnlag()
        opprettOgTildelOppgave()
        sendMeldingForHendelse()
        return revurdering
    }

    fun behandlingId() = revurdering.id

    fun sakType() = revurdering.sak.sakType
}
