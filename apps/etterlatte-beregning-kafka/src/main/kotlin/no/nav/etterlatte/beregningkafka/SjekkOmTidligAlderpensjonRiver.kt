package no.nav.etterlatte.beregningkafka

import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

class SjekkOmTidligAlderpensjonRiver(
    rapidsConnection: RapidsConnection,
    private val beregningService: BeregningService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun kontekst() = Kontekst.INNTEKTSJUSTERING

    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey("vedtak.behandlingId") }
            validate { it.requireKey("vedtak.sak") }
            validate { it.requireKey("vedtak.sak.id") }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Mottatt sjekk om tidlig alderpensjon for ${packet["vedtak.behandlingId"].asText()}")

        // TODO: try/catch?
        runBlocking {
            sjekkOmTidligAlderpensjon(packet["vedtak.behandlingId"].asUUID())
        }
    }

    internal suspend fun sjekkOmTidligAlderpensjon(behandlingId: UUID) {
        // TODO: hent behandling, virkningstidspunkt

        val avkorting =
            beregningService
                .hentAvkorting(behandlingId)
                .takeIf { it.status == HttpStatusCode.OK }
                ?.body<AvkortingDto>()
                ?: throw IllegalStateException("Kunne ikke hente Avkorting for behandling $behandlingId")

        // TODO: sjekk om tidlig alderspensjon
        // TODO: opprett generell_oppgave med merknad og frist
        // TODO: ????
    }
}
