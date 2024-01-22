package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.sjekkEnhet
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class MaksEnBehandlingsOppgaveUnderbehandlingException(message: String) : Exception(message)

class RevurderingaarsakIkkeStoettetIMiljoeException(message: String) : Exception(message)

class RevurderingManglerIverksattBehandlingException(message: String) : Exception(message)

class RevurderingSluttbehandlingUtlandMaaHaEnBehandlingMedSkalSendeKravpakke(message: String) : Exception(message)

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
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val revurderingDao: RevurderingDao,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(RevurderingService::class.java)

    fun hentBehandling(id: UUID): Revurdering? = (behandlingDao.hentBehandling(id) as? Revurdering)?.sjekkEnhet()

    fun hentRevurderingsinfoForSakMedAarsak(
        sakId: Long,
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

    private fun maksEnOppgaveUnderbehandlingForKildeBehandling(sakId: Long) {
        val oppgaverForSak = oppgaveService.hentOppgaverForSak(sakId)
        val ingenBehandlingerUnderarbeid =
            oppgaverForSak.filter {
                it.kilde == OppgaveKilde.BEHANDLING
            }.none { it.status === Status.UNDER_BEHANDLING }
        if (ingenBehandlingerUnderarbeid) {
            return
        } else {
            throw MaksEnBehandlingsOppgaveUnderbehandlingException(
                "Sak $sakId har allerede en" +
                    " oppgave under behandling",
            )
        }
    }

    fun opprettManuellRevurderingWrapper(
        sakId: Long,
        aarsak: Revurderingaarsak,
        paaGrunnAvHendelseId: String?,
        begrunnelse: String?,
        fritekstAarsak: String? = null,
        saksbehandler: Saksbehandler,
    ): Revurdering? {
        if (!aarsak.kanBrukesIMiljo()) {
            throw RevurderingaarsakIkkeStoettetIMiljoeException(
                "Feil revurderingsårsak $aarsak, foreløpig ikke støttet",
            )
        }
        val paaGrunnAvHendelseUuid =
            try {
                paaGrunnAvHendelseId?.let { UUID.fromString(it) }
            } catch (e: Exception) {
                throw BadRequestException(
                    "$aarsak har en ugyldig hendelse id for sakid" +
                        " $sakId. " +
                        "Hendelsesid: $paaGrunnAvHendelseId",
                )
            }

        maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        val forrigeIverksatteBehandling =
            behandlingService.hentSisteIverksatte(sakId) ?: throw RevurderingManglerIverksattBehandlingException(
                "Kan ikke revurdere en sak uten iverksatt behandling sakid:" +
                    " $sakId",
            )
        val sakType = forrigeIverksatteBehandling.sak.sakType
        if (!aarsak.gyldigForSakType(sakType)) {
            throw BadRequestException("$aarsak er ikke støttet for $sakType")
        }
        kanOppretteRevurderingForAarsak(sakId, aarsak)
        return opprettManuellRevurdering(
            sakId = forrigeIverksatteBehandling.sak.id,
            forrigeBehandling = forrigeIverksatteBehandling,
            revurderingAarsak = aarsak,
            paaGrunnAvHendelse = paaGrunnAvHendelseUuid,
            begrunnelse = begrunnelse,
            fritekstAarsak = fritekstAarsak,
            saksbehandler = saksbehandler,
        )
    }

    private fun kanOppretteRevurderingForAarsak(
        sakId: Long,
        aarsak: Revurderingaarsak,
    ) {
        if (aarsak == Revurderingaarsak.SLUTTBEHANDLING_UTLAND) {
            val behandlingerForSak = behandlingService.hentBehandlingerForSak(sakId)
            behandlingerForSak.find { behandling ->
                (behandling is Foerstegangsbehandling) && behandling.boddEllerArbeidetUtlandet?.skalSendeKravpakke == true
            }
                ?: throw RevurderingSluttbehandlingUtlandMaaHaEnBehandlingMedSkalSendeKravpakke(
                    "Sak " +
                        "$sakId må ha en førstegangsbehandling som har huket av for skalSendeKravpakke for å kunne opprette ${aarsak.name}",
                )
        }
    }

    private fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        paaGrunnAvHendelse: UUID?,
        begrunnelse: String?,
        fritekstAarsak: String?,
        saksbehandler: Saksbehandler,
    ): Revurdering? =
        forrigeBehandling.sjekkEnhet()?.let {
            val persongalleri = runBlocking { grunnlagService.hentPersongalleri(forrigeBehandling.id) }

            opprettRevurdering(
                sakId = sakId,
                persongalleri = persongalleri,
                forrigeBehandling = forrigeBehandling.id,
                mottattDato = Tidspunkt.now().toLocalDatetimeUTC().toString(),
                prosessType = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = revurderingAarsak,
                virkningstidspunkt = null,
                utlandstilknytning = forrigeBehandling.utlandstilknytning,
                boddEllerArbeidetUtlandet = forrigeBehandling.boddEllerArbeidetUtlandet,
                begrunnelse = begrunnelse,
                fritekstAarsak = fritekstAarsak,
                saksbehandlerIdent = saksbehandler.ident,
            ).oppdater()
                .also { revurdering ->
                    if (paaGrunnAvHendelse != null) {
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIRevurdering(
                            paaGrunnAvHendelse,
                            revurdering.id,
                        )
                        try {
                            oppgaveService.ferdigStillOppgaveUnderBehandling(
                                paaGrunnAvHendelse.toString(),
                                saksbehandler,
                            )
                        } catch (e: Exception) {
                            logger.error(
                                "Kunne ikke ferdigstille oppgaven til hendelsen på grunn av feil, " +
                                    "men oppgave er ikke i bruk i miljø så feilen svelges.",
                                e,
                            )
                        }
                    }
                }
        }

    private fun behandlingErAvTypenRevurderingOgKanEndres(behandlingId: UUID) {
        val behandling = hentBehandling(behandlingId)
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

    // Denne burde nok ha vore private eller noko, men for å ikkje få ei altfor stor omskriving
    // gjer eg han til internal for no
    internal fun opprettRevurdering(
        sakId: Long,
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
        ).let { opprettBehandling ->
            behandlingDao.opprettBehandling(opprettBehandling)

            if (!fritekstAarsak.isNullOrEmpty()) {
                lagreRevurderingsaarsakFritekst(fritekstAarsak, opprettBehandling.id, saksbehandlerIdent)
            }

            forrigeBehandling?.let {
                kommerBarnetTilGodeService.hentKommerBarnetTilGode(it)
                    ?.copy(behandlingId = opprettBehandling.id)
                    ?.let { kopiert -> kommerBarnetTilGodeService.lagreKommerBarnetTilgode(kopiert) }
            }
            hendelseDao.behandlingOpprettet(opprettBehandling.toBehandlingOpprettet())

            logger.info("Opprettet behandling ${opprettBehandling.id} i sak ${opprettBehandling.sakId}")

            behandlingDao.hentBehandling(opprettBehandling.id)!! as Revurdering
        }.let {
            RevurderingOgOppfoelging(
                it,
                leggInnGrunnlag = { grunnlagService.leggInnNyttGrunnlag(it, persongalleri) },
                sendMeldingForHendelse = {
                    behandlingHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                        it.toStatistikkBehandling(persongalleri),
                        BehandlingHendelseType.OPPRETTET,
                    )
                },
                opprettOgTildelOppgave = {
                    val oppgave =
                        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                            referanse = it.id.toString(),
                            sakId = sakId,
                            oppgaveKilde = OppgaveKilde.BEHANDLING,
                            oppgaveType = OppgaveType.REVURDERING,
                            merknad = begrunnelse,
                        )
                    oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
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
