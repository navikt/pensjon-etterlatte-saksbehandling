package no.nav.etterlatte.oppgave

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.Self
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class BrukerManglerAttestantRolleException(msg: String) : UgyldigForespoerselException(
    code = "BRUKER_ER_IKKE_ATTESTANT",
    detail = msg,
)

class ManglerOppgaveUnderBehandling(msg: String) : UgyldigForespoerselException(
    code = "MANGLER_OPPGAVE_UNDER_BEHANDLING",
    detail = msg,
)

class ForMangeOppgaverUnderBehandling(msg: String) : UgyldigForespoerselException(
    code = "FOR_MANGE_OPPGAVER_UNDER_BEHANDLING",
    detail = msg,
)

class OppgaveService(
    private val oppgaveDao: OppgaveDaoMedEndringssporing,
    private val sakDao: SakDao,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun finnOppgaverForBruker(bruker: SaksbehandlerMedEnheterOgRoller): List<OppgaveIntern> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker.saksbehandlerMedRoller)
        val aktuelleOppgavetyperForRoller = aktuelleOppgavetyperForRolleTilSaksbehandler(rollerSomBrukerHar)

        return if (bruker.saksbehandlerMedRoller.harRolleStrengtFortrolig()) {
            oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(aktuelleOppgavetyperForRoller)
        } else {
            val oppgaverForBruker =
                oppgaveDao.hentOppgaver(
                    aktuelleOppgavetyperForRoller,
                    bruker.enheter(),
                    bruker.erSuperbruker(),
                )
            val oppgaverMedSaksbehandlerNavn = populerOppgaverMedNavn(oppgaverForBruker)
            oppgaverMedSaksbehandlerNavn.sortedByDescending { it.opprettet }
        }
    }

    // TODO: test for denne
    fun populerOppgaverMedNavn(oppgaver: List<OppgaveIntern>): List<OppgaveIntern> {
        val oppgaverMedSaksbehandler = oppgaver.mapNotNull { it.saksbehandler }.distinct()
        val saksbehandlerereMedNavn =
            oppgaveDao.hentSaksbehandlerNavnForidenter(oppgaverMedSaksbehandler)
                .associate { it.ident to it.navn }
        oppgaver.forEach {
            if (it.harSaksbehandler()) {
                val navn = saksbehandlerereMedNavn[it.saksbehandler]
                if (navn != null) {
                    println("bytter navn for ${it.saksbehandler} til $navn")
                    it.saksbehandlerNavn = navn
                }
            }
        }
        return oppgaver
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
                    throw BrukerManglerAttestantRolleException(
                        "Bruker ${saksbehandlerMedRoller.saksbehandler.ident} " +
                            "mangler attestant rolle for tildeling",
                    )
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
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        hentetOppgave.erAttestering() && sjekkOmkanTildeleAttestantOppgave()
        if (hentetOppgave.saksbehandler.isNullOrEmpty()) {
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
        } else {
            throw OppgaveAlleredeTildeltException(oppgaveId)
        }
    }

    fun byttSaksbehandler(
        oppgaveId: UUID,
        saksbehandler: String,
    ) {
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveId)
        if (hentetOppgave != null) {
            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
        } else {
            throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")
        }
    }

    fun fjernSaksbehandler(oppgaveId: UUID) {
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId)
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")

        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        if (hentetOppgave.saksbehandler != null) {
            oppgaveDao.fjernSaksbehandler(oppgaveId)
        } else {
            throw BadRequestException(
                "Oppgaven har ingen saksbehandler, id: $oppgaveId",
            )
        }
    }

    private fun sikreAtOppgaveIkkeErAvsluttet(oppgave: OppgaveIntern) {
        if (oppgave.erAvsluttet()) {
            throw IllegalStateException(
                "Oppgave med id ${oppgave.id} kan ikke endres siden den har " +
                    "status ${oppgave.status}",
            )
        }
    }

    fun redigerFrist(
        oppgaveId: UUID,
        frist: Tidspunkt,
    ) {
        if (frist.isBefore(Tidspunkt.now())) {
            throw BadRequestException("Tidspunkt tilbake i tid id: $oppgaveId")
        }
        val hentetOppgave =
            oppgaveDao.hentOppgave(oppgaveId)
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")
        sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
        if (hentetOppgave.saksbehandler != null) {
            oppgaveDao.redigerFrist(oppgaveId, frist)
        } else {
            throw BadRequestException(
                "Oppgaven har ingen saksbehandler, id: $oppgaveId",
            )
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
    ) {
        val oppgave =
            checkNotNull(oppgaveDao.hentOppgave(id)) {
                "Oppgave med id=$id finnes ikke – avbryter ferdigstilling av oppgaven"
            }
        ferdigstillOppgaveById(oppgave, saksbehandler)
    }

    private fun ferdigstillOppgaveById(
        oppgave: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
    ) {
        logger.info("Ferdigstiller oppgave=${oppgave.id}")

        sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgave, saksbehandler)

        oppgaveDao.endreStatusPaaOppgave(oppgave.id, Status.FERDIGSTILT)
        logger.info("Oppgave med id=${oppgave.id} ferdigstilt av ${saksbehandler.ident()}")
    }

    private fun sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(
        oppgaveUnderBehandling: OppgaveIntern,
        saksbehandler: BrukerTokenInfo,
    ) {
        if (!saksbehandler.kanEndreOppgaverFor(oppgaveUnderBehandling.saksbehandler)) {
            throw FeilSaksbehandlerPaaOppgaveException(
                "Kan ikke lukke oppgave for en annen saksbehandler oppgave:" +
                    " ${oppgaveUnderBehandling.id}",
            )
        }
    }

    class FeilSaksbehandlerPaaOppgaveException(message: String) : Exception(message)

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
            val oppgaveUnderbehandling =
                oppgaveDao.hentOppgaverForReferanse(behandlingEllerHendelseId)
                    .single { it.status == Status.UNDER_BEHANDLING }
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

    fun opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
        referanse: String,
        sakId: Long,
    ): OppgaveIntern {
        val oppgaverForBehandling = oppgaveDao.hentOppgaverForReferanse(referanse)
        val oppgaverSomKanLukkes = oppgaverForBehandling.filter { !it.erAvsluttet() }
        oppgaverSomKanLukkes.forEach {
            oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
        }

        return opprettNyOppgaveMedSakOgReferanse(
            referanse = referanse,
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null,
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
        return opprettOppave(
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

    fun hentSisteSaksbehandlerIkkeAttestertOppgave(referanse: String): String? {
        val oppgaverForBehandlingUtenAttesterting =
            oppgaveDao.hentOppgaverForReferanse(referanse)
                .filter {
                    it.type !== OppgaveType.ATTESTERING
                }
        return oppgaverForBehandlingUtenAttesterting.sortedByDescending { it.opprettet }[0].saksbehandler
    }

    fun hentOppgaveForSaksbehandlerFraFoerstegangsbehandling(behandlingId: UUID): OppgaveIntern? {
        val oppgaverForBehandlingFoerstegangs =
            oppgaveDao.hentOppgaverForReferanse(behandlingId.toString()).filter {
                it.type == OppgaveType.FOERSTEGANGSBEHANDLING
            }
        return oppgaverForBehandlingFoerstegangs.maxByOrNull { it.opprettet }
    }

    fun hentSaksbehandlerForOppgaveUnderArbeidByReferanse(referanse: String): String? {
        val oppgaverforBehandling = oppgaveDao.hentOppgaverForReferanse(referanse)
        return try {
            val oppgaveUnderbehandling = oppgaverforBehandling.single { it.status == Status.UNDER_BEHANDLING }
            oppgaveUnderbehandling.saksbehandler
        } catch (e: NoSuchElementException) {
            logger.info("Det må finnes en oppgave under behandling, gjelder referanse: $referanse")
            return null
        } catch (e: IllegalArgumentException) {
            logger.info("Skal kun ha en oppgave under behandling, gjelder referanse: $referanse")
            return null
        }
    }

    private fun opprettOppave(oppgaveIntern: OppgaveIntern): OppgaveIntern {
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

    fun hentSakOgOppgaverForSak(sakId: Long): OppgaveListe {
        val sak = sakDao.hentSak(sakId)
        if (sak != null) {
            return OppgaveListe(sak, hentOppgaverForSak(sak.id))
        } else {
            throw FantIkkeSakException("Fant ikke sakid $sakId")
        }
    }
}

class FantIkkeSakException(msg: String) : Exception(msg)

enum class Rolle {
    SAKSBEHANDLER,
    ATTESTANT,
    STRENGT_FORTROLIG,
}
