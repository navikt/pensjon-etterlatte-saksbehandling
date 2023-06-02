package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.grunnlag.BarnepensjonBeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
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
import rapidsandrivers.withFeilhaandtering

internal class MigreringHendelser(rapidsConnection: RapidsConnection, private val beregningService: BeregningService) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(Migreringshendelser.BEREGN)
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, Migreringshendelser.BEREGN) {
                val behandlingId = packet.behandlingId
                logger.info("Mottatt beregnings-migreringshendelse for $BEHANDLING_ID_KEY $behandlingId")

                beregningService.opprettBeregningsgrunnlag(behandlingId, tilGrunnlagDTO(packet.hendelseData))

                runBlocking {
                    val beregning = beregningService.beregn(behandlingId).body<BeregningDTO>()
                    packet[BEREGNING_KEY] = beregning
                }
                packet.eventName = Migreringshendelser.VEDTAK
                context.publish(packet.toJson())
                logger.info("Publiserte oppdatert migreringshendelse fra beregning for behandling $behandlingId")
            }
        }
    }

    private fun tilGrunnlagDTO(request: MigreringRequest): BarnepensjonBeregningsGrunnlag =
        BarnepensjonBeregningsGrunnlag(
            soeskenMedIBeregning = listOf(
                GrunnlagMedPeriode(
                    fom = request.trygdetidsgrunnlag.fom,
                    tom = null,
                    data = request.persongalleri.soesken.map {
                        SoeskenMedIBeregning(foedselsnummer = Folkeregisteridentifikator.of(it), skalBrukes = true)
                    }
                )
            ),
            institusjonsoppholdBeregnignsGrunnlag = InstitusjonsoppholdBeregningsgrunnlag(false)
        )
}