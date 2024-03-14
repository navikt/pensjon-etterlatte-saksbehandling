package no.nav.etterlatte.oppgave

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.Self
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgavebenkStats
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUt
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OppgaveService(
    private val oppgaveDao: OppgaveDaoMedEndringssporing,
    private val sakDao: SakDao,
    private val hendelser: BehandlingHendelserKafkaProducer,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun finnOppgaverForBruker(
        bruker: SaksbehandlerMedEnheterOgRoller,
        oppgaveStatuser: List<String>,
        minOppgavelisteIdentFilter: String? = null,
    ): List<OppgaveIntern> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker.saksbehandlerMedRoller)
        val aktuelleOppgavetyperForRoller = aktuelleOppgavetyperForRolleTilSaksbehandler(rollerSomBrukerHar)

        return if (bruker.saksbehandlerMedRoller.harRolleStrengtFortrolig()) {
            oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(aktuelleOppgavetyperForRoller)
        } else {
            oppgaveDao.hentOppgaver(
                aktuelleOppgavetyperForRoller,
                bruker.enheter(),
                oppgaveStatuser,
                minOppgavelisteIdentFilter,
            ).sortedByDescending { it.opprettet }
        }
    }

    fun genererStatsForOppgaver(innloggetSaksbehandlerIdent: String): OppgavebenkStats {
        return oppgaveDao.hentAntallOppgaver(innloggetSaksbehandlerIdent)
    }

    private fun aktuelleOppgavetyperForRolleTilSaksbehandler(roller: List<Rolle>) =
        roller.flatMap {
            when (it) {
                Rolle.SAKSBEHANDLER -> OppgaveType.entries - OppgaveType.ATTESTERING
                Rolle.ATTESTANT -> listOf(OppgaveType.ATTESTERING)
                Rolle.STRENGT_FORTROLIG -> OppgaveType.entries
            }.distinct()
        }

    private fun finnAktuelleRoller(bruker: SaksbehandlerMedRoller): List<Rolle> =
        listOfNotNull(
            Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
            Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() },
            Rolle.STRENGT_FORTROLIG.takeIf { bruker.harRolleStrengtFortrolig() },
        )

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
        val eksisterendeSaksbehandler = hentetOppgave.saksbehandler?.ident
        if (eksisterendeSaksbehandler.isNullOrEmpty() || eksisterendeSaksbehandler == Fagsaksystem.EY.navn) {
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
        } else {
            throw OppgaveAlleredeTildeltSaksbehandler(oppgaveId, eksisterendeSaksbehandler)
        }
    }

    fun byttSaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId)
                ?: throw OppgaveIkkeFunnet(oppgaveId)

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
    }

    fun fjernSaksbehandler(oppgaveId: UUID) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)
        sikreAktivOppgaveOgTildeltSaksbehandler(hentetOppgave) {
            oppgaveDao.fjernSaksbehandler(oppgaveId)
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
        sikreAktivOppgaveOgTildeltSaksbehandler(hentetOppgave) {
            oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, merknad, status)
        }
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

    fun endrePaaVent(
        oppgaveId: UUID,
        merknad: String,
        paaVent: Boolean,
    ) {
        val oppgave = hentOppgave(oppgaveId) ?: throw OppgaveIkkeFunnet(oppgaveId)
        if (paaVent && oppgave.status == Status.PAA_VENT) return
        if (!paaVent && oppgave.status != Status.PAA_VENT) return

        sikreAktivOppgaveOgTildeltSaksbehandler(oppgave) {
            val nyStatus = if (paaVent) Status.PAA_VENT else Status.UNDER_BEHANDLING
            oppgaveDao.oppdaterStatusOgMerknad(oppgaveId, merknad, nyStatus)
            when (oppgave.type) {
                OppgaveType.FOERSTEGANGSBEHANDLING,
                OppgaveType.REVURDERING,
                OppgaveType.ATTESTERING,
                OppgaveType.UNDERKJENT,
                OppgaveType.TILBAKEKREVING,
                OppgaveType.KLAGE,
                -> {
                    hendelser.sendMeldingForHendelsePaaVent(
                        UUID.fromString(oppgave.referanse),
                        if (nyStatus == Status.PAA_VENT) BehandlingHendelseType.PAA_VENT else BehandlingHendelseType.AV_VENT,
                    )
                }
                else -> {} // Ingen statistikk for resten
            }
        }
    }

    private fun sikreAktivOppgaveOgTildeltSaksbehandler(
        oppgave: OppgaveIntern,
        operasjon: () -> Unit,
    ) {
        sikreAtOppgaveIkkeErAvsluttet(oppgave)
        if (oppgave.saksbehandler?.ident.isNullOrEmpty()) {
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

    fun ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
        fattetoppgaveReferanseOgSak: SakIdOgReferanse,
        oppgaveType: OppgaveType,
        merknad: String?,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForReferanse(fattetoppgaveReferanseOgSak.referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å kunne lage attesteringsoppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            ferdigstillOppgaveById(oppgaveUnderbehandling, saksbehandler)
            return opprettNyOppgaveMedSakOgReferanse(
                referanse = fattetoppgaveReferanseOgSak.referanse,
                sakId = fattetoppgaveReferanseOgSak.sakId,
                oppgaveKilde = oppgaveUnderbehandling.kilde,
                oppgaveType = oppgaveType,
                merknad = merknad,
            )
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgaveReferanseOgSak.referanse}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgaveReferanseOgSak.referanse}",
                e,
            )
        }
    }

    fun ferdigStillOppgaveUnderBehandling(
        referanse: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForReferanse(referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å ferdigstille oppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            ferdigstillOppgaveById(oppgaveUnderbehandling, saksbehandler)
            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat ferdigstilte kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $referanse}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $referanse",
                e,
            )
        }
    }

    fun hentOgFerdigstillOppgaveById(
        id: UUID,
        saksbehandler: BrukerTokenInfo,
        merknad: String? = null,
    ) {
        val oppgave =
            checkNotNull(oppgaveDao.hentOppgave(id)) {
                "Oppgave med id=$id finnes ikke – avbryter ferdigstilling av oppgaven"
            }
        ferdigstillOppgaveById(oppgave, saksbehandler, merknad)
    }

    private fun ferdigstillOppgaveById(
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
    }

    private fun sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(
        oppgaveUnderBehandling: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
    ) {
        if (!saksbehandler.kanEndreOppgaverFor(oppgaveUnderBehandling.saksbehandler?.ident)) {
            throw OppgaveTilhoererAnnenSaksbehandler(oppgaveUnderBehandling.id)
        }
    }

    fun oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet: List<GrunnlagsendringshendelseService.SakMedEnhet>) {
        sakerMedNyEnhet.forEach {
            endreEnhetForOppgaverTilknyttetSak(it.id, it.enhet)
        }
    }

    private fun endreEnhetForOppgaverTilknyttetSak(
        sakId: Long,
        enhetsID: String,
    ) {
        val oppgaverForSak = oppgaveDao.hentOppgaverForSak(sakId)
        oppgaverForSak.forEach {
            oppgaveDao.endreEnhetPaaOppgave(it.id, enhetsID)
        }
    }

    fun hentOppgaverForSak(sakId: Long): List<OppgaveIntern> {
        return oppgaveDao.hentOppgaverForSak(sakId)
    }

    fun hentOppgaverForReferanse(referanse: String): List<OppgaveIntern> {
        return oppgaveDao.hentOppgaverForReferanse(referanse)
    }

    fun hentEnkeltOppgaveForReferanse(referanse: String): OppgaveIntern {
        val hentOppgaverForReferanse = hentOppgaverForReferanse(referanse)
        try {
            return hentOppgaverForReferanse.single()
        } catch (e: NoSuchElementException) {
            throw BadRequestException("Finner ingen oppgaver for referanse: $referanse")
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Det finnes mer enn en oppgave for referanse: $referanse")
        }
    }

    fun avbrytOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        try {
            val oppgaveUnderbehandling = hentOppgaveUnderBehandlingForReferanse(behandlingEllerHendelseId)
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.AVBRUTT)
            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat avbrøt kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw ManglerOppgaveUnderBehandling(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId}",
            )
        } catch (e: IllegalArgumentException) {
            throw ForMangeOppgaverUnderBehandling(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId",
            )
        }
    }

    fun hentOppgaveUnderBehandlingForReferanse(behandlingEllerHendelseId: String) =
        oppgaveDao.hentOppgaverForReferanse(behandlingEllerHendelseId)
            .single { it.status == Status.UNDER_BEHANDLING }

    fun opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
        referanse: String,
        sakId: Long,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        merknad: String? = null,
    ): OppgaveIntern {
        val oppgaverForBehandling = oppgaveDao.hentOppgaverForReferanse(referanse)
        val oppgaverSomKanLukkes = oppgaverForBehandling.filter { !it.erAvsluttet() }
        oppgaverSomKanLukkes.forEach {
            oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
        }

        return opprettNyOppgaveMedSakOgReferanse(
            referanse = referanse,
            sakId = sakId,
            oppgaveKilde = oppgaveKilde,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = merknad,
        )
    }

    fun opprettNyOppgaveMedSakOgReferanse(
        referanse: String,
        sakId: Long,
        oppgaveKilde: OppgaveKilde?,
        oppgaveType: OppgaveType,
        merknad: String?,
        frist: Tidspunkt? = null,
    ): OppgaveIntern {
        val sak = sakDao.hentSak(sakId)!!
        return opprettOppgave(
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                oppgaveKilde = oppgaveKilde,
                oppgaveType = oppgaveType,
                merknad = merknad,
                frist = frist,
            ),
        )
    }

    fun hentSisteSaksbehandlerIkkeAttestertOppgave(referanse: String): OppgaveSaksbehandler? {
        val oppgaverForBehandlingUtenAttesterting =
            oppgaveDao.hentOppgaverForReferanse(referanse)
                .filter {
                    it.type !== OppgaveType.ATTESTERING
                }
        val sortedByDescending = oppgaverForBehandlingUtenAttesterting.sortedByDescending { it.opprettet }
        if (sortedByDescending.isEmpty()) {
            throw ManglerSaksbehandlerException("Fant ingen saksbehandler for oppgave uten attesteringstype med referanse $referanse")
        } else {
            return sortedByDescending[0].saksbehandler
        }
    }

    fun hentSisteIkkeAttestertOppgave(referanse: String): OppgaveIntern {
        val oppgaverForBehandlingUtenAttesterting =
            oppgaveDao.hentOppgaverForReferanse(referanse)
                .filter {
                    it.type !== OppgaveType.ATTESTERING
                }
        val sortedByDescending = oppgaverForBehandlingUtenAttesterting.sortedByDescending { it.opprettet }
        if (sortedByDescending.isEmpty()) {
            throw ManglerSaksbehandlerException("Fant ingen oppgave uten attesteringstype med referanse $referanse")
        } else {
            return sortedByDescending[0]
        }
    }

    fun hentOppgaveForSaksbehandlerFraFoerstegangsbehandling(behandlingId: UUID): OppgaveIntern? {
        val oppgaverForBehandlingFoerstegangs =
            oppgaveDao.hentOppgaverForReferanse(behandlingId.toString()).filter {
                it.type == OppgaveType.FOERSTEGANGSBEHANDLING
            }
        return oppgaverForBehandlingFoerstegangs.maxByOrNull { it.opprettet }
    }

    fun hentSaksbehandlerForOppgaveUnderArbeidByReferanse(referanse: String): OppgaveSaksbehandler? {
        val oppgaverforBehandling = oppgaveDao.hentOppgaverForReferanse(referanse)
        return try {
            oppgaverforBehandling.single { it.status == Status.UNDER_BEHANDLING }.saksbehandler
        } catch (e: NoSuchElementException) {
            logger.info("Det må finnes en oppgave under behandling, gjelder referanse: $referanse")
            return null
        } catch (e: IllegalArgumentException) {
            logger.info("Skal kun ha en oppgave under behandling, gjelder referanse: $referanse")
            return null
        }
    }

    private fun opprettOppgave(oppgaveIntern: OppgaveIntern): OppgaveIntern {
        var oppgaveLagres = oppgaveIntern
        if (oppgaveIntern.frist === null) {
            val enMaanedFrem = oppgaveIntern.opprettet.toLocalDatetimeUTC().plusMonths(1L).toTidspunkt()
            oppgaveLagres = oppgaveIntern.copy(frist = enMaanedFrem)
        }
        oppgaveDao.opprettOppgave(oppgaveLagres)
        return oppgaveDao.hentOppgave(oppgaveLagres.id)!!
    }

    fun hentOppgave(oppgaveId: UUID): OppgaveIntern? {
        return oppgaveDao.hentOppgave(oppgaveId)
    }

    /**
     * Skal kun brukes for automatisk avbrudd når vi får erstattende førstegangsbehandling i saken
     */
    fun avbrytAapneOppgaverForBehandling(behandlingId: String) {
        oppgaveDao.hentOppgaverForReferanse(behandlingId)
            .filter { !it.erAvsluttet() }
            .forEach {
                oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
            }
    }

    fun avbrytAapneOppgaverMedReferanse(referanse: String) {
        logger.info("Avbryter åpne oppgaver med referanse=$referanse")

        oppgaveDao.hentOppgaverForReferanse(referanse)
            .filterNot(OppgaveIntern::erAvsluttet)
            .forEach { oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT) }
    }

    fun hentSakOgOppgaverForSak(sakId: Long): OppgaveListe {
        val sak = sakDao.hentSak(sakId)
        if (sak != null) {
            return OppgaveListe(sak, hentOppgaverForSak(sak.id))
        } else {
            throw FantIkkeSakException("Fant ikke sakid $sakId")
        }
    }

    fun hentFristGaarUt(request: VentefristGaarUtRequest): List<VentefristGaarUt> =
        oppgaveDao.hentFristGaarUt(request.dato, request.type, request.oppgaveKilde, request.oppgaver)
}

class BrukerManglerAttestantRolleException(ident: String) : UgyldigForespoerselException(
    code = "BRUKER_ER_IKKE_ATTESTANT",
    detail = "Bruker $ident mangler attestant rolle for tildeling",
)

class ManglerOppgaveUnderBehandling(msg: String) : UgyldigForespoerselException(
    code = "MANGLER_OPPGAVE_UNDER_BEHANDLING",
    detail = msg,
)

class ForMangeOppgaverUnderBehandling(msg: String) : UgyldigForespoerselException(
    code = "FOR_MANGE_OPPGAVER_UNDER_BEHANDLING",
    detail = msg,
)

class ManglerSaksbehandlerException(msg: String) : UgyldigForespoerselException(
    code = "MANGLER_SAKSBEHANDLER_PAA_OPPGAVE",
    detail = msg,
)

class FantIkkeSakException(msg: String) : Exception(msg)

class OppgaveTilhoererAnnenSaksbehandler(oppgaveId: UUID) : UgyldigForespoerselException(
    code = "OPPGAVE_TILHOERER_ANNEN_SAKSBEHANDLER",
    detail = "Kan ikke lukke oppgave som tilhører en annen saksbehandler",
    meta = mapOf("oppgaveId" to oppgaveId),
)

class OppgaveIkkeTildeltSaksbehandler(oppgaveId: UUID) : UgyldigForespoerselException(
    code = "OPPGAVE_IKKE_TILDELT_SAKSBEHANDLER",
    detail = "Oppgaven er ikke tildelt en saksbehandler",
    meta = mapOf("oppgaveId" to oppgaveId),
)

