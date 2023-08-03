package no.nav.etterlatte.oppgaveny

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.User
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
import no.nav.etterlatte.oppgave.OppgaveServiceFeatureToggle
import no.nav.etterlatte.oppgave.Rolle
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import java.util.*

class OppgaveServiceNy(
    private val oppgaveDaoNy: OppgaveDaoMedEndringssporing,
    private val sakDao: SakDao,
    private val kanBrukeNyOppgaveliste: Boolean,
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
        inTransaction {
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
        inTransaction {
            val hentetOppgave = oppgaveDaoNy.hentOppgave(oppgaveId)
            if (hentetOppgave != null) {
                sikreAtOppgaveIkkeErAvsluttet(hentetOppgave)
                oppgaveDaoNy.settNySaksbehandler(oppgaveId, saksbehandler)
            } else {
                throw NotFoundException("Oppgaven finnes ikke, id: $oppgaveId")
            }
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

    private fun sikreAtSaksbehandlerErSattPaaOppgaveHvisNyOppgavelisteIkkeErStoettet(
        behandlingId: String,
        saksbehandler: String?
    ) {
        if (!kanBrukeNyOppgaveliste && saksbehandler != null) {
            // Vi sikrer at saksbehandler tar oppgaven før de fullfører den
            val oppgaveUnderBehandling = oppgaveDaoNy.hentOppgaverForBehandling(behandlingId)
                .single { it.status == Status.UNDER_BEHANDLING || it.status == Status.NY }
            if (oppgaveUnderBehandling.saksbehandler != saksbehandler) {
                byttSaksbehandler(
                    oppgaveId = oppgaveUnderBehandling.id,
                    saksbehandler = saksbehandler
                )
            }
        }
    }

    fun lukkOppgaveUnderbehandlingOgLagNyMedType(
        fattetoppgave: VedtakOppgaveDTO,
        oppgaveType: OppgaveType,
        saksbehandler: String? = null
    ): OppgaveNy {
        sikreAtSaksbehandlerErSattPaaOppgaveHvisNyOppgavelisteIkkeErStoettet(fattetoppgave.referanse, saksbehandler)

        val behandlingsoppgaver = oppgaveDaoNy.hentOppgaverForBehandling(fattetoppgave.referanse)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å kunne lage attesteringsoppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
            oppgaveDaoNy.endreStatusPaaOppgave(oppgaveUnderbehandling.id, Status.FERDIGSTILT)
            return opprettNyOppgaveMedSakOgReferanse(
                fattetoppgave.referanse,
                fattetoppgave.sakId,
                oppgaveUnderbehandling.kilde,
                oppgaveType
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


    fun avbrytOppgaveUnderBehandling(
        behandlingEllerHendelseId: String,
        saksbehandler: String? = null
    ): OppgaveNy {
        sikreAtSaksbehandlerErSattPaaOppgaveHvisNyOppgavelisteIkkeErStoettet(
            behandlingEllerHendelseId,
            saksbehandler
        )
        try {
            val oppgaveUnderbehandling = oppgaveDaoNy.hentOppgaverForBehandling(behandlingEllerHendelseId)
                .single { it.status == Status.UNDER_BEHANDLING }
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
        saksbehandler: String? = null
    ): OppgaveNy {
        sikreAtSaksbehandlerErSattPaaOppgaveHvisNyOppgavelisteIkkeErStoettet(
            behandlingEllerHendelseId,
            saksbehandler
        )
        val behandlingsoppgaver = oppgaveDaoNy.hentOppgaverForBehandling(behandlingEllerHendelseId)
        if (behandlingsoppgaver.isEmpty()) {
            throw BadRequestException("Må ha en oppgave for å ferdigstille oppgave")
        }
        try {
            val oppgaveUnderbehandling = behandlingsoppgaver.single { it.status == Status.UNDER_BEHANDLING }
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
        val oppgaverSomKanLukkes = oppgaverForBehandling.filter { o ->
            o.status in listOf(
                Status.UNDER_BEHANDLING,
                Status.NY
            )
        }
        oppgaverSomKanLukkes.forEach {
            oppgaveDaoNy.endreStatusPaaOppgave(it.id, Status.AVBRUTT)
        }

        return opprettNyOppgaveMedSakOgReferanse(
            referanse,
            sakId,
            OppgaveKilde.BEHANDLING,
            OppgaveType.FOERSTEGANGSBEHANDLING
        )
    }

    fun opprettNyOppgaveMedSakOgReferanse(
        referanse: String,
        sakId: Long,
        oppgaveKilde: OppgaveKilde?,
        oppgaveType: OppgaveType
    ): OppgaveNy {
        val sak = sakDao.hentSak(sakId)!!
        return lagreOppgave(
            opprettNyOppgaveMedReferanseOgSak(
                referanse = referanse,
                sak = sak,
                oppgaveKilde = oppgaveKilde,
                oppgaveType = oppgaveType
            )
        )
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