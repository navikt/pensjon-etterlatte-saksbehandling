package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.dbutils.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class ManuellRevurderingService(
    private val revurderingService: RevurderingService,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val oppgaveService: OppgaveService,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettManuellRevurderingWrapper(
        sakId: SakId,
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

        revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        val forrigeIverksatteBehandling =
            behandlingService.hentSisteIverksatte(sakId)
                ?: throw RevurderingManglerIverksattBehandling(sakId)

        val sakType = forrigeIverksatteBehandling.sak.sakType
        if (!aarsak.gyldigForSakType(sakType)) {
            throw BadRequestException("$aarsak er ikke støttet for $sakType")
        }
        kanOppretteRevurderingForAarsak(aarsak)
        return opprettManuellRevurdering(
            sakId = forrigeIverksatteBehandling.sak.id,
            forrigeBehandling = forrigeIverksatteBehandling,
            revurderingAarsak = aarsak,
            paaGrunnAvHendelse = paaGrunnAvHendelseUuid,
            paaGrunnAvOppgave = paaGrunnAvOppgaveUuid,
            begrunnelse = begrunnelse,
            fritekstAarsak = fritekstAarsak,
            saksbehandler = saksbehandler,
            opphoerFraOgMed =
                if (aarsak !=
                    Revurderingaarsak.REVURDERE_ETTER_OPPHOER
                ) {
                    forrigeIverksatteBehandling.opphoerFraOgMed
                } else {
                    null
                },
        )
    }

    private fun kanOppretteRevurderingForAarsak(aarsak: Revurderingaarsak) {
        if (aarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
            throw UgyldigForespoerselException(
                code = "OMGJOERING_KLAGE_MAA_OPPRETTES_FRA_OPPGAVE",
                detail = "Omgjøring etter klage må opprettes fra en omgjøringsoppgave for å koble omgjøringen til klagen riktig.",
            )
        }
    }

    private fun opprettManuellRevurdering(
        sakId: SakId,
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

            revurderingService
                .opprettRevurdering(
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
}