class OppgaveKanIkkeEndres(oppgaveId: UUID, status: Status) : UgyldigForespoerselException(
    code = "OPPGAVE_KAN_IKKE_ENDRES",
    detail = "Oppgaven kan ikke endres siden den har status $status",
    meta = mapOf("oppgaveId" to oppgaveId),
)

class FristTilbakeITid(oppgaveId: UUID) : UgyldigForespoerselException(
    code = "FRIST_TILBAKE_I_TID",
    detail = "Frist kan ikke settes tilbake i tid",
    meta = mapOf("oppgaveId" to oppgaveId),
)

class OppgaveAlleredeTildeltSaksbehandler(oppgaveId: UUID, saksbehandler: String?) : ForespoerselException(
    status = HttpStatusCode.Conflict.value,
    code = "OPPGAVE_ALLEREDE_TILDELT_SAKSBEHANDLER",
    detail = "Oppgaven er allerede tildelt saksbehandler $saksbehandler",
    meta = mapOf("oppgaveId" to oppgaveId),
)

class OppgaveIkkeFunnet(oppgaveId: UUID) : IkkeFunnetException(
    code = "OPPGAVE_IKKE_FUNNET",
    detail = "Oppgaven finnes ikke",
    meta = mapOf("oppgaveId" to oppgaveId),
)

enum class Rolle {
    SAKSBEHANDLER,
    ATTESTANT,
    STRENGT_FORTROLIG,
}
