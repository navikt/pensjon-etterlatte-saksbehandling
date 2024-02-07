package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.rapidsandrivers.BEHANDLING_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BEREGNING_KEY
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.behandlingId
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class MigreringBeregningHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.BEREGN) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
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

        packet.setEventNameForHendelseType(Migreringshendelser.VEDTAK)
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
        check(beregningsMetode == migreringRequest.finnBeregningsmetode()) {
            "Migrerte saker skal benytte samme beregningsmetode som Pesys. " +
                "Kun folketrygd (nasjonal)"
        }

        check(utbetaltBeloep >= migreringRequest.beregning.brutto) {
            "Man skal ikke kunne komme dårligere ut på nytt regelverk. " +
                "Beregnet beløp i Gjenny ($utbetaltBeloep) er lavere enn dagens beløp i Pesys " +
                "(${migreringRequest.beregning.brutto})."
        }

        when (migreringRequest.finnBeregningsmetode()) {
            BeregningsMetode.NASJONAL -> {
                check(trygdetid == migreringRequest.beregning.anvendtTrygdetid) {
                    "Beregning må være basert på samme trygdetid som i Pesys. Er $trygdetid i Gjenny" +
                        ", var ${migreringRequest.beregning.anvendtTrygdetid} i Pesys."
                }
                check(trygdetid == samletNorskTrygdetid) {
                    "Trygdetid ($trygdetid) skal ha samme verdi som samletNorskTrygdetid ($samletNorskTrygdetid) i " +
                        "nasjonal beregning."
                }
            }
            BeregningsMetode.PRORATA -> throw IllegalStateException("Gjenoppretting støtter ikke beregningsMetode.PRORATA")
            BeregningsMetode.BEST -> throw IllegalStateException("Gjenoppretting støtter ikke beregningsMetode.BEST")
        }
    }
}

private fun tilGrunnlagDTO(request: MigreringRequest): BarnepensjonBeregningsGrunnlag =
    BarnepensjonBeregningsGrunnlag(
        soeskenMedIBeregning =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = null,
                    data = emptyList(),
                ),
            ),
        institusjonsopphold = emptyList(),
        beregningsMetode =
            BeregningsMetodeBeregningsgrunnlag(
                request.finnBeregningsmetode(),
                "Gjenoppretta automatisk basert på sak fra Pesys",
            ),
    )

private fun MigreringRequest.finnBeregningsmetode(): BeregningsMetode =
    when (val beregningsMetode = this.beregning.meta?.beregningsMetodeType) {
        "FOLKETRYGD" -> BeregningsMetode.NASJONAL
        "EOS" -> throw IllegalStateException("Vi ønsker ikke å beregne saker med EOS for autmatisk gjenoppretting")
        "USA" -> throw IllegalStateException("Vi klarer ikke beregne saker etter beregningsmetode USA")
        "NORDISK" -> throw IllegalStateException("Vi klarer ikke beregne etter beregningsmetode NORDISK")
        null -> throw IllegalStateException("Vi kan ikke beregne saker som har vært overstyrt i Pesys")
        else -> throw IllegalStateException("Har fått inn sak med ukjent beregningsmetode: $beregningsMetode")
    }
