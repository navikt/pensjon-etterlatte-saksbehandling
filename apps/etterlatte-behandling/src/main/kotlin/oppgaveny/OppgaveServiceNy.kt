package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.User
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveNy
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.oppgaveNy.Status
import no.nav.etterlatte.libs.common.oppgaveNy.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.oppgaveNy.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import java.util.*

class OppgaveServiceNy(
    private val oppgaveDaoNy: OppgaveDaoMedEndringssporing,
    private val sakDao: SakDao,
    private val featureToggleService: FeatureToggleService
) {

    fun finnOppgaverForBruker(bruker: SaksbehandlerMedRoller): List<OppgaveNy> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker)
        val aktuelleOppgavetyperForRoller = aktuelleOppgavetyperForRolleTilSaksbehandler(rollerSomBrukerHar)

        return if (bruker.harRolleStrengtFortrolig()) {
            inTransaction {
                oppgaveDaoNy.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(aktuelleOppgavetyperForRoller)
            }
        } else {
            inTransaction {
                oppgaveDaoNy.hentOppgaver(aktuelleOppgavetyperForRoller)
            }.sortedByDescending { it.opprettet }
        }.filterForEnheter(Kontekst.get().AppUser)
    }

    private fun List<OppgaveNy>.filterForEnheter(bruker: User) =
        this.filterOppgaverForEnheter(featureToggleService, bruker)

    private fun aktuelleOppgavetyperForRolleTilSaksbehandler(roller: List<Rolle>) = roller.flatMap {
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
            Rolle.STRENGT_FORTROLIG.takeIf { bruker.harRolleStrengtFortrolig() }
        )

    fun tildelSaksbehandler(oppgaveId: UUID, saksbehandler: String) {
        inTransaction(gjenbruk = true) {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId)
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")

            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            if (hentetOppgave.saksbehandler.isNullOrEmpty()) {
                oppgaveDaoNy.settNySaksbehandler(oppgaveId, saksbehandler)
            } else {
                throw BadRequestException(
                    "Oppgaven har allerede en saksbehandler, id: $oppgaveId"
                )
            }
        }
    }

    fun byttSaksbehandler(oppgaveId: UUID, saksbehandler: String) {
        val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId)
        if (hentetOppgave != null) {
            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            oppgaveDaoNy.settNySaksbehandler(oppgaveId, saksbehandler)
        } else {
            throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")
        }
    }

    fun fjernSaksbehandler(oppgaveId: UUID) {
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId)
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")

            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            if (hentetOppgave.saksbehandler != null) {
                oppgaveDaoNy.fjernSaksbehandler(oppgaveId)
            } else {
                throw BadRequestException(
                    "Oppgaven har ingen saksbehandler, id: $oppgaveId"
                )
            }
        }
    }

    private fun sikreAtOppgaveIkkeErAvsluttet(oppgave: OppgaveNy) {
        if (oppgave.erAvsluttet()) {
            throw IllegalStateException(
                "Oppgave med id ${oppgave.id} kan ikke endres siden den har " +
                    "status ${oppgave.status}"
            )
        }
    }

    fun redigerFrist(oppgaveId: UUID, frist: Tidspunkt) {
        inTransaction {
            if (frist.isBefore(Tidspunkt.now())) {
                throw BadRequestException("Tidspunkt tilbake i tid id: $oppgaveId")
            }
            val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId)
                ?: throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")
            sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
            if (hentetOppgave.saksbehandler != null) {
                oppgaveDaoNy.redigerFrist(oppgaveId, frist)
            } else {
                throw BadRequestException(
                    "Oppgaven har ingen saksbehandler, id: $oppgaveId"
                )
            }
        }
    }

    fun ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
        fattetoppgave: VedtakOppgaveDTO,
        oppgaveType: OppgaveType,
        merknad: String?,
        saksbehandler: String
    ): OppgaveNy {
        val behandlingsoppgaver = oppgaveDaoNy.hentOppgaverForBehandling(fattetoppgave.referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å kunne lage attesteringsoppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDaoNy.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
            return opprettNyOppgaveMedSakOgReferanse(
                referanse = fattetoppgave.referanse,
                sakId = fattetoppgave.sakId,
                oppgaveKilde = oppgaveUnderbehandling.kilde,
                oppgaveType = oppgaveType,
                merknad = merknad
            )
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgave.referanse}",
                e
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling:" +
                    " ${fattetoppgave.referanse}",
                e
            )
        }
    }

    private fun sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(
        oppgaveUnderBehandling: OppgaveNy,
        saksbehandler: String
    ) {
        if (oppgaveUnderBehandling.saksbehandler != saksbehandler) {
            throw FeilSaksbehandlerPaaOppgaveException(
                "Kan ikke lukke oppgave for en annen saksbehandler oppgave:" +
                    " ${oppgaveUnderBehandling.id}"
            )
        }
    }

    class FeilSaksbehandlerPaaOppgaveException(message: String) : Exception(message)

    fun endreEnhetForOppgaverTilknyttetSak(
        sakId: Long,
        enhetsID: String
    ) {
        inTransaction {
            val oppgaverForbehandling = oppgaveDaoNy.hentOppgaverForSak(sakId)
            oppgaverForbehandling.forEach {
                oppgaveDaoNy.endreEnhetPaaOppgave(it.id, enhetsID)
            }
        }
    }

    fun hentOppgaverForSak(sakId: Long): List<OppgaveNy> {
        return inTransaction { oppgaveDaoNy.hentOppgaverForSak(sakId) }
    }

    fun avbrytOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: String
    ): OppgaveNy {
        try {
            val oppgaveUnderbehandling = oppgaveDaoNy.hentOppgaverForBehandling(behandlingEllerHendelseId)
                .single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDaoNy.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.AVBRUTT)
            return requireNotNull(oppgaveDaoNy.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat avbrøt kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId}",
                e
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId",
                e
            )
        }
    }

    fun ferdigStillOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: String
    ): OppgaveNy {
        val behandlingsoppgaver = oppgaveDaoNy.hentOppgaverForBehandling(behandlingEllerHendelseId)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å ferdigstille oppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            sikreAtSaksbehandlerSomLukkerOppgaveEierOppgaven(oppgaveUnderbehandling, saksbehandler)
            oppgaveDaoNy.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
            return requireNotNull(oppgaveDaoNy.hentOppgave(oppgaveUnderbehandling.id)) {
                "Oppgaven vi akkurat ferdigstilte kunne ikke hentes ut"
            }
        } catch (e: NoSuchElementException) {
            throw BadRequestException(
                "Det må finnes en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId}",
                e
            )
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(
                "Skal kun ha en oppgave under behandling, gjelder behandling / hendelse med ID:" +
                    " $behandlingEllerHendelseId",
                e
            )
        }
    }

    fun opprettFoerstegangsbehandlingsOppgaveForInnsendSoeknad(referanse: String, sakId: Long): OppgaveNy {
        val oppgaverForBehandling = oppgaveDaoNy.hentOppgaverForBehandling(referanse)
        val oppgaverSomKanLukkes = oppgaverForBehandling.filter { !it.erAvsluttet() }
        oppgaverSomKanLukkes.forEach {
            oppgaveDaoNy.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
        }

        return opprettNyOppgaveMedSakOgReferanse(
            referanse = referanse,
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = null
        )
    }

    fun opprettNyOppgaveMedSakOgReferanse(
        referanse: String,
        sakId: Long,
        oppgaveKilde: OppgaveKilde?,
        oppgaveType: OppgaveType,
        merknad: String?
    ): OppgaveNy {
        val sak = sakDao.hentSak(sakId)!!
        return lagreOppgave(
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                oppgaveKilde = oppgaveKilde,
                oppgaveType = oppgaveType,
                merknad = merknad
            )
        )
    }

    fun hentSaksbehandlerForBehandling(behandlingsId: UUID): String? {
        val oppgaverforBehandling = inTransaction {
            oppgaveDaoNy.hentOppgaverForBehandling(behandlingsId.toString())
        }

        val oppgaverForBehandlingUtenAttesterting = oppgaverforBehandling.filter {
            it.type !== OppgaveType.ATTESTERING
        }
        return oppgaverForBehandlingUtenAttesterting.sortedByDescending { it.opprettet }[0].saksbehandler
    }

    private fun lagreOppgave(oppgaveNy: OppgaveNy): OppgaveNy {
        var oppgaveLagres = oppgaveNy
        if (oppgaveNy.frist === null) {
            val enMaanedFrem = oppgaveNy.opprettet.toLocalDatetimeUTC().plusMonths(1L).toTidspunkt()
            oppgaveLagres = oppgaveNy.copy(frist = enMaanedFrem)
        }
        oppgaveDaoNy.lagreOppgave(oppgaveLagres)
        return oppgaveDaoNy.hentOppgave(oppgaveLagres.id)!!
    }

    fun hentOppgave(oppgaveId: UUID): OppgaveNy? {
        return oppgaveDaoNy.hentOppgave(oppgaveId)
    }

    /**
     * Skal kun brukes for automatisk avbrudd når vi får erstattende førstegangsbehandling i saken
     */
    fun avbrytAapneOppgaverForBehandling(behandlingId: String) {
        oppgaveDaoNy.hentOppgaverForBehandling(behandlingId)
            .filter { !it.erAvsluttet() }
            .forEach {
                oppgaveDaoNy.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
            }
    }
}

fun List<OppgaveNy>.filterOppgaverForEnheter(
    featureToggleService: FeatureToggleService,
    user: User
) = this.filterForEnheter(
    featureToggleService,
    OppgaveServiceFeatureToggle.EnhetFilterOppgaver,
    user
) { item, enheter ->
    enheter.contains(item.enhet)
}

enum class OppgaveServiceFeatureToggle(private val key: String) : FeatureToggle {
    EnhetFilterOppgaver("pensjon-etterlatte.filter-oppgaver-enhet");

    override fun key() = key
}

enum class Rolle {
    SAKSBEHANDLER, ATTESTANT, STRENGT_FORTROLIG
}