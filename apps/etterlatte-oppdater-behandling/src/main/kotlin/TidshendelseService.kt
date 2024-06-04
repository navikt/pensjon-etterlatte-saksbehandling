package no.nav.etterlatte

import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.AO_BP20
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.AO_BP21
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.AO_OMS67
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_3AAR
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_4MND
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_5AAR
import no.nav.etterlatte.TidshendelseService.TidshendelserJobbType.OMS_DOED_6MND
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto.VurderingVedMaaned
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.AKTIVITETSPLIKT
import no.nav.etterlatte.libs.common.oppgave.OppgaveType.REVURDERING
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.util.UUID

class TidshendelseService(
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(TidshendelseService::class.java)

    enum class TidshendelserJobbType(val beskrivelse: String) {
        AO_BP20("Aldersovergang v/20 år"),
        AO_BP21("Aldersovergang v/21 år"),
        AO_OMS67("Aldersovergang v/67 år"),
        OMS_DOED_3AAR("Opphør OMS etter 3 år"),
        OMS_DOED_5AAR("Opphør OMS etter 5 år"),
        OMS_DOED_4MND("Infobrev om aktivitetsplikt OMS etter 4 mnd"),
        OMS_DOED_6MND("Vurdering av aktivitetsplikt OMS etter 6 mnd"),
    }

    fun haandterHendelse(hendelse: TidshendelsePacket): TidshendelseResult {
        if (skalSkippes(hendelse)) {
            return TidshendelseResult.Skipped
        }
        logger.info(
            """Løpende ytelse: oppretter behandling/oppgave for sak ${hendelse.sakId} 
                    behandlingsmåned=${hendelse.behandlingsmaaned}""",
        )
        if (skalLageOmregning(hendelse)) {
            try {
                return behandlingService.opprettOmregning(omregningshendelse(hendelse)).let { response ->
                    TidshendelseResult.OpprettetOmregning(
                        response.behandlingId,
                        response.forrigeBehandlingId,
                    )
                }
            } catch (e: Exception) {
                logger.error("Kunne ikke opprette omregning [sak=${hendelse.sakId}]", e)
                return opprettOppgave(hendelse)
                    .let { oppgaveId -> TidshendelseResult.OpprettetOppgave(oppgaveId) }
            }
        } else {
            return when (hendelse.jobbtype) {
                OMS_DOED_4MND -> opprettOppgave(hendelse).let { oppgaveId -> TidshendelseResult.OpprettetOppgave(oppgaveId) }
                OMS_DOED_6MND -> opprettRevurderingForAktivitetsplikt(hendelse)
                else -> throw IllegalArgumentException("Ingen håndtering for jobbtype: ${hendelse.jobbtype} for sak: ${hendelse.sakId}")
            }
        }
    }

    private fun skalSkippes(hendelse: TidshendelsePacket): Boolean {
        if (hendelse.jobbtype == AO_BP20 && hendelse.harMigrertYrkesskadeFordel) {
            logger.info("Har migrert yrkesskadefordel: utvidet aldersgrense [sak=${hendelse.sakId}]")
            return true
        }
        if (hendelse.jobbtype in arrayOf(OMS_DOED_3AAR, OMS_DOED_5AAR) && hendelse.harRettUtenTidsbegrensning) {
            logger.info("Har omstillingsstønad med rett uten tidsbegrensning, opphører ikke [sak=${hendelse.sakId}]")
            return true
        }
        if (hendelse.harLoependeYtelse && hendelse.dryrun) {
            logger.info(
                """Dry run: Løpende ytelse: skipper oppretting av behandling/oppgave
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

    private fun omregningshendelse(hendelse: TidshendelsePacket) =
        Omregningshendelse(
            sakId = hendelse.sakId,
            fradato = hendelse.behandlingsmaaned.plusMonths(1).atDay(1),
            prosesstype = Prosesstype.AUTOMATISK,
            revurderingaarsak = Revurderingaarsak.ALDERSOVERGANG,
            oppgavefrist = hendelse.behandlingsmaaned.atEndOfMonth(),
        )

    private fun opprettOppgave(hendelse: TidshendelsePacket): UUID {
        val oppgaveId =
            behandlingService.opprettOppgave(
                hendelse.sakId,
                oppgaveTypeFor(hendelse.jobbtype),
                referanse = hendelse.behandlingId?.toString(),
                merknad = hendelse.jobbtype.beskrivelse,
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
            )
        logger.info("Opprettet oppgave $oppgaveId [sak=${hendelse.sakId}]")
        return oppgaveId
    }

    private fun opprettRevurderingForAktivitetsplikt(hendelse: TidshendelsePacket): TidshendelseResult {
        val response =
            behandlingService.opprettRevurderingAktivitetsplikt(
                sakId = hendelse.sakId,
                behandlingsmaaned = hendelse.behandlingsmaaned,
                vurderingVedMaaned =
                    when (hendelse.jobbtype) {
                        OMS_DOED_6MND -> VurderingVedMaaned.SEKS_MND
                        else -> throw IllegalArgumentException(
                            "Ingen håndtering for jobbtype: ${hendelse.jobbtype} for sak: ${hendelse.sakId}",
                        )
                    },
                frist = Tidspunkt.ofNorskTidssone(hendelse.behandlingsmaaned.atEndOfMonth(), LocalTime.NOON),
            )

        return when (response.opprettetRevurdering) {
            true -> {
                logger.info("Opprettet revurdering for aktivitetsplikt [sak=${hendelse.sakId}, behandling=$response]")
                TidshendelseResult.Skipped
            }
            false -> {
                logger.info("Det ble ikke opprettet revurdering for aktivitetsplikt [sak=${hendelse.sakId}]")
                TidshendelseResult.OpprettRevurderingForAktivitetsplikt(response.nyBehandlingId)
            }
        }
    }

    private fun skalLageOmregning(hendelse: TidshendelsePacket) =
        when (hendelse.jobbtype) {
            AO_BP20 -> true
            AO_BP21 -> true
            AO_OMS67 -> true
            OMS_DOED_3AAR -> true
            OMS_DOED_5AAR -> true
            OMS_DOED_4MND -> false
            OMS_DOED_6MND -> false
        }

    private fun oppgaveTypeFor(type: TidshendelserJobbType): OppgaveType =
        when (type) {
            AO_BP20 -> REVURDERING
            AO_BP21 -> REVURDERING
            AO_OMS67 -> REVURDERING
            OMS_DOED_3AAR -> REVURDERING
            OMS_DOED_5AAR -> REVURDERING
            OMS_DOED_4MND -> AKTIVITETSPLIKT
            OMS_DOED_6MND -> AKTIVITETSPLIKT
        }
}

sealed class TidshendelseResult {
    data class OpprettetOppgave(val opprettetOppgaveId: UUID) : TidshendelseResult()

    data class OpprettetOmregning(val behandlingId: UUID, val forrigeBehandlingId: UUID) : TidshendelseResult()

    data class OpprettRevurderingForAktivitetsplikt(val behandlingId: UUID?, val oppgaveId: UUID? = null) : TidshendelseResult()

    data object Skipped : TidshendelseResult()
}
