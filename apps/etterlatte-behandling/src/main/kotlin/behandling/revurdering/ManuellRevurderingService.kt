package no.nav.etterlatte.behandling.revurdering

import io.ktor.server.plugins.BadRequestException
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
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
            } catch (_: Exception) {
                throw UgyldigForespoerselException(
                    "UGYLDIG_HENDELSE_ID",
                    "$aarsak har en ugyldig hendelse id for sakid" +
                        " $sakId. " +
                        "Hendelsesid: $paaGrunnAvHendelseId",
                )
            }

        val paaGrunnAvOppgaveUuid =
            try {
                paaGrunnAvOppgaveId?.let { UUID.fromString(it) }
            } catch (_: Exception) {
                throw BadRequestException("Ugyldig oppgaveId $paaGrunnAvOppgaveId (sakid=$sakId).")
            }

        revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        val forrigeIverksatteBehandling =
            behandlingService.hentSisteIverksatteBehandling(sakId)
                ?: throw RevurderingManglerIverksattBehandling(sakId)

        if (forrigeIverksatteBehandling.status != BehandlingStatus.IVERKSATT) {
            throw UgyldigForespoerselException(
                code = "BEHANDLING_BLOKKERER_REVURDERING",
                detail =
                    "Kan ikke opprette ny revurdering når forrige behandling har status " +
                        "${forrigeIverksatteBehandling.status}, id=${forrigeIverksatteBehandling.id}",
            )
        }

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
            opprinnelse =
                if (paaGrunnAvHendelseUuid != null) {
                    BehandlingOpprinnelse.HENDELSE
                } else if (paaGrunnAvOppgaveUuid != null) {
                    BehandlingOpprinnelse.MELD_INN_ENDRING_SKJEMA
                } else {
                    BehandlingOpprinnelse.SAKSBEHANDLER
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
        opprinnelse: BehandlingOpprinnelse,
    ): Revurdering =
        forrigeBehandling.let {
            val persongalleri = grunnlagService.hentPersongalleri(forrigeBehandling.id)
            val triggendeOppgave = paaGrunnAvOppgave?.let { oppgaveService.hentOppgave(it) }

            revurderingService
                .opprettRevurdering(
                    sakId = sakId,
                    persongalleri = krevIkkeNull(persongalleri) { "Persongalleri mangler for sak=$sakId" },
                    forrigeBehandling = forrigeBehandling,
                    mottattDato = Tidspunkt.now().toLocalDatetimeUTC().toString(),
                    prosessType = Prosesstype.MANUELL,
                    kilde = Vedtaksloesning.GJENNY,
                    revurderingAarsak = revurderingAarsak,
                    virkningstidspunkt = null,
                    begrunnelse = begrunnelse ?: triggendeOppgave?.merknad,
                    saksbehandlerIdent = saksbehandler.ident,
                    frist = triggendeOppgave?.frist,
                    paaGrunnAvOppgave = paaGrunnAvOppgave,
                    opprinnelse = opprinnelse,
                ).oppdater()
                .also { revurdering ->
                    if (!fritekstAarsak.isNullOrEmpty() && revurdering.revurderingsaarsak!!.kanLagreFritekstFeltForManuellRevurdering()) {
                        revurderingService.lagreRevurderingsaarsakFritekstForRevurderingAnnenMedEllerUtenBrev(
                            fritekstAarsak,
                            revurdering,
                            saksbehandler.ident,
                        )
                    }

                    if (paaGrunnAvHendelse != null) {
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIRevurdering(
                            paaGrunnAvHendelse,
                            revurdering.id,
                        )
                        try {
                            oppgaveService.ferdigstillOppgaveUnderBehandling(
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
