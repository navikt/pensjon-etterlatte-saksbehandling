package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import jakarta.jms.ExceptionListener
import jakarta.jms.Message
import jakarta.jms.MessageListener
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mq.EtterlatteJmsConnectionFactory
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseRepository
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseStatus
import no.nav.etterlatte.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toDetaljertKravgrunnlagDto
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagJaxb.toKravOgVedtakstatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
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
                val jmsTimestamp = Tidspunkt(Instant.ofEpochMilli(message.jmsTimestamp))

                when {
                    payload.contains("detaljertKravgrunnlagMelding") -> {
                        logger.info("Mottok melding av type detaljertKravgrunnlagMelding")
                        val detaljertKravgrunnlag = toDetaljertKravgrunnlagDto(payload)
                        val sakId = detaljertKravgrunnlag.fagsystemId.toLong()

                        sjekkAtSisteHendelseForSakErFerdigstilt(sakId, jmsTimestamp)

                        val hendelseId =
                            hendelseRepository.lagreTilbakekrevingHendelse(
                                sakId = sakId,
                                payload = payload,
                                type = TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT,
                                jmsTimestamp = jmsTimestamp,
                            )

                        val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlag)
                        kravgrunnlagService.haandterKravgrunnlag(kravgrunnlag)
                        hendelseRepository.ferdigstillTilbakekrevingHendelse(sakId, hendelseId)
                    }
                    payload.contains("endringKravOgVedtakstatus") -> {
                        logger.info("Mottok melding av type endringKravOgVedtakstatus")
                        val kravOgVedtakstatusDto = toKravOgVedtakstatus(payload)
                        val sakId = kravOgVedtakstatusDto.fagsystemId.toLong()

                        sjekkAtSisteHendelseForSakErFerdigstilt(sakId, jmsTimestamp)

                        val hendelseId =
                            hendelseRepository.lagreTilbakekrevingHendelse(
                                sakId = sakId,
                                payload = payload,
                                type = TilbakekrevingHendelseType.KRAV_VEDTAK_STATUS_MOTTATT,
                                jmsTimestamp = jmsTimestamp,
                            )

                        val kravOgVedtakstatus = KravgrunnlagMapper.toKravOgVedtakstatus(kravOgVedtakstatusDto)
                        kravgrunnlagService.haandterKravOgVedtakStatus(kravOgVedtakstatus)
                        hendelseRepository.ferdigstillTilbakekrevingHendelse(sakId, hendelseId)
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

    private fun sjekkAtSisteHendelseForSakErFerdigstilt(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        jmsTimestampNyHendelse: Tidspunkt,
    ) {
        val sisteHendelse = hendelseRepository.hentSisteTilbakekrevingHendelse(sakId)
        if (sisteHendelse?.status == TilbakekrevingHendelseStatus.NY) {
            throw Exception("Må ferdigstille forrige hendelse ${sisteHendelse.id} for sak $sakId før ny kan prosesseres")
        }
        if (sisteHendelse?.jmsTimestamp?.isAfter(jmsTimestampNyHendelse) == true) {
            throw Exception("Hendelser har blitt behandlet i feil rekkefølge for $sakId - dette må undersøkes videre")
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
