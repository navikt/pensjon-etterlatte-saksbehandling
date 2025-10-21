package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import java.util.UUID

class OmgjoeringKlageRevurderingService(
    private val revurderingService: RevurderingService,
    private val oppgaveService: OppgaveService,
    private val klageService: KlageService,
    private val behandlingDao: BehandlingDao,
    private val grunnlagService: GrunnlagService,
) {
    fun opprettOmgjoeringKlage(
        sakId: SakId,
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

        val persongalleri = grunnlagService.hentPersongalleri(sakId)
        return revurderingService
            .opprettRevurdering(
                sakId = sakId,
                persongalleri = krevIkkeNull(persongalleri) { "Persongalleri mangler for sak=$sakId" },
                forrigeBehandling = behandlingSomOmgjoeres,
                mottattDato =
                    klagenViOmgjoerPaaGrunnAv.innkommendeDokument
                        ?.mottattDato
                        ?.atStartOfDay()
                        ?.toString(),
                prosessType = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                virkningstidspunkt = behandlingSomOmgjoeres.virkningstidspunkt,
                begrunnelse = "Omgjøring på grunn av klage",
                saksbehandlerIdent = saksbehandler.ident,
                relatertBehandlingId = klagenViOmgjoerPaaGrunnAv.id.toString(),
                opprinnelse = BehandlingOpprinnelse.SAKSBEHANDLER,
            ).oppdater()
            .also {
                oppgaveService.ferdigstillOppgaveUnderBehandling(
                    referanse = klagenViOmgjoerPaaGrunnAv.id.toString(),
                    type = OppgaveType.OMGJOERING,
                    saksbehandler = saksbehandler,
                )
            }
    }
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
        sakId: SakId,
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
