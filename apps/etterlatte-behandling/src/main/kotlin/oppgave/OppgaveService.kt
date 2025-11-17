package no.nav.etterlatte.oppgave

import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.Self
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgavebenkStats
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUt
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.UUID

class OppgaveService(
    private val oppgaveDao: OppgaveDaoMedEndringssporing,
    private val sakDao: SakLesDao,
    private val hendelseDao: HendelseDao,
    private val hendelser: BehandlingHendelserKafkaProducer,
    private val saksbehandlerService: SaksbehandlerService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun finnOppgaverForBruker(
        bruker: SaksbehandlerMedEnheterOgRoller,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String? = null,
    ): List<OppgaveIntern> =
        oppgaveDao
            .hentOppgaver(
                bruker.enheter(),
                oppgaveStatuser,
                minOppgavelisteIdentFilter,
            ).sortedByDescending { it.opprettet }

    fun genererStatsForOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats =
        oppgaveDao.hentAntallOppgaver(innloggetSaksbehandlerIdent)

    private fun sjekkOmkanTildeleAttestantOppgave(saksbehandler: String): Boolean {
        val appUser = Kontekst.get().AppUser
        return when (appUser) {
            is SystemUser -> true
            is Self -> true
            is SaksbehandlerMedEnheterOgRoller -> {
                val saksbehandlerMedRoller = appUser.saksbehandlerMedRoller
                if (saksbehandler == appUser.name()) {
                    if (saksbehandlerMedRoller.harRolleAttestant()) {
                        return true
                    } else {
                        throw BrukerManglerAttestantRolleException(saksbehandlerMedRoller.saksbehandler.ident)
                    }
                } else {
                    true
                }
            }

            is ExternalUser -> throw IllegalArgumentException("ExternalUser er ikke støttet for å tildele oppgave")
            else -> throw IllegalArgumentException(
                "Ukjent brukertype ${appUser.name()} støtter ikke tildeling av oppgave",
            )
        }
    }

    fun tildelSaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId)
                ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        hentetOppgave.erAttestering() && sjekkOmkanTildeleAttestantOppgave(saksbehandler)

        oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)

        // Må ha denne for å ikke overskride attesteringstatus på oppgaver
        if (hentetOppgave.status == Status.NY) {
            oppgaveDao.endreStatusPaaOppgave(oppgaveId, Status.UNDER_BEHANDLING)
        }
    }

    fun tildelSaksbehandlerBulk(
        oppgaveIds: List<UUID>,
        nyTildeling: String,
        saksbehandler: Saksbehandler,
    ) {
        val enheterForSaksbehandler =
            saksbehandlerService
                .hentEnheterForSaksbehandlerIdentWrapper(saksbehandler.ident)
                .map { it.enhetsNummer }
        oppgaveIds.forEach {
            val oppgave = oppgaveDao.hentOppgave(it) ?: throw OppgaveIkkeFunnet(it)
            if (oppgave.enhet !in enheterForSaksbehandler) {
                throw IkkeTillattException(
                    "HAR_IKKE_TILGANG_TIL_SAK",
                    "Kan ikke tildele saksbehandler i en sak man ikke har tilgang i.",
                )
            }
            tildelSaksbehandler(it, nyTildeling)
        }
    }

    fun fjernSaksbehandler(oppgaveId: UUID) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)

        if (hentetOppgave.saksbehandler != null) {
            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        } else {
            logger.warn("Ingen saksbehandler å fjerne på oppgave med id: $oppgaveId")
        }
    }

    fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    ) {
        if (frist.isBefore(Tidspunkt.now())) {
            throw FristTilbakeITid(oppgaveId)
        }
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)
        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        if (hentetOppgave.saksbehandler?.ident.isNullOrBlank()) {
            throw OppgaveIkkeTildeltSaksbehandler(oppgaveId)
        } else {
            oppgaveDao.redigerFrist(oppgaveId, frist)
        }
    }

    fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
        status: Status,
    ) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)
        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, merknad, status)
    }

    fun endreTilKildeBehandlingOgOppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String? = null,
    ): OppgaveIntern {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        if (hentetOppgave.saksbehandler?.ident.isNullOrBlank()) {
            logger.warn(
                "Saksbehandler ikke satt på oppgave som vi oppretter behandling ut i fra, " +
                    "oppgaveId=${hentetOppgave.id}, saksbehandler satt på oppgave " +
                    "er ${hentetOppgave.saksbehandler?.ident}",
            )
        }
        oppgaveDao.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId, referanse)

        if (!merknad.isNullOrEmpty()) {
            oppgaveDao.oppdaterMerknad(oppgaveId, merknad)
        }

        return oppgaveDao.hentOppgave(oppgaveId)!!
    }

    fun oppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String,
    ): OppgaveIntern {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)

        // Krever ikke at saksbehandler er tildelt for dette
        oppgaveDao.oppdaterReferanseOgMerknad(oppgaveId, referanse, merknad)
        return oppgaveDao.hentOppgave(oppgaveId)!!
    }

    fun endrePaaVent(
        oppgaveId: UUID,
        paavent: Boolean,
        merknad: String,
        aarsak: PaaVentAarsak?,
    ): OppgaveIntern {
        val oppgave = hentOppgave(oppgaveId)
        if (paavent && oppgave.status == Status.PAA_VENT) return oppgave
        if (!paavent && oppgave.status != Status.PAA_VENT) return oppgave

        if (paavent && aarsak == null) {
            throw UgyldigForespoerselException(
                "MANGLER_AARSAK_PAA_VENT",
                "Kan ikke sette en oppgave på vent uten en årsak.",
            )
        }

        sikreAktivOppgaveOgTildeltSaksbehandler(oppgave) {
            val nyStatus = if (paavent) Status.PAA_VENT else hentForrigeStatus(oppgaveId)

            oppgaveDao.oppdaterPaaVent(oppgaveId, merknad, aarsak, nyStatus)

            when (oppgave.type) {
                OppgaveType.FOERSTEGANGSBEHANDLING,
                OppgaveType.REVURDERING,
                OppgaveType.TILBAKEKREVING,
                OppgaveType.KLAGE,
                -> {
                    // Oppgaver for tilbakekreving har referanse som er sakId og ikke behandlingId (uuid) inntil
                    // kravgrunnlag mottas og tilbakekrevingsbehandlingen opprettes. Disse er derfor ikke relevante her
                    // før dette inntreffer og vi kan få en gyldig uuid fra behandlingen.
                    val behandlingId = safeUUIDFromString(oppgave.referanse)
                    if (behandlingId != null) {
                        if (nyStatus == Status.PAA_VENT) {
                            hendelser.sendMeldingForHendelsePaaVent(
                                UUID.fromString(oppgave.referanse),
                                BehandlingHendelseType.PAA_VENT,
                                aarsak!!,
                            )
                        } else {
                            hendelser.sendMeldingForHendelseAvVent(
                                UUID.fromString(oppgave.referanse),
                                BehandlingHendelseType.AV_VENT,
                            )
                        }
                    }
                }

                else -> {} // Ingen statistikk for resten
            }
        }

        return hentOppgave(oppgaveId)
    }

    private fun sikreAktivOppgaveOgTildeltSaksbehandler(
        oppgave: OppgaveIntern,
        operasjon: () -> Unit,
    ) {
        sikreAtOppgaveIkkeErAvsluttet(oppgave)
        if (oppgave.saksbehandler?.ident.isNullOrEmpty() && oppgave.type != OppgaveType.TILBAKEKREVING) {
            throw OppgaveIkkeTildeltSaksbehandler(oppgave.id)
        } else {
            operasjon()
        }
    }

    private fun sikreAtOppgaveIkkeErAvsluttet(oppgave: OppgaveIntern) {
        if (oppgave.erAvsluttet()) {
            throw OppgaveKanIkkeEndres(oppgave.id, oppgave.status)
        }
    }

    fun tilAttestering(
        referanse: String,
        type: OppgaveType,
        merknad: String?,
    ): OppgaveIntern {
        val oppgaver =
            hentOppgaverForReferanse(referanse)
                .filter { it.type == type }
                .filter { it.status in listOf(Status.UNDER_BEHANDLING, Status.UNDERKJENT) }

        if (oppgaver.isEmpty()) {
            throw ManglerOppgaveUnderBehandling("Ingen oppgave funnet for referanse: $referanse")
        } else if (oppgaver.size > 1) {
            throw ForMangeOppgaverUnderBehandling("For mange oppgaver under behandling (ref: $referanse)")
        }

        val oppgave = oppgaver.single()

        val oppdatertMerknad = merknad ?: oppgave.merknad ?: ""
        oppgaveDao.oppdaterStatusOgMerknad(oppgave.id, oppdatertMerknad, Status.ATTESTERING)

        val toDagerFremITid = Tidspunkt.now().plus(2L, ChronoUnit.DAYS)
        oppgaveDao.redigerFrist(oppgave.id, toDagerFremITid)

        settSaksbehandlerSomForrigeSaksbehandlerOgFjern(oppgave.id)

        return hentOppgave(oppgave.id)
    }

    private fun settSaksbehandlerSomForrigeSaksbehandlerOgFjern(oppgaveId: UUID) {
        oppgaveDao.settForrigeSaksbehandlerFraSaksbehandler(oppgaveId)
        oppgaveDao.fjernSaksbehandler(oppgaveId)
    }

    fun tilUnderkjent(
        referanse: String,
        type: OppgaveType,
        merknad: String?,
    ): OppgaveIntern {
        val oppgave =
            hentOppgaverForReferanse(referanse)
                .filter { it.type == type }
                .singleOrNull { it.erAttestering() }
                ?: throw InternfeilException("Fant ikke oppgave med referanse: $referanse")

        val oppdatertMerknad = merknad ?: oppgave.merknad ?: ""
        val oppgaveId = oppgave.id

        val saksbehandlerIdent = saksbehandlerSomFattetVedtak(oppgave)
        if (saksbehandlerIdent != null) {
            if (saksbehandlerIdent != Fagsaksystem.EY.navn) {
                oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandlerIdent)
                oppgaveDao.fjernForrigeSaksbehandler(oppgaveId)
            }
        } else {
            logger.error("Fant ikke siste saksbehandler for oppgave med referanse: $referanse")
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        }
        oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, oppdatertMerknad, Status.UNDERKJENT)

        return oppgaveDao.hentOppgave(oppgaveId)!!
    }

    @Deprecated("Se på forrigesaksbehandler feltet")
    fun saksbehandlerSomFattetVedtak(oppgave: OppgaveIntern): String? =
        oppgave.forrigeSaksbehandlerIdent ?: oppgaveDao
            .hentEndringerForOppgave(oppgave.id)
            .sortedByDescending { it.tidspunkt }
            .firstOrNull(OppgaveEndring::sendtTilAttestering)
            ?.oppgaveFoer
            ?.saksbehandler
            ?.ident

    // TODO: Slå sammen med de 3 andre "ferdigstill"-funksjonene
    fun ferdigstillOppgaveUnderBehandling(
        referanse: String,
        type: OppgaveType,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForReferanse(referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw IkkeFunnetException("INGEN_OPPGAVE_MED_REFERANSE", "Fant ingen oppgaver med referanse=$referanse.")
        }
        try {
            val oppgaveUnderbehandling =
                behandlingsoppgaver
                    .filter { it.type == type }
                    .single { !it.erAvsluttet() }

            ferdigstillOppgave(oppgaveUnderbehandling, saksbehandler, merknad)

            return krevIkkeNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat ferdigstilte kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw UgyldigForespoerselException(
                "INGEN_UAVSLUTTET_OPPGAVE",
                "Fant ikke en åpen oppgave med type $type og referanse=$referanse",
            )
        } catch (e: IllegalArgumentException) {
            throw InternfeilException(
                "Fant mer enn en oppgave under behandling med type=$type og referanse=$referanse," +
                    " kan ikke avslutte oppgaven riktig.",
                cause = e,
            )
        }
    }

    fun ferdigstillOppgave(
        id: UUID,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ): OppgaveIntern {
        val oppgave =
            krevIkkeNull(oppgaveDao.hentOppgave(id)) {
                "Oppgave med id=$id finnes ikke – avbryter ferdigstilling av oppgaven"
            }
        ferdigstillOppgave(oppgave, saksbehandler, merknad)
        return hentOppgave(id)
    }

    fun sjekkOmKanFerdigstilleOppgave(
        oppgave: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
    ) {
        sikreAtOppgaveIkkeErAvsluttet(oppgave = oppgave)
        sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgave, saksbehandler)
    }

    private fun ferdigstillOppgave(
        oppgave: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ) {
        logger.info("Ferdigstiller oppgave=${oppgave.id}")

        sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgave, saksbehandler)

        when (merknad) {
            null -> oppgaveDao.endreStatusPaaOppgave(oppgave.id, Status.FERDIGSTILT)
            else -> oppgaveDao.oppdaterStatusOgMerknad(oppgave.id, merknad, Status.FERDIGSTILT)
        }
        logger.info("Oppgave med id=${oppgave.id} ferdigstilt av ${saksbehandler.ident()}")

        if (oppgave.typeKanAttesteres()) {
            tildelOpprinneligSaksbehandler(oppgave)
        }
    }

    fun hentEndringerOppgave(id: UUID): List<GenerellEndringshendelse> {
        val oppgave = hentOppgave(id)

        val behandlingHendelser =
            if (oppgave.kilde == OppgaveKilde.BEHANDLING) {
                hendelseDao
                    .finnHendelserIBehandling(UUID.fromString(oppgave.referanse))
                    .map {
                        val hendelse = EndringMapper.mapBehandlingHendelse(it)
                        GenerellEndringshendelse(
                            tidspunkt = it.opprettet,
                            saksbehandler = it.ident,
                            endringer =
                                listOf(
                                    EndringLinje(
                                        hendelse,
                                        it.kommentar?.let { kommentar -> "Kommentar: $kommentar" },
                                    ),
                                ),
                        )
                    }
            } else {
                emptyList()
            }

        val oppgaveHendelser = EndringMapper.mapOppgaveEndringer(oppgaveDao.hentEndringerForOppgave(id))

        return (oppgaveHendelser + behandlingHendelser)
            .sortedByDescending { it.tidspunkt }
    }

    private fun tildelOpprinneligSaksbehandler(oppgave: OppgaveIntern) {
        val forrigeSaksbehandler = saksbehandlerSomFattetVedtak(oppgave)

        if (forrigeSaksbehandler.isNullOrBlank()) {
            logger.warn("Fant ikke saksbehandleren som sendte oppgaveid ${oppgave.id} til attestering")
        } else {
            logger.info("Tildeler oppgave ${oppgave.id} til $forrigeSaksbehandler som sendte oppgaven til attestering")
            oppgaveDao.settNySaksbehandler(oppgave.id, forrigeSaksbehandler)
            oppgaveDao.fjernForrigeSaksbehandler(oppgave.id)
        }
    }

    private fun sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(
        oppgaveUnderBehandling: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
    ) {
        if (oppgaveUnderBehandling.status == Status.NY && oppgaveUnderBehandling.saksbehandler == null) {
            logger.info("Oppgave (id=${oppgaveUnderBehandling.id}) er ikke tildelt - tildeles ${saksbehandler.ident()}")
            tildelSaksbehandler(oppgaveUnderBehandling.id, saksbehandler.ident())
        } else if (!saksbehandler.kanEndreOppgaverFor(oppgaveUnderBehandling.saksbehandler?.ident)) {
            throw OppgaveTilhoererAnnenSaksbehandler(oppgaveUnderBehandling.id)
        }
    }

    fun oppdaterIdentForOppgaver(sak: Sak) {
        logger.info("Oppdaterer ident på oppgaver som ikke er avsluttet på sak ${sak.id}")

        oppgaveDao
            .hentOppgaverForSakMedType(sak.id, OppgaveType.entries)
            .filterNot(OppgaveIntern::erAvsluttet)
            .forEach {
                oppgaveDao.oppdaterIdent(it.id, sak.ident)
            }
    }

    fun oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet: List<SakMedEnhet>) {
        sakerMedNyEnhet.forEach {
            fjernSaksbehandlerFraOppgaveVedFlytt(it.id)
            endreEnhetForOppgaverTilknyttetSak(it.id, it.enhet)
        }
    }

    private fun fjernSaksbehandlerFraOppgaveVedFlytt(sakId: SakId) {
        for (oppgaveIntern in hentOppgaverForSak(sakId)) {
            if (oppgaveIntern.saksbehandler != null &&
                oppgaveIntern.erIkkeAvsluttet()
            ) {
                fjernSaksbehandler(oppgaveIntern.id)
            }
        }
    }

    private fun endreEnhetForOppgaverTilknyttetSak(
        sakId: SakId,
        enhetsID: Enhetsnummer,
    ) {
        val oppgaverForSak = oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries)
        oppgaverForSak.forEach {
            if (it.erIkkeAvsluttet()) {
                if (it.status == Status.UNDER_BEHANDLING) {
                    // Kun oppgaver som er UNDER_BEHANDLING kan gå til statusen NY
                    oppgaveDao.endreStatusPaaOppgave(it.id, Status.NY)
                }

                // For oppgaver som ikke er ferdige er det relevant for saksbehandlingsstatistikken
                // å få en oppdatert rad med ny enhet
                if (it.type.senderStatistikk()) {
                    hendelser.sendMeldingForEndretEnhet(it.referanse, enhetsID)
                }
            }
            oppgaveDao.endreEnhetPaaOppgave(it.id, enhetsID)
        }
    }

    fun hentOppgaverForSak(sakId: SakId): List<OppgaveIntern> = oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries)

    fun hentOppgaverForSakAvType(
        sakId: SakId,
        typer: List<OppgaveType>,
    ): List<OppgaveIntern> = oppgaveDao.hentOppgaverForSakMedType(sakId, typer)

    fun hentOppgaverForSak(
        sakId: SakId,
        type: OppgaveType,
    ): List<OppgaveIntern> = oppgaveDao.hentOppgaverForSakMedType(sakId, listOf(type))

    fun oppgaveMedTypeFinnes(
        sakId: SakId,
        type: OppgaveType,
    ): Boolean = oppgaveDao.oppgaveMedTypeFinnes(sakId, type)

    fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> = oppgaveDao.hentOppgaverForReferanse(referanse)

    fun hentOppgaverForGruppeId(
        gruppeId: String,
        type: OppgaveType,
        saksbehandler: Saksbehandler,
    ): List<OppgaveIntern> {
        val oppgaver = oppgaveDao.hentOppgaverForGruppeId(gruppeId, type)
        val enheterForSaksbehandler =
            saksbehandlerService
                .hentEnheterForSaksbehandlerIdentWrapper(saksbehandler.ident)
                .map { it.enhetsNummer }
        return oppgaver.filter { enheterForSaksbehandler.contains(it.enhet) }
    }

    fun hentForrigeStatus(oppgaveId: UUID): Status {
        val oppgave = hentOppgave(oppgaveId)

        return oppgaveDao
            .hentEndringerForOppgave(oppgaveId)
            .sortedByDescending { it.tidspunkt }
            .first { it.oppgaveFoer.status != oppgave.status }
            .oppgaveFoer.status
    }

    fun avbrytAktivitetspliktoppgave(
        oppgaveId: UUID,
        merknad: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        val oppgave = oppgaveDao.hentOppgave(oppgaveId)
        krevIkkeNull(oppgave) { "Fant ingen oppgave under behandling med id=$oppgaveId" }

        if (oppgave.status in listOf(Status.FERDIGSTILT, Status.AVBRUTT)) {
            throw InternfeilException("Kan ikke avbryte oppgave med status ${oppgave.status}")
        }

        val oppgaverSomKanAvbrytes = listOf(OppgaveType.AKTIVITETSPLIKT, OppgaveType.AKTIVITETSPLIKT_12MND)
        if (oppgave.type !in oppgaverSomKanAvbrytes) {
            throw InternfeilException("Kan ikke avbryte oppgaveType ${oppgave.type}")
        }

        sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgave, saksbehandler)
        oppgaveDao.oppdaterStatusOgMerknad(oppgave.id, merknad, Status.AVBRUTT)

        return krevIkkeNull(oppgaveDao.hentOppgave(oppgaveId)) {
            "Oppgaven kunne ikke hentes ut"
        }
    }

    fun avbrytOppgaveUnderBehandling(
        referanse: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        try {
            // TODO: Må undersøke konsekvensen av å legge til Status.NY på [hentOppgaveUnderBehandling]
            val oppgaveUnderbehandling =
                oppgaveDao
                    .hentOppgaverForReferanse(referanse)
                    .singleOrNull { !it.erAvsluttet() }

            krevIkkeNull(oppgaveUnderbehandling) {
                "Fant ingen oppgave under behandling med referanse=$referanse"
            }

            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.AVBRUTT)

            return krevIkkeNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat avbrøt kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw ManglerOppgaveUnderBehandling(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $referanse}",
            )
        } catch (e: IllegalArgumentException) {
            throw ForMangeOppgaverUnderBehandling(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $referanse",
            )
        }
    }

    /**
     * Henter behandlingsoppgaven til en attesterbar behandling.
     *
     * Hvis det fins andre oppgaver med samme refereanse, men med forskjellig type vil denne hente selve
     * behandlingsoppgaven.
     */
    fun hentOppgaveForAttesterbarBehandling(referanse: String): OppgaveIntern? =
        oppgaveDao
            .hentOppgaverForReferanse(referanse)
            .singleOrNull(OppgaveIntern::typeKanAttesteres)
            .also {
                if (it == null) {
                    logger.warn("Ingen behandlingsoppgave for referanse: $referanse")
                }
            }

    fun opprettOppgaveBulk(
        referanse: String,
        sakIds: List<SakId>,
        kilde: OppgaveKilde?,
        type: OppgaveType,
        merknad: String?,
        frist: Tidspunkt? = null,
        saksbehandler: Saksbehandler,
    ) {
        val saker: List<Sak> = sakDao.hentSaker("", 1000, sakIds, emptyList())
        val enheterForSaksbehandler =
            saksbehandlerService
                .hentEnheterForSaksbehandlerIdentWrapper(saksbehandler.ident)
                .map { it.enhetsNummer }
        if (saker.any { it.enhet !in enheterForSaksbehandler }) {
            throw IkkeTillattException(
                code = "HAR_IKKE_SKRIVETILGANG_TIL_SAK",
                detail =
                    "Har ikke skrivetilgang til en eller flere saker, kan ikke opprette oppgaver " +
                        "på saker man ikke har skrivetilgang til.",
            )
        }

        if (!saker.map { it.id }.containsAll(sakIds)) {
            val finnesIkke = sakIds.filterNot { it in saker.map { sak -> sak.id } }
            throw IkkeFunnetException("GO-01-SAK-IKKE-FUNNET", "Følgende saks-ID-er ble ikke funnet: $finnesIkke")
        }

        val oppgaveListe: List<OppgaveIntern> =
            saker.map { sak ->
                opprettNyOppgaveMedReferanseOgSak(
                    referanse = referanse,
                    sak = sak,
                    kilde = kilde,
                    type = type,
                    merknad = merknad,
                    frist = frist,
                    saksbehandler = null,
                )
            }

        oppgaveDao.opprettOppgaveBulk(oppgaveListe)
    }

    fun opprettOppgave(
        referanse: String,
        sakId: SakId,
        kilde: OppgaveKilde?,
        type: OppgaveType,
        merknad: String?,
        frist: Tidspunkt? = null,
        saksbehandler: String? = null,
        gruppeId: String? = null,
    ): OppgaveIntern {
        val sak = sakDao.hentSak(sakId)!!

        val oppgave =
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                kilde = kilde,
                type = type,
                merknad = merknad,
                frist = frist,
                gruppeId = gruppeId,
            )
        oppgaveDao.opprettOppgave(oppgave)

        if (saksbehandler != null) {
            tildelSaksbehandler(oppgave.id, saksbehandler)
        }

        return hentOppgave(oppgave.id)
    }

    fun hentOppgave(oppgaveId: UUID): OppgaveIntern =
        oppgaveDao.hentOppgave(oppgaveId)
            ?: throw InternfeilException("Oppgave med id=$oppgaveId ikke funnet!")

    /**
     * Skal kun brukes til:
     *  - automatisk avbrudd når vi får erstattende førstegangsbehandling i saken
     *  - journalposter som avbrytes/annuleres
     *  - automatisk avbrudd når kravgrunnlag i tilbakekreving er nullet ut
     *  - automatisk avbrudd når kravgrunnlag i tilbakekreving er endret
     */
    fun avbrytAapneOppgaverMedReferanse(
        referanse: String,
        merknad: String? = null,
    ) {
        logger.info("Avbryter åpne oppgaver med referanse=$referanse")

        oppgaveDao
            .hentOppgaverForReferanse(referanse)
            .filterNot(OppgaveIntern::erAvsluttet)
            .forEach {
                if (merknad != null) {
                    oppgaveDao.oppdaterStatusOgMerknad(it.id, merknad, Status.AVBRUTT)
                } else {
                    oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
                }
            }
    }

    fun hentFristGaarUt(request: VentefristGaarUtRequest): List<VentefristGaarUt> =
        oppgaveDao.hentFristGaarUt(request.dato, request.type, request.oppgaveKilde, request.oppgaver, request.grense)

    fun tilbakestillOppgaverUnderAttestering(saker: List<SakId>) {
        val oppgaverTilAttestering =
            oppgaveDao
                .hentOppgaverTilSaker(
                    saker,
                    listOf(Status.ATTESTERING.name),
                ).filter { it.type.skalTilbakestillesUnderAttestering() }
        oppgaverTilAttestering.forEach { oppgave ->
            oppgaveDao.tilbakestillOppgaveUnderAttestering(oppgave)
            saksbehandlerSomFattetVedtak(oppgave)?.let { saksbehandlerIdent ->
                oppgaveDao.settNySaksbehandler(oppgave.id, saksbehandlerIdent)
            }
        }

        val oppgaverPaaVent =
            oppgaveDao
                .hentOppgaverTilSaker(saker, listOf(Status.PAA_VENT.name))
                .filter { it.type.skalTilbakestillesUnderAttestering() }
        oppgaverPaaVent.forEach { oppgave ->
            val forrigeStatus = hentForrigeStatus(oppgave.id)
            // Vi må ta ekstra steg for å passe på at oppgaven blir satt til under behandling etter den tas av vent
            // Siden vi ser på forrige status når vi tar en oppgave av vent, kan vi oppnå dette ved å tilbakestille
            // oppgaven som over, og så sette den på vent igjen
            if (forrigeStatus == Status.ATTESTERING) {
                oppgaveDao.tilbakestillOppgaveUnderAttestering(oppgave)
                oppgaveDao.settNySaksbehandler(oppgave.id, Fagsaksystem.EY.navn)
                oppgaveDao.oppdaterPaaVent(
                    oppgaveId = oppgave.id,
                    merknad = oppgave.merknad ?: "",
                    aarsak = null,
                    oppgaveStatus = Status.PAA_VENT,
                )
                val originalSaksbehandler = oppgave.saksbehandler
                if (originalSaksbehandler != null) {
                    oppgaveDao.settNySaksbehandler(oppgave.id, originalSaksbehandler.ident)
                } else {
                    oppgaveDao.fjernSaksbehandler(oppgave.id)
                }
            }
        }
    }

    private fun safeUUIDFromString(value: String): UUID? =
        try {
            UUID.fromString(value)
        } catch (e: Exception) {
            null
        }
}

