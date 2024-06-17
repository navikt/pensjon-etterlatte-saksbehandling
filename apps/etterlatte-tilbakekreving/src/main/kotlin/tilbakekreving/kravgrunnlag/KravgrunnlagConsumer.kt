package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.jms.ExceptionListener
import jakarta.jms.Message
import jakarta.jms.MessageListener
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toDetaljertKravgrunnlagDto
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toKravOgVedtakstatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class KravgrunnlagConsumer(
    private val connectionFactory: EtterlatteJmsConnectionFactory,
    private val queue: String,
    private val kravgrunnlagService: KravgrunnlagService,
    private val hendelseRepository: TilbakekrevingHendelseRepository,
) : MessageListener {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sikkerLogg: Logger = sikkerlogger()

    fun start() =
        connectionFactory
            .start(
                listener = exceptionListener(),
                queue = queue,
                messageListener = this,
            ).also { logger.info("Lytter på kravgrunnlag fra tilbakekrevingskomponenten") }

    override fun onMessage(message: Message) =
        withLogContext {
            var payload: String? = null
            try {
                logger.info("Melding (id=${message.jmsMessageID}) mottatt ${message.deliveryCount()} gang(er)")
                payload = message.getBody(String::class.java)

                when {
                    payload.contains("detaljertKravgrunnlagMelding") -> {
                        logger.info("Mottok melding av type detaljertKravgrunnlagMelding")
                        val detaljertKravgrunnlag = toDetaljertKravgrunnlagDto(payload)

                        hendelseRepository.lagreTilbakekrevingHendelse(
                            sakId = detaljertKravgrunnlag.fagsystemId.toLong(),
                            payload = payload,
                            type = TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
                        )

                        val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlag)
                        kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)
                    }
                    payload.contains("endringKravOgVedtakstatus") -> {
                        logger.info("Mottok melding av type endringKravOgVedtakstatus")
                        val kravOgVedtakstatusDto = toKravOgVedtakstatus(payload)

                        hendelseRepository.lagreTilbakekrevingHendelse(
                            sakId = kravOgVedtakstatusDto.fagsystemId.toLong(),
                            payload = payload,
                            type = TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
                        )

                        val kravOgVedtakstatus = KravgrunnlagMapper.toKravOgVedtakstatus(kravOgVedtakstatusDto)
                        kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)
                    }

                    else -> throw Exception("Ukjent meldingstype, sjekk sikkerlogg og feilkø")
                }
            } catch (t: Throwable) {
                logger.error("Feilet under mottak av melding (Sjekk sikkerlogg for payload)", t)
                sikkerLogg.error("Feilet under mottak av melding ${kv("melding", payload)}", t)

                // Exception trigger retry - etter x forsøk vil meldingen legges på backout kø
                throw t
            }
        }

    private fun exceptionListener() =
        ExceptionListener {
            logger.error(
                "En feil oppstod med tilkoblingen mot tilbakekrevingskomponenten: ${it.message}. " +
                    "Restarter appen for å sette opp tilkobling på nytt",
                it,
            )

            // Trigger restart av appen for å sette opp tilkobling mot MQ på nytt
            exitProcess(-1)
        }

    private fun Message.deliveryCount() = this.getLongProperty("JMSXDeliveryCount")
}
