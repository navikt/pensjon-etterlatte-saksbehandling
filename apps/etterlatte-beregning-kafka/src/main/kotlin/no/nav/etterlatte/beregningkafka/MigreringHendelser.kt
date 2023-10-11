package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val beregningService: BeregningService) :
    ListenerMedLoggingOgFeilhaandtering(Migreringshendelser.BEREGN) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(hendelsestype)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            correlationId()
        }.register(this)
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Mottatt beregnings-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")

        beregningService.opprettBeregningsgrunnlag(behandlingId, tilGrunnlagDTO(packet.hendelseData))

        val beregning =
            runBlocking {
                beregningService.beregn(behandlingId).body<BeregningDTO>()
            }

        verifiserNyBeregning(beregning, packet.hendelseData)

        packet.eventName = Migreringshendelser.VEDTAK
        packet[BEREGNING_KEY] = beregning
        context.publish(packet.toJson())
        logger.info("Publiserte oppdatert migreringshendelse fra beregning for behandling $behandlingId")
    }
}

private fun verifiserNyBeregning(
    beregning: BeregningDTO,
    migreringRequest: MigreringRequest,
) {
    check(beregning.beregningsperioder.size == 1) {
        "Migrerte saker skal kun opprette en beregningperiode, men oppretta ${beregning.beregningsperioder.size}: " +
            beregning.beregningsperioder.map { Periode(it.datoFOM, it.datoTOM) }.joinToString(", ")
    }

    with(beregning.beregningsperioder.first()) {
        check(trygdetid == migreringRequest.beregning.anvendtTrygdetid.toInt()) {
            "Beregning må være basert på samme trygdetid som i Pesys"
        }
        check(grunnbelop == migreringRequest.beregning.g.toInt()) {
            "Beregning må være basert på samme G som i Pesys"
        }
        check(utbetaltBeloep >= migreringRequest.beregning.brutto.toInt()) {
            "Man skal ikke kunne komme dårligere ut på nytt regelverk. " +
                "Beregnet beløp i Gjenny er lavere enn dagens beløp i Pesys."
        }
        // todo: Vi må også verifisere at samme beregningsmetode har blitt benyttet,
        //  f. eks nasjonal, prorata og evt. avkorting (uføre, inst. opphold, fengsel)
    }
}

private fun tilGrunnlagDTO(request: MigreringRequest): BarnepensjonBeregningsGrunnlag =
    BarnepensjonBeregningsGrunnlag(
        soeskenMedIBeregning =
            listOf(
                GrunnlagMedPeriode(
                    fom = request.virkningstidspunkt.atDay(1),
                    tom = null,
                    data = emptyList(),
                ),
            ),
        institusjonsopphold = emptyList(),
    )