private fun OppgaveType.skalTilbakestillesUnderAttestering(): Boolean =
    when (this) {
        OppgaveType.FOERSTEGANGSBEHANDLING,
        OppgaveType.REVURDERING,
        OppgaveType.TILBAKEKREVING,
        -> true

        else -> false
    }

class BrukerManglerAttestantRolleException(
    ident: String,
) : UgyldigForespoerselException(
        code = "BRUKER_ER_IKKE_ATTESTANT",
        detail = "Bruker $ident mangler attestant rolle for tildeling",
    )

class ManglerOppgaveUnderBehandling(
    msg: String,
) : UgyldigForespoerselException(
        code = "MANGLER_OPPGAVE_UNDER_BEHANDLING",
        detail = msg,
    )

class ForMangeOppgaverUnderBehandling(
    msg: String,
) : UgyldigForespoerselException(
        code = "FOR_MANGE_OPPGAVER_UNDER_BEHANDLING",
        detail = msg,
    )

class OppgaveTilhoererAnnenSaksbehandler(
    oppgaveId: UUID,
) : UgyldigForespoerselException(
        code = "OPPGAVE_TILHOERER_ANNEN_SAKSBEHANDLER",
        detail = "Kan ikke lukke oppgave som tilhører en annen saksbehandler",
        meta = mapOf("oppgaveId" to oppgaveId),
    )

class OppgaveIkkeTildeltSaksbehandler(
    oppgaveId: UUID,
) : UgyldigForespoerselException(
        code = "OPPGAVE_IKKE_TILDELT_SAKSBEHANDLER",
        detail = "Oppgaven er ikke tildelt en saksbehandler",
        meta = mapOf("oppgaveId" to oppgaveId),
    )

class OppgaveKanIkkeEndres(
    oppgaveId: UUID,
    status: Status,
) : UgyldigForespoerselException(
        code = "OPPGAVE_KAN_IKKE_ENDRES",
        detail = "Oppgaven kan ikke endres siden den har status $status",
        meta = mapOf("oppgaveId" to oppgaveId),
    )

class FristTilbakeITid(
    oppgaveId: UUID,
) : UgyldigForespoerselException(
        code = "FRIST_TILBAKE_I_TID",
        detail = "Frist kan ikke settes tilbake i tid",
        meta = mapOf("oppgaveId" to oppgaveId),
    )

class OppgaveIkkeFunnet(
    oppgaveId: UUID,
) : IkkeFunnetException(
        code = "OPPGAVE_IKKE_FUNNET",
        detail = "Oppgaven finnes ikke",
        meta = mapOf("oppgaveId" to oppgaveId),
    )
