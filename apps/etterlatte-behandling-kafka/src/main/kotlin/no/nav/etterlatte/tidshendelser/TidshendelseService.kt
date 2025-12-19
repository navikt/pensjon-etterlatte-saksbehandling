package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.AKTIVITETSPLIKT
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.AKTIVITETSPLIKT_12MND
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.AKTIVITETSPLIKT_REVURDERING
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.REVURDERING
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.tidshendelser.JobbType
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.UUID

class TidshendelseService(
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterHendelse(hendelse: TidshendelsePacket): TidshendelseResult {
        if (skalSkippes(hendelse)) {
            return TidshendelseResult.Skipped
        }
        logger.info(
            """Løpende ytelse: behandler tidshendelse for sak ${hendelse.sakId} 
                    behandlingsmåned=${hendelse.behandlingsmaaned}""",
        )

        return when (hendelse.jobbtype) {
            JobbType.OMS_DOED_4MND, JobbType.OMS_DOED_10MND -> opprettAktivitetspliktOppgave(hendelse)
            JobbType.OMS_DOED_6MND, JobbType.OMS_DOED_12MND -> opprettRevurderingForAktivitetsplikt(hendelse)
            JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK -> opprettOppgaveForAktivitetspliktVarigUnntak(hendelse)
            JobbType.OP_BP_FYLT_18 -> opprettOppgaveForBpFylt18Aar(hendelse)

            JobbType.AO_BP20, JobbType.AO_BP21, JobbType.AO_OMS67, JobbType.OMS_DOED_3AAR, JobbType.OMS_DOED_5AAR ->
                opprettAutomatiskRevurdering(
                    hendelse,
                )

            JobbType.OPPDATER_SKJERMING_BP -> oppdaterSkjerming(hendelse)

            else -> throw IllegalArgumentException("Ingen håndtering for jobbtype: ${hendelse.jobbtype} for sak: ${hendelse.sakId}")
        }
    }

    private fun opprettAutomatiskRevurdering(hendelse: TidshendelsePacket): TidshendelseResult {
        try {
            return behandlingService.opprettAutomatiskRevurdering(revurderingRequest(hendelse)).let { response ->
                TidshendelseResult.OpprettetOmregning(
                    response.behandlingId,
                    response.forrigeBehandlingId,
                )
            }
        } catch (e: Exception) {
            logger.error("Kunne ikke opprette omregning [sak=${hendelse.sakId}]", e)
            return opprettOppgaveOpphoerYtelse(hendelse)
                .let { oppgaveId -> TidshendelseResult.OpprettetOppgave(oppgaveId) }
        }
    }

    private fun oppdaterSkjerming(hendelse: TidshendelsePacket): TidshendelseResult {
        logger.info("Oppdaterer skjerming (hvis endringer) for sak=${hendelse.sakId}")
        behandlingService.oppdaterSkjerming(hendelse.sakId)
        return TidshendelseResult.OppdatertSak
    }

    private fun skalSkippes(hendelse: TidshendelsePacket): Boolean {
        if (hendelse.jobbtype == JobbType.AO_BP20 && hendelse.harMigrertYrkesskadeFordel) {
            logger.info("Har migrert yrkesskadefordel: utvidet aldersgrense [sak=${hendelse.sakId}]")
            return true
        }
        if (hendelse.jobbtype in
            arrayOf(
                JobbType.OMS_DOED_3AAR,
                JobbType.OMS_DOED_5AAR,
            ) &&
            hendelse.harRettUtenTidsbegrensning
        ) {
            logger.info("Har omstillingsstønad med rett uten tidsbegrensning, opphører ikke [sak=${hendelse.sakId}]")
            return true
        }
        if (hendelse.harLoependeYtelse && hendelse.dryrun) {
            logger.info(
                """Dry run: Løpende ytelse: skipper behandling av tidshendelse
                    for sak ${hendelse.sakId} behandlingsmåned=${hendelse.behandlingsmaaned}""",
            )
            return true
        }
        if (!hendelse.harLoependeYtelse) {
            logger.info("Ingen løpende ytelse funnet for sak ${hendelse.sakId}")
            return true
        }
        return false
    }

    private fun revurderingRequest(hendelse: TidshendelsePacket) =
        AutomatiskRevurderingRequest(
            sakId = hendelse.sakId,
            fraDato = hendelse.behandlingsmaaned.plusMonths(1).atDay(1),
            revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            oppgavefrist = hendelse.behandlingsmaaned.atEndOfMonth(),
        )

    private fun opprettOppgaveOpphoerYtelse(hendelse: TidshendelsePacket): UUID {
        val oppgaveId =
            behandlingService.opprettOppgave(
                hendelse.sakId,
                oppgaveTypeFor(hendelse.jobbtype),
                referanse = null, // Settes til null da opprettelse av behandling feilet. Har da ingen gyldig referanse.
                merknad = hendelse.jobbtype.beskrivelse,
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
            )
        logger.info("Opprettet oppgave $oppgaveId [sak=${hendelse.sakId}]")
        return oppgaveId
    }

    private fun opprettAktivitetspliktOppgave(hendelse: TidshendelsePacket): TidshendelseResult {
        if (hendelse.jobbtype !in listOf(JobbType.OMS_DOED_4MND, JobbType.OMS_DOED_10MND)) {
            throw InternfeilException(
                "Ingen håndtering for jobbtype: ${hendelse.jobbtype} som " +
                    "aktivitetspliktoppgave for sak: ${hendelse.sakId}",
            )
        }
        val response =
            behandlingService.opprettOppgaveOppfoelgingAktivitetsplikt(
                sakId = hendelse.sakId,
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
                jobbType = hendelse.jobbtype,
                referanse = null,
            )

        return when {
            response.opprettetOppgave -> {
                logger.info(
                    "Opprettet oppgave for infobrev aktivitetsplikt for jobbtype ${hendelse.jobbtype}" +
                        "i sak ${hendelse.sakId}",
                )
                TidshendelseResult.OpprettetOppgave(response.oppgaveId!!)
            }

            else -> TidshendelseResult.Skipped
        }
    }

    private fun opprettRevurderingForAktivitetsplikt(hendelse: TidshendelsePacket): TidshendelseResult {
        val response =
            behandlingService.opprettRevurderingAktivitetsplikt(
                sakId = hendelse.sakId,
                behandlingsmaaned = hendelse.behandlingsmaaned,
                jobbType = hendelse.jobbtype,
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
            )

        return when {
            response.opprettetRevurdering -> {
                logger.info("Opprettet revurdering for aktivitetsplikt [sak=${hendelse.sakId}, behandling=$response]")
                TidshendelseResult.OpprettRevurderingForAktivitetsplikt(response.nyBehandlingId!!)
            }

            response.opprettetOppgave -> {
                logger.info("Opprettet oppgave for aktivitetsplikt [sak=${hendelse.sakId}, oppgave=${response.oppgaveId}]")
                TidshendelseResult.OpprettetOppgave(response.oppgaveId!!)
            }

            else -> {
                logger.info("Det ble ikke opprettet revurdering for aktivitetsplikt [sak=${hendelse.sakId}]")
                TidshendelseResult.Skipped
            }
        }
    }

    private fun opprettOppgaveForBpFylt18Aar(hendelse: TidshendelsePacket): TidshendelseResult {
        val oppgaveId =
            behandlingService.opprettOppgave(
                hendelse.sakId,
                oppgaveTypeFor(hendelse.jobbtype),
                referanse = null,
                merknad = hendelse.jobbtype.beskrivelse,
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.minusMonths(1).atDay(21), LocalTime.NOON),
            )
        logger.info("Opprettet oppgave $oppgaveId [sak=${hendelse.sakId}]")
        return TidshendelseResult.OpprettetOppgave(oppgaveId)
    }

    private fun opprettOppgaveForAktivitetspliktVarigUnntak(hendelse: TidshendelsePacket): TidshendelseResult {
        val response =
            behandlingService.opprettOppgaveAktivitetspliktVarigUnntak(
                sakId = hendelse.sakId,
                referanse = hendelse.behandlingId?.toString(),
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
                jobbType = JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK,
            )

        return when {
            response.opprettetOppgave -> {
                logger.info("Opprettet oppgave for aktivitetsplikt [sak=${hendelse.sakId}, oppgave=${response.oppgaveId}]")
                TidshendelseResult.OpprettetOppgave(response.oppgaveId!!)
            }

            else -> {
                logger.info("Det ble ikke opprettet oppgave for aktivitetsplikt varig unntak [sak=${hendelse.sakId}]")
                TidshendelseResult.Skipped
            }
        }
    }

    private fun oppgaveTypeFor(type: JobbType): OppgaveType =
        when (type) {
            JobbType.AO_BP20 -> REVURDERING
            JobbType.AO_BP21 -> REVURDERING
            JobbType.AO_OMS67 -> REVURDERING
            JobbType.OMS_DOED_3AAR -> REVURDERING
            JobbType.OMS_DOED_5AAR -> REVURDERING
            JobbType.OMS_DOED_4MND -> AKTIVITETSPLIKT
            JobbType.OMS_DOED_10MND -> AKTIVITETSPLIKT_12MND
            JobbType.OMS_DOED_6MND, JobbType.OMS_DOED_12MND -> AKTIVITETSPLIKT_REVURDERING
            JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK -> AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK
            JobbType.OP_BP_FYLT_18 -> OppgaveType.OPPFOELGING
            JobbType.REGULERING,
            JobbType.FINN_SAKER_TIL_REGULERING,
            JobbType.AARLIG_INNTEKTSJUSTERING,
            JobbType.OPPDATER_SKJERMING_BP,
            JobbType.OPPRETT_ETTEROPPGJOER_FORBEHANDLING,
            -> throw InternfeilException("Skal ikke lage oppgave for jobbtype: $type")
        }
}

sealed class TidshendelseResult {
    data class OpprettetOppgave(
        val opprettetOppgaveId: UUID,
    ) : TidshendelseResult()

    data class OpprettetOmregning(
        val behandlingId: UUID,
        val forrigeBehandlingId: UUID,
    ) : TidshendelseResult()

    data class OpprettRevurderingForAktivitetsplikt(
        val behandlingId: UUID,
    ) : TidshendelseResult()

    data object OppdatertSak : TidshendelseResult()

    data object Skipped : TidshendelseResult()
}
