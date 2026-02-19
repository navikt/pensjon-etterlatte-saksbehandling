package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpphoerFraTidligereBehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagUtils.opplysningsbehov
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Locale
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
                    it.kilde == OppgaveKilde.BEHANDLING &&
                        it.type != OppgaveType.OMGJOERING
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
        forrigeBehandling: Behandling,
        persongalleri: Persongalleri,
        mottattDato: String?,
        prosessType: Prosesstype,
        kilde: Vedtaksloesning,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: Virkningstidspunkt?,
        begrunnelse: String?,
        saksbehandlerIdent: String?,
        opprinnelse: BehandlingOpprinnelse,
        relatertBehandlingId: String? = null,
        frist: Tidspunkt? = null,
        paaGrunnAvOppgave: UUID? = null,
        opphoerFraOgMed: OpphoerFraTidligereBehandling? = null,
    ): RevurderingOgOppfoelging =
        OpprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sakId,
            status = BehandlingStatus.OPPRETTET,
            soeknadMottattDato = mottattDato?.let { LocalDateTime.parse(it) },
            revurderingsAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            utlandstilknytning = forrigeBehandling.utlandstilknytning,
            boddEllerArbeidetUtlandet = forrigeBehandling.boddEllerArbeidetUtlandet,
            vedtaksloesning = kilde,
            prosesstype = prosessType,
            begrunnelse = begrunnelse,
            relatertBehandlingId = relatertBehandlingId,
            sendeBrev = revurderingAarsak.skalSendeBrev,
            opphoer = opphoerFraOgMed ?: opphoerFraBehandling(forrigeBehandling),
            tidligereFamiliepleier = forrigeBehandling.tidligereFamiliepleier,
            opprinnelse = opprinnelse,
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)

            forrigeBehandling.id.let { behandlingId ->
                kommerBarnetTilGodeService
                    .hentKommerBarnetTilGode(behandlingId)
                    ?.copy(behandlingId = opprettBehandling.id)
                    ?.let { kopiert -> kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kopiert) }
                aktivitetspliktDao.kopierAktiviteter(behandlingId, opprettBehandling.id)
                aktivitetspliktKopierService.kopierVurderingTilBehandling(sakId, opprettBehandling.id)
                kopierViderefoertOpphoer(opprettBehandling, saksbehandlerIdent)
            }
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)!! as Revurdering
        }.let {
            RevurderingOgOppfoelging(
                it,
                leggInnGrunnlag = {
                    val kanHenteNyttGrunnlag =
                        when (revurderingAarsak) {
                            Revurderingaarsak.REGULERING,
                            Revurderingaarsak.OMREGNING,
                            -> false

                            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING,
                            -> it.prosesstype != Prosesstype.AUTOMATISK

                            else -> true
                        }
                    runBlocking {
                        if (kanHenteNyttGrunnlag) {
                            grunnlagService.opprettGrunnlag(
                                it.id,
                                opplysningsbehov(it.sak, persongalleri),
                            )
                        } else {
                            grunnlagService.laasTilVersjonForBehandling(
                                it.id,
                                krevIkkeNull(forrigeBehandling.id) {
                                    "Har en automatisk behandling som ikke sender med behandlingId for sist iverksatt. " +
                                        "Da kan vi ikke legge inn riktig grunnlag. Automatisk behandling id=${it.id}"
                                },
                            )
                        }
                    }
                },
                sendMeldingForHendelse = {
                    behandlingHendelser.sendMeldingForHendelseStatistikk(
                        it.toStatistikkBehandling(persongalleri),
                        BehandlingHendelseType.OPPRETTET,
                    )
                },
                opprettOgTildelOppgave = {
                    if (paaGrunnAvOppgave != null) {
                        (
                            oppgaveService.endreTilKildeBehandlingOgOppdaterReferanseOgMerknad(
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
                                merknad =
                                    begrunnelse ?: revurderingAarsak.lesbar(),
                                frist = frist,
                                gruppeId = persongalleri.avdoed.firstOrNull(),
                            )
                        if (
                            (
                                saksbehandlerIdent != null &&
                                    (prosessType == Prosesstype.MANUELL && saksbehandlerIdent != Fagsaksystem.EY.navn)
                            ) ||
                            (prosessType == Prosesstype.AUTOMATISK && saksbehandlerIdent == Fagsaksystem.EY.navn)
                        ) {
                            oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
                        }
                    }
                },
            )
        }

    private fun opphoerFraBehandling(behandling: Behandling): OpphoerFraTidligereBehandling? =
        behandling.opphoerFraOgMed?.let { opphoerFraOgMed ->
            OpphoerFraTidligereBehandling(
                opphoerFraOgMed,
                behandling.id,
            )
        }

    fun fjernSaksbehandlerFraRevurderingsOppgave(revurdering: Revurdering) {
        val revurderingsOppgave =
            oppgaveService
                .hentOppgaverForReferanse(revurdering.id.toString())
                .find { it.type == OppgaveType.REVURDERING }

        if (revurderingsOppgave != null) {
            oppgaveService.fjernSaksbehandler(revurderingsOppgave.id)
        } else {
            logger.warn("Fant ikke oppgave for revurdering av aktivitetsplikt for sak ${revurdering.sak.id}")
        }
    }

    fun lagreRevurderingsaarsakFritekstForRevurderingAnnenMedEllerUtenBrev(
        fritekstAarsak: String,
        revurdering: Revurdering,
        saksbehandlerIdent: String,
    ) {
        val revurderingInfo =
            when (revurdering.revurderingsaarsak) {
                Revurderingaarsak.ANNEN -> RevurderingInfo.RevurderingAarsakAnnen(fritekstAarsak)
                Revurderingaarsak.ANNEN_UTEN_BREV -> RevurderingInfo.RevurderingAarsakAnnenUtenBrev(fritekstAarsak)
                else -> throw FeilRevurderingAarsakForFritekstLagring(revurdering)
            }

        lagreRevurderingInfo(revurdering.id, RevurderingInfoMedBegrunnelse(revurderingInfo, null), saksbehandlerIdent)
    }

    /**
     * Sjekker om det er satt opphoerFom i forrige behandling. Hvis så skal ViderefoertOpphoer også kopieres med.
     * Dersom ViderefoertOpphoer ikke er satt (dette kan feks skje dersom en revrudering kun inneholder et opphør,
     * opprettes det en ny ViderefoertOpphoer uten vilkår satt. Dette fordi det ikke er trivielt å utlede fra
     * den forrige behandlingens vilkårsvurdering.
     */
    private fun kopierViderefoertOpphoer(
        opprettBehandling: OpprettBehandling,
        saksbehandlerIdent: String?,
    ) {
        if (opprettBehandling.opphoer != null) {
            val viderefoertOpphoer = behandlingDao.hentViderefoertOpphoer(opprettBehandling.opphoer.behandlingId)
            loggHvisViderefoertOpphoerIkkeViderefoerer(viderefoertOpphoer)

            if (viderefoertOpphoer != null && viderefoertOpphoer.skalViderefoere == JaNei.JA) {
                loggHvisAvvikendeOpphoersdato(viderefoertOpphoer, opprettBehandling)
                logger.info("Lagrer tidligere opprettet videreført opphør i behandling ${opprettBehandling.id}")
                behandlingDao.lagreViderefoertOpphoer(
                    opprettBehandling.id,
                    viderefoertOpphoer.copy(
                        skalViderefoere = JaNei.JA,
                        behandlingId = opprettBehandling.id,
                        dato = opprettBehandling.opphoer.opphoerFraOgMed,
                    ),
                )
            } else {
                logger.info("Oppretter nytt videreført opphør i behandling ${opprettBehandling.id}")
                behandlingDao.lagreViderefoertOpphoer(
                    opprettBehandling.id,
                    ViderefoertOpphoer(
                        skalViderefoere = JaNei.JA,
                        behandlingId = opprettBehandling.id,
                        dato = opprettBehandling.opphoer.opphoerFraOgMed,
                        vilkaar = null,
                        begrunnelse = "Automatisk videreført fra eksisterende opphør",
                        kilde =
                            saksbehandlerIdent?.let { Grunnlagsopplysning.Saksbehandler(it, Tidspunkt.now()) }
                                ?: Grunnlagsopplysning.automatiskSaksbehandler,
                    ),
                )
            }
        }
    }

    private fun loggHvisViderefoertOpphoerIkkeViderefoerer(viderefoertOpphoer: ViderefoertOpphoer?) {
        if (viderefoertOpphoer != null && viderefoertOpphoer.skalViderefoere == JaNei.NEI) {
            logger.error("Viderefører opphør fra behandling ${viderefoertOpphoer.behandlingId}")
        }
    }

    private fun loggHvisAvvikendeOpphoersdato(
        viderefoertOpphoer: ViderefoertOpphoer,
        opprettBehandling: OpprettBehandling,
    ) {
        val nyOpphoerFraOgMed = opprettBehandling.opphoer?.opphoerFraOgMed
        if (viderefoertOpphoer.dato != nyOpphoerFraOgMed) {
            logger.error(
                """
                Ny behandling med id=${opprettBehandling.id} har opphoerFraOgMed=$nyOpphoerFraOgMed,
                mens videreført opphør fra tidligere behandling ${viderefoertOpphoer.behandlingId}
                har opphoerFraOgMed=${viderefoertOpphoer.dato}.
                """.trimIndent(),
            )
        }
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

fun Revurderingaarsak.lesbar(): String =
    this.name.lowercase().replace("_", " ").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

class FeilRevurderingAarsakForFritekstLagring(
    revurdering: Revurdering,
) : InternfeilException(
        detail = "Prøvde å lagre revurdering info annen/annen uten brev med årsak ${revurdering.revurderingsaarsak} id: ${revurdering.id}",
    )
