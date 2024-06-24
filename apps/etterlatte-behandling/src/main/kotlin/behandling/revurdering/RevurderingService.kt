package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktKopierService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.toBehandlingOpprettet
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class MaksEnAktivOppgavePaaBehandling(
    sakId: Long,
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
    sakId: Long,
) : UgyldigForespoerselException(
        code = "REVURDERING_MANGLER_IVERKSATT_BEHANDLING",
        detail = "Sak $sakId kan ikke revurderes uten en iverksatt behandling",
    )

class RevurderingSluttbehandlingUtlandMaaHaEnBehandlingMedSkalSendeKravpakke(
    message: String,
) : Exception(message)

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
    private val klageService: KlageService,
    private val behandlingService: BehandlingService,
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktKopierService: AktivitetspliktKopierService,
    private val revurderingKopierGrunnlag: RevurderingKopierGrunnlag,
) {
    private val logger = LoggerFactory.getLogger(RevurderingService::class.java)

    fun hentBehandling(id: UUID): Revurdering? = (behandlingDao.hentBehandling(id) as? Revurdering)

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

    fun maksEnOppgaveUnderbehandlingForKildeBehandling(sakId: Long) {
        val oppgaverForSak = oppgaveService.hentOppgaverForSak(sakId)
        if (oppgaverForSak
                .filter {
                    it.kilde == OppgaveKilde.BEHANDLING
                }.any { !it.erAvsluttet() }
        ) {
            throw MaksEnAktivOppgavePaaBehandling(sakId)
        }
    }

    fun opprettManuellRevurderingWrapper(
        sakId: Long,
        aarsak: Revurderingaarsak,
        paaGrunnAvHendelseId: String?,
        paaGrunnAvOppgaveId: String? = null,
        begrunnelse: String?,
        fritekstAarsak: String? = null,
        saksbehandler: Saksbehandler,
    ): Revurdering {
        if (!aarsak.kanBrukesIMiljo()) {
            throw RevurderingaarsakIkkeStoettet(aarsak)
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

        val paaGrunnAvOppgaveUuid =
            try {
                paaGrunnAvOppgaveId?.let { UUID.fromString(it) }
            } catch (e: Exception) {
                throw BadRequestException("Ugyldig oppgaveId $paaGrunnAvOppgaveId (sakid=$sakId).")
            }

        maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        val forrigeIverksatteBehandling =
            behandlingService.hentSisteIverksatte(sakId)
                ?: throw RevurderingManglerIverksattBehandling(sakId)

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
            paaGrunnAvOppgave = paaGrunnAvOppgaveUuid,
            begrunnelse = begrunnelse,
            fritekstAarsak = fritekstAarsak,
            saksbehandler = saksbehandler,
            opphoerFraOgMed = forrigeIverksatteBehandling.opphoerFraOgMed,
        ).also {
            revurderingKopierGrunnlag.kopier(it.id, forrigeIverksatteBehandling.id, saksbehandler)
        }
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
        } else if (aarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
            throw UgyldigForespoerselException(
                code = "OMGJOERING_KLAGE_MAA_OPPRETTES_FRA_OPPGAVE",
                detail = "Omgjøring etter klage må opprettes fra en omgjøringsoppgave for å koble omgjøringen til klagen riktig.",
            )
        }
    }

    private fun opprettManuellRevurdering(
        sakId: Long,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        paaGrunnAvHendelse: UUID?,
        paaGrunnAvOppgave: UUID?,
        begrunnelse: String?,
        fritekstAarsak: String?,
        saksbehandler: Saksbehandler,
        opphoerFraOgMed: YearMonth? = null,
    ): Revurdering =
        forrigeBehandling.let {
            val persongalleri = runBlocking { grunnlagService.hentPersongalleri(forrigeBehandling.id) }
            val triggendeOppgave = paaGrunnAvOppgave?.let { oppgaveService.hentOppgave(it) }

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
                begrunnelse = begrunnelse ?: triggendeOppgave?.merknad,
                fritekstAarsak = fritekstAarsak,
                saksbehandlerIdent = saksbehandler.ident,
                frist = triggendeOppgave?.frist,
                paaGrunnAvOppgave = paaGrunnAvOppgave,
                opphoerFraOgMed = opphoerFraOgMed,
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
                                OppgaveType.VURDER_KONSEKVENS,
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
        relatertBehandlingId: String? = null,
        frist: Tidspunkt? = null,
        paaGrunnAvOppgave: UUID? = null,
        opphoerFraOgMed: YearMonth? = null,
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
                aktivitetspliktKopierService.kopierVurdering(sakId, opprettBehandling.id)
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
                            grunnlagService.laasTilGrunnlagIBehandling(
                                it,
                                checkNotNull(forrigeBehandling) {
                                    "Har en regulering som ikke sender med behandlingId for sist iverksatt. " +
                                        "Da kan vi ikke legge inn riktig grunnlag. regulering id=${it.id}"
                                },
                            )
                        else -> grunnlagService.leggInnNyttGrunnlag(it, persongalleri)
                    }
                },
                sendMeldingForHendelse = {
                    behandlingHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                        it.toStatistikkBehandling(persongalleri),
                        BehandlingHendelseType.OPPRETTET,
                    )
                },
                opprettOgTildelOppgave = {
                    if (paaGrunnAvOppgave != null) {
                        (
                            oppgaveService.endreTilKildeBehandlingOgOppdaterReferanse(paaGrunnAvOppgave, it.id.toString())
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
                        oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
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

    fun opprettOmgjoeringKlage(
        sakId: Long,
        oppgaveIdOmgjoering: UUID,
        saksbehandler: Saksbehandler,
    ): Revurdering {
        val omgjoeringsoppgave = oppgaveService.hentOppgave(oppgaveIdOmgjoering)
        if (omgjoeringsoppgave.type != OppgaveType.OMGJOERING) {
            throw FeilIOmgjoering.IngenOmgjoeringsoppgave()
        }
        if (omgjoeringsoppgave.status.erAvsluttet()) {
            throw FeilIOmgjoering.OmgjoeringsOppgaveLukket(omgjoeringsoppgave)
        }
        if (omgjoeringsoppgave.sakId != sakId) {
            throw FeilIOmgjoering.OppgaveOgSakErForskjellig(sakId, omgjoeringsoppgave)
        }
        if (omgjoeringsoppgave.saksbehandler?.ident != saksbehandler.ident) {
            throw FeilIOmgjoering.SaksbehandlerHarIkkeOppgaven(saksbehandler, omgjoeringsoppgave)
        }

        val klageId = UUID.fromString(omgjoeringsoppgave.referanse)
        val klagenViOmgjoerPaaGrunnAv =
            klageService.hentKlage(klageId)
                ?: throw InternfeilException(
                    "Omgjøringsoppgaven med id=${omgjoeringsoppgave.id} peker på en " +
                        "klageId=${omgjoeringsoppgave.referanse} som vi ikke finner. " +
                        "Her har noe blitt koblet feil, og må ryddes opp i.",
                )

        val behandlingSomOmgjoeresId =
            klagenViOmgjoerPaaGrunnAv.formkrav
                ?.formkrav
                ?.vedtaketKlagenGjelder
                ?.behandlingId
                ?.let { UUID.fromString(it) }
                ?: throw FeilIOmgjoering.ManglerBehandlingForOmgjoering(klagenViOmgjoerPaaGrunnAv)
        val behandlingSomOmgjoeres =
            behandlingDao.hentBehandling(behandlingSomOmgjoeresId)
                ?: throw FeilIOmgjoering.ManglerBehandlingForOmgjoering(klagenViOmgjoerPaaGrunnAv)

        val persongalleri = runBlocking { grunnlagService.hentPersongalleri(behandlingSomOmgjoeres.id) }
        return opprettRevurdering(
            sakId = sakId,
            persongalleri = persongalleri,
            forrigeBehandling = behandlingSomOmgjoeresId,
            mottattDato =
                klagenViOmgjoerPaaGrunnAv.innkommendeDokument
                    ?.mottattDato
                    ?.atStartOfDay()
                    ?.toString(),
            prosessType = Prosesstype.MANUELL,
            kilde = Vedtaksloesning.GJENNY,
            revurderingAarsak = Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
            virkningstidspunkt = behandlingSomOmgjoeres.virkningstidspunkt,
            utlandstilknytning = behandlingSomOmgjoeres.utlandstilknytning,
            boddEllerArbeidetUtlandet = behandlingSomOmgjoeres.boddEllerArbeidetUtlandet,
            begrunnelse = "Omgjøring på grunn av klage",
            fritekstAarsak = omgjoeringsoppgave.merknad,
            saksbehandlerIdent = saksbehandler.ident,
            relatertBehandlingId = klagenViOmgjoerPaaGrunnAv.id.toString(),
        ).oppdater().also {
            oppgaveService.ferdigStillOppgaveUnderBehandling(
                referanse = klagenViOmgjoerPaaGrunnAv.id.toString(),
                type = OppgaveType.OMGJOERING,
                saksbehandler = saksbehandler,
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

sealed class FeilIOmgjoering {
    class IngenOmgjoeringsoppgave :
        UgyldigForespoerselException("INGEN_OMGJOERINGSOPPGAVE", "Mottok ikke en gyldig omgjøringsoppgave")

    class OmgjoeringsOppgaveLukket(
        oppgave: OppgaveIntern,
    ) : UgyldigForespoerselException(
            "OMGJOERINGSOPPGAVE_LUKKET",
            "Oppgaven ${oppgave.id} har status ${oppgave.status}.",
        )

    class OppgaveOgSakErForskjellig(
        sakId: Long,
        oppgave: OppgaveIntern,
    ) : UgyldigForespoerselException(
            "SAK_I_OPPGAVE_MATCHER_IKKE",
            "Saken det skal omgjøres i har id=$sakId, men omgjøringsoppgaven er i sak med id=${oppgave.sakId}",
        )

    class SaksbehandlerHarIkkeOppgaven(
        saksbehandler: Saksbehandler,
        omgjoeringsoppgave: OppgaveIntern,
    ) : UgyldigForespoerselException(
            "SAKSBEHANDLER_HAR_IKKE_OPPGAVEN",
            "Saksbehandler med ident=${saksbehandler.ident} er ikke saksbehandler i oppgaven med " +
                "id=$omgjoeringsoppgave (saksbehandler i oppgaven er ${omgjoeringsoppgave.saksbehandler?.ident}).",
        )

    class ManglerBehandlingForOmgjoering(
        klage: Klage,
    ) : InternfeilException(
            "Klagen med id=${klage.id} har laget en omgjøringsoppgave men vi finner ikke behandlingen som skal omgjøres." +
                " Noe galt har skjedd i ferdigstillingen av denne klagen, eller dette er ikke et behandlingsvedtak.",
        )
}

enum class RevurderingFeatureToggle(
    private val key: String,
) : FeatureToggle {
    KopierGrunnlag("Revurdering_kopier_grunnlag"),
    ;

    override fun key() = key
}
