package no.nav.etterlatte.oppgave

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.ExternalUser
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.Self
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.User
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveListe
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class BrukerManglerAttestantRolleException(msg: String) : Exception(msg)

class OppgaveService(
    private val oppgaveDao: OppgaveDaoMedEndringssporing,
    private val sakDao: SakDao,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveIntern> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker)
        val aktuelleOppgavetyperForRoller = aktuelleOppgavetyperForRolleTilSaksbehandler(rollerSomBrukerHar)

        return if (bruker.harRolleStrengtFortrolig()) {
            inTransaction {
                oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(aktuelleOppgavetyperForRoller)
            }
        } else {
            inTransaction {
                oppgaveDao.hentOppgaver(aktuelleOppgavetyperForRoller)
            }.sortedByDescending { it.opprettet }
        }.filterForEnheter(Kontekst.get().AppUser)
    }

    private fun List<OppgaveIntern>.filterForEnheter(bruker: User) = this.filterOppgaverForEnheter(featureToggleService, bruker)

    private fun aktuelleOppgavetyperForRolleTilSaksbehandler(roller: List<Rolle>) =
        roller.flatMap {
            when (it) {
                Rolle.SAKSBEHANDLER -> OppgaveType.values().toList() - OppgaveType.ATTESTERING
                Rolle.ATTESTANT -> listOf(OppgaveType.ATTESTERING)
                Rolle.STRENGT_FORTROLIG -> OppgaveType.values().toList()
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
        inTransaction(gjenbruk = true) {
            val hentetOppgave =
                oppgaveDao.hentOppgave(oppgaveId)
                    ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")

            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            hentetOppgave.erAttestering() && sjekkOmkanTildeleAttestantOppgave()
            if (hentetOppgave.saksbehandler.isNullOrEmpty()) {
                oppgaveDao.settNySaksbehandler(oppgaveId, saksbehandler)
            } else {
                throw BadRequestException(
                    "Oppgaven har allerede en saksbehandler, id: $oppgaveId",
                )
            }
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
        inTransaction {
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
        inTransaction {
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
    }

    fun ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
        fattetoppgave: VedtakOppgaveDTO,
        oppgaveType: OppgaveType,
        merknad: String?,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForBehandling(fattetoppgave.referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å kunne lage attesteringsoppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
            return opprettNyOppgaveMedSakOgReferanse(
                referanse = fattetoppgave.referanse,
                sakId = fattetoppgave.sakId,
                oppgaveKilde = oppgaveUnderbehandling.kilde,
                oppgaveType = oppgaveType,
                merknad = merknad,
            )
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgave.referanse}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgave.referanse}",
                e,
            )
        }
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

    fun endreEnhetForOppgaverTilknyttetSak(
        sakId: Long,
        enhetsID: String,
    ) {
        inTransaction {
            val oppgaverForbehandling = oppgaveDao.hentOppgaverForSak(sakId)
            oppgaverForbehandling.forEach {
                oppgaveDao.endreEnhetPaaOppgave(it.id, enhetsID)
            }
        }
    }

    fun hentOppgaverForSak(sakId: Long): List<OppgaveIntern> {
        return inTransaction { oppgaveDao.hentOppgaverForSak(sakId) }
    }

    fun avbrytOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        try {
            val oppgaveUnderbehandling =
                oppgaveDao.hentOppgaverForBehandling(behandlingEllerHendelseId)
                    .single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.AVBRUTT)
            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat avbrøt kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId",
                e,
            )
        }
    }

    fun ferdigStillOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: BrukerTokenInfo,
    ): OppgaveIntern {
        val behandlingsoppgaver = oppgaveDao.hentOppgaverForBehandling(behandlingEllerHendelseId)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å ferdigstille oppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDao.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
            return requireNotNull(oppgaveDao.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat ferdigstilte kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId}",
                e,
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId",
                e,
            )
        }
    }

    fun opprettFoerstegangsbehandlingsOppgaveForInnsendtSoeknad(
        referanse: String,
        sakId: Long,
    ): OppgaveIntern {
        val oppgaverForBehandling = oppgaveDao.hentOppgaverForBehandling(referanse)
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
    ): OppgaveIntern {
        val sak = sakDao.hentSak(sakId)!!
        return lagreOppgave(
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                oppgaveKilde = oppgaveKilde,
                oppgaveType = oppgaveType,
                merknad = merknad,
            ),
        )
    }

    fun hentSaksbehandlerForBehandling(behandlingsId: UUID): String? {
        val oppgaverForBehandlingUtenAttesterting =
            inTransaction {
                oppgaveDao.hentOppgaverForBehandling(behandlingsId.toString())
            }.filter {
                it.type !== OppgaveType.ATTESTERING
            }
        return oppgaverForBehandlingUtenAttesterting.sortedByDescending { it.opprettet }[0].saksbehandler
    }

    fun hentSaksbehandlerFraFoerstegangsbehandling(behandlingsId: UUID): String? {
        val oppgaverForBehandlingFoerstegangs =
            inTransaction(gjenbruk = true) {
                oppgaveDao.hentOppgaverForBehandling(behandlingsId.toString())
            }.filter {
                it.type == OppgaveType.FOERSTEGANGSBEHANDLING
            }
        return oppgaverForBehandlingFoerstegangs.sortedByDescending { it.opprettet }[0].saksbehandler
    }

    fun hentSaksbehandlerForOppgaveUnderArbeid(behandlingsId: UUID): String? {
        val oppgaverforBehandling =
            inTransaction {
                oppgaveDao.hentOppgaverForBehandling(behandlingsId.toString())
            }
        return try {
            val oppgaveUnderbehandling = oppgaverforBehandling.single { it.status == Status.UNDER_BEHANDLING }
            oppgaveUnderbehandling.saksbehandler
        } catch (e: NoSuchElementException) {
            logger.info("Det må finnes en oppgave under behandling, gjelder behandling: $behandlingsId")
            return null
        } catch (e: IllegalArgumentException) {
            logger.info("Skal kun ha en oppgave under behandling, gjelder behandling: $behandlingsId")
            return null
        }
    }

    private fun lagreOppgave(oppgaveIntern: OppgaveIntern): OppgaveIntern {
        var oppgaveLagres = oppgaveIntern
        if (oppgaveIntern.frist === null) {
            val enMaanedFrem = oppgaveIntern.opprettet.toLocalDatetimeUTC().plusMonths(1L).toTidspunkt()
            oppgaveLagres = oppgaveIntern.copy(frist = enMaanedFrem)
        }
        oppgaveDao.lagreOppgave(oppgaveLagres)
        return oppgaveDao.hentOppgave(oppgaveLagres.id)!!
    }

    fun hentOppgave(oppgaveId: UUID): OppgaveIntern? {
        return oppgaveDao.hentOppgave(oppgaveId)
    }

    /**
     * Skal kun brukes for automatisk avbrudd når vi får erstattende førstegangsbehandling i saken
     */
    fun avbrytAapneOppgaverForBehandling(behandlingId: String) {
        oppgaveDao.hentOppgaverForBehandling(behandlingId)
            .filter { !it.erAvsluttet() }
            .forEach {
                oppgaveDao.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
            }
    }

    fun hentSakOgOppgaverForSak(sakId: Long) =
        inTransaction { sakDao.hentSak(sakId)!! }
            .let { OppgaveListe(it, hentOppgaverForSak(it.id)) }
}

fun List<OppgaveIntern>.filterOppgaverForEnheter(
    featureToggleService: FeatureToggleService,
    user: User,
) = this.filterForEnheter(
    featureToggleService,
    OppgaveServiceFeatureToggle.EnhetFilterOppgaver,
    user,
) { item, enheter ->
    enheter.contains(item.enhet)
}

enum class OppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    EnhetFilterOppgaver("pensjon-etterlatte.filter-oppgaver-enhet"),
    ;

    override fun key() = key
}

enum class Rolle {
    SAKSBEHANDLER,
    ATTESTANT,
    STRENGT_FORTROLIG,
}
