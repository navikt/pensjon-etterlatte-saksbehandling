package no.nav.etterlatte.oppgave

import io.ktor.server.plugins.BadRequestException
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
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.UUID

class OppgaveService(
    private val oppgaveDao: OppgaveDaoMedEndringssporing,
    private val sakDao: SakLesDao,
    private val hendelseDao: HendelseDao,
    private val hendelser: BehandlingHendelserKafkaProducer,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun finnOppgaverForBruker(
        bruker: SaksbehandlerMedEnheterOgRoller,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String? = null,
    ): List<OppgaveIntern> =
        if (bruker.saksbehandlerMedRoller.harRolleStrengtFortrolig()) {
            oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland()
        } else {
            oppgaveDao
                .hentOppgaver(
                    bruker.enheter(),
                    oppgaveStatuser,
                    minOppgavelisteIdentFilter,
                ).sortedByDescending { it.opprettet }
        }

    fun genererStatsForOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats =
        oppgaveDao.hentAntallOppgaver(innloggetSaksbehandlerIdent)

    private fun sjekkOmkanTildeleAttestantOppgave(): Boolean {
        val appUser = Kontekst.get().AppUser
        return when (appUser) {
            is SystemUser -> true
            is Self -> true
            is SaksbehandlerMedEnheterOgRoller -> {
                val saksbehandlerMedRoller = appUser.saksbehandlerMedRoller
                if (saksbehandlerMedRoller.harRolleAttestant()) {
                    return true
                } else {
                    throw BrukerManglerAttestantRolleException(saksbehandlerMedRoller.saksbehandler.ident)
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
        hentetOppgave.erAttestering() && sjekkOmkanTildeleAttestantOppgave()

        oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)

        // Må ha denne for å ikke overskride attesteringstatus på oppgaver
        if (hentetOppgave.status == Status.NY) {
            oppgaveDao.endreStatusPaaOppgave(oppgaveId, Status.UNDER_BEHANDLING)
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

    fun endreTilKildeBehandlingOgOppdaterReferanse(
        oppgaveId: UUID,
        referanse: String,
    ) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAktivOppgaveOgTildeltSaksbehandler(hentetOppgave) {
            oppgaveDao.endreTilKildeBehandlingOgOppdaterReferanse(oppgaveId, referanse)
        }
    }

    fun oppdaterReferanseOgMerknad(
        oppgaveId: UUID,
        referanse: String,
        merknad: String,
    ) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)

        // Krever ikke at saksbehandler er tildelt for dette
        oppgaveDao.oppdaterReferanseOgMerknad(oppgaveId, referanse, merknad)
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
                ?: throw IllegalStateException("Fant ikke oppgave med referanse: $referanse")

        val oppdatertMerknad = merknad ?: oppgave.merknad ?: ""
        val oppgaveId = oppgave.id

        val saksbehandlerIdent = saksbehandlerSomFattetVedtak(oppgave)
        if (saksbehandlerIdent != null) {
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandlerIdent)
            oppgaveDao.fjernForrigeSaksbehandler(oppgaveId)
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
    fun ferdigStillOppgaveUnderBehandling(
        referanse: String,
        type: OppgaveType,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForReferanse(referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å ferdigstille oppgave")
        }
        try {
            val oppgaveUnderbehandling =
                behandlingsoppgaver
                    .filter { it.type == type }
                    .single { !it.erAvsluttet() }

            ferdigstillOppgave(oppgaveUnderbehandling, saksbehandler, merknad)

            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat ferdigstilte kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID: $referanse",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID: $referanse",
                e,
            )
        }
    }

    fun ferdigstillOppgave(
        id: UUID,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ) {
        val oppgave =
            checkNotNull(oppgaveDao.hentOppgave(id)) {
                "Oppgave med id=$id finnes ikke – avbryter ferdigstilling av oppgaven"
            }
        ferdigstillOppgave(oppgave, saksbehandler, merknad)
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
            logger.warn("Fant ikke saksbehandleren som sendte oppgave $oppgave til attestering")
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

    fun oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet: List<SakMedEnhet>) {
        sakerMedNyEnhet.forEach {
            fjernSaksbehandlerFraOppgaveVedFlytt(it.id)
            endreEnhetForOppgaverTilknyttetSak(it.id, it.enhet)
        }
    }

    private fun fjernSaksbehandlerFraOppgaveVedFlytt(sakId: SakId) {
        for (oppgaveIntern in hentOppgaverForSak(sakId)) {
            if (oppgaveIntern.saksbehandler != null &&
                oppgaveIntern.erUnderBehandling()
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
            if (it.erUnderBehandling()) {
                oppgaveDao.endreStatusPaaOppgave(it.id, Status.NY)
            }
            oppgaveDao.endreEnhetPaaOppgave(it.id, enhetsID)
        }
    }

    fun hentOppgaverForSak(sakId: SakId): List<OppgaveIntern> = oppgaveDao.hentOppgaverForSakMedType(sakId, OppgaveType.entries)

    fun hentOppgaverForSak(
        sakId: SakId,
        type: OppgaveType,
    ): List<OppgaveIntern> = oppgaveDao.hentOppgaverForSakMedType(sakId, listOf(type))

    fun oppgaveMedTypeFinnes(
        sakId: SakId,
        type: OppgaveType,
    ): Boolean = oppgaveDao.oppgaveMedTypeFinnes(sakId, type)

    fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> = oppgaveDao.hentOppgaverForReferanse(referanse)

    fun hentForrigeStatus(oppgaveId: UUID): Status {
        val oppgave = hentOppgave(oppgaveId)

        return oppgaveDao
            .hentEndringerForOppgave(oppgaveId)
            .sortedByDescending { it.tidspunkt }
            .first { it.oppgaveFoer.status != oppgave.status }
            .oppgaveFoer.status
    }

    fun avbrytOppgaveUnderBehandling(
        referanse: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        try {
            val oppgaveUnderbehandling =
                checkNotNull(hentOppgaveUnderBehandling(referanse)) {
                    "Fant ingen oppgave under behandling med referanse=$referanse"
                }

            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.AVBRUTT)

            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
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

    fun hentOppgaveUnderBehandling(referanse: String) =
        oppgaveDao
            .hentOppgaverForReferanse(referanse)
            .singleOrNull(OppgaveIntern::erUnderBehandling)
            .also {
                if (it == null) {
                    logger.warn("Ingen oppgave under behandling for referanse: $referanse")
                }
            }

    fun opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
        referanse: String,
        sakId: SakId,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        merknad: String? = null,
    ): OppgaveIntern {
        val oppgaverForBehandling = oppgaveDao.hentOppgaverForReferanse(referanse)
        val oppgaverSomKanLukkes = oppgaverForBehandling.filter { !it.erAvsluttet() }
        oppgaverSomKanLukkes.forEach {
            oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
        }

        return opprettOppgave(
            referanse = referanse,
            sakId = sakId,
            kilde = oppgaveKilde,
            type = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = merknad,
        )
    }

    fun opprettOppgaveBulk(
        referanse: String,
        sakIds: List<SakId>,
        kilde: OppgaveKilde?,
        type: OppgaveType,
        merknad: String?,
        frist: Tidspunkt? = null,
        saksbehandler: String? = null,
    ) {
        val saker: List<Sak> = sakDao.hentSaker("", 1000, sakIds, emptyList())

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
                    saksbehandler = saksbehandler,
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
                ).filter {
                    when (it.type) {
                        OppgaveType.FOERSTEGANGSBEHANDLING,
                        OppgaveType.REVURDERING,
                        OppgaveType.VURDER_KONSEKVENS,
                        OppgaveType.GOSYS,
                        OppgaveType.TILBAKEKREVING,
                        OppgaveType.OMGJOERING,
                        OppgaveType.JOURNALFOERING,
                        OppgaveType.TILLEGGSINFORMASJON,
                        OppgaveType.GJENOPPRETTING_ALDERSOVERGANG, // Saker som ble opphørt i Pesys etter 18 år gammel regelverk
                        OppgaveType.AKTIVITETSPLIKT,
                        OppgaveType.AKTIVITETSPLIKT_REVURDERING,
                        OppgaveType.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK,
                        ->
                            true

                        OppgaveType.KLAGE,
                        OppgaveType.KRAVPAKKE_UTLAND,
                        OppgaveType.MANGLER_SOEKNAD,
                        OppgaveType.GENERELL_OPPGAVE,
                        OppgaveType.MANUELL_UTSENDING_BREV,
                        -> {
                            logger.info(
                                "Tilbakestiller ikke oppgave av type ${it.type} " +
                                    "fra attestering for oppgave ${it.id}",
                            )
                            false
                        }
                    }
                }
        oppgaverTilAttestering.forEach { oppgave ->
            oppgaveDao.tilbakestillOppgaveUnderAttestering(oppgave)
            saksbehandlerSomFattetVedtak(oppgave)?.let { saksbehandlerIdent ->
                oppgaveDao.settNySaksbehandler(oppgave.id, saksbehandlerIdent)
            }
        }
    }
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
