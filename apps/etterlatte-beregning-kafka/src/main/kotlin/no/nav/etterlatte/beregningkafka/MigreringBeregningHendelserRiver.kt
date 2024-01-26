package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.BEREGNING_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
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

        packet.eventName = Migreringshendelser.VEDTAK.lagEventnameForType()
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
        check(grunnbelop == migreringRequest.beregning.g) {
            "Beregning må være basert på samme G som i Pesys. Er $grunnbelop i Gjenny, " +
                "var ${migreringRequest.beregning.g} i Pesys."
        }
        check(utbetaltBeloep >= migreringRequest.beregning.brutto) {
            "Man skal ikke kunne komme dårligere ut på nytt regelverk. " +
                "Beregnet beløp i Gjenny ($utbetaltBeloep) er lavere enn dagens beløp i Pesys " +
                "(${migreringRequest.beregning.brutto})."
        }
        check(beregningsMetode == migreringRequest.finnBeregningsmetode()) {
            "Migrerte saker skal benytte samme beregningsmetode som Pesys. " +
                "Kun folketrygd (nasjonal) og EOS (prorata) er støttet."
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
            BeregningsMetode.PRORATA -> {
                check(broek != null && broek == migreringRequest.beregning.prorataBroek) {
                    "Det er brukt ulik proratabrøk i Pesys (${migreringRequest.beregning.prorataBroek}) og Gjenny " +
                        "($broek), beregningen er trolig feil."
                }
                val anvendtTTBroek =
                    migreringRequest.beregning.anvendtTrygdetid *
                        migreringRequest.beregning.prorataBroek!!.teller /
                        migreringRequest.beregning.prorataBroek!!.nevner
                check(trygdetid == anvendtTTBroek) {
                    "Trygdetid har blitt regnet ulikt i beregning mellom Pesys og Gjenny"
                }
                check(samletTeoretiskTrygdetid == migreringRequest.beregning.anvendtTrygdetid) {
                    "Anvendt trygdetid i Pesys (${migreringRequest.beregning.anvendtTrygdetid}) stemmer ikke overens " +
                        "med samletTeoretiskTrygdetid $samletTeoretiskTrygdetid som er brukt i Gjenny"
                }
            }
            BeregningsMetode.BEST -> throw IllegalStateException("Migrering støtter ikke beregningsMetode.BEST")
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
        beregningsMetode = BeregningsMetodeBeregningsgrunnlag(request.finnBeregningsmetode(), "Migrert fra Pesys"),
    )

private fun MigreringRequest.finnBeregningsmetode(): BeregningsMetode =
    when (val beregningsMetode = this.beregning.meta?.beregningsMetodeType) {
        "FOLKETRYGD" -> BeregningsMetode.NASJONAL
        "EOS" -> BeregningsMetode.PRORATA
        "USA" -> throw IllegalStateException("Vi klarer ikke beregne saker etter beregningsmetode USA")
        "NORDISK" -> throw IllegalStateException("Vi klarer ikke beregne etter beregningsmetode NORDISK")
        null -> throw IllegalStateException("Vi kan ikke beregne saker som har vært overstyrt i Pesys")
        else -> throw IllegalStateException("Har fått inn sak med ukjent beregningsmetode: $beregningsMetode")
    }
