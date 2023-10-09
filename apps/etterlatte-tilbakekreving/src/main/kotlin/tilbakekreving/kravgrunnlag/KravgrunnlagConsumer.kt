package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.jms.ExceptionListener
import jakarta.jms.Message
import jakarta.jms.MessageListener
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toDetaljertKravgrunnlagDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KravgrunnlagConsumer(
    private val connectionFactory: EtterlatteJmsConnectionFactory,
    private val queue: String,
    private val kravgrunnlagService: KravgrunnlagService,
    private val hendelseRepository: TilbakekrevingHendelseRepository,
) : MessageListener {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sikkerLogg: Logger = sikkerlogger()

    fun start() =
        connectionFactory.start(
            listener = exceptionListener(),
            queue = queue,
            messageListener = this,
        ).also { logger.info("Lytter på kravgrunnlag fra tilbakekrevingskomponenten") }

    override fun onMessage(message: Message) =
        withLogContext {
            var kravgrunnlagPayload: String? = null
            try {
                logger.info("Kravgrunnlag (id=${message.jmsMessageID}) mottatt ${message.deliveryCount()} gang(er)")
                kravgrunnlagPayload = message.getBody(String::class.java)

                val detaljertKravgrunnlag = toDetaljertKravgrunnlagDto(kravgrunnlagPayload)
                hendelseRepository.lagreMottattKravgrunnlag(
                    detaljertKravgrunnlag.kravgrunnlagId.toString(),
                    detaljertKravgrunnlag.fagsystemId,
                    kravgrunnlagPayload,
                )

                kravgrunnlagService.opprettTilbakekreving(detaljertKravgrunnlag)
            } catch (t: Throwable) {
                logger.error("Feilet under mottak av kravgrunnlag (Sjekk sikkerlogg for payload", t)
                sikkerLogg.error("Feilet under mottak av kravgrunnlag: ${kv("kravgrunnlag", kravgrunnlagPayload)}", t)

                // Exception trigger retry - etter x forsøk vil meldingen legges på backout kø
                throw t
            }
        }

    private fun exceptionListener() =
        ExceptionListener {
            logger.error("En feil oppstod med tilkoblingen mot tilbakekrevingskomponenten: ${it.message}", it)
        }

    private fun Message.deliveryCount() = this.getLongProperty("JMSXDeliveryCount")
}
