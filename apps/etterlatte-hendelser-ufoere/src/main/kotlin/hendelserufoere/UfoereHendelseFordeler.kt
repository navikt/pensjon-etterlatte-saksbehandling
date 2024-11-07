package no.nav.etterlatte.hendelserufoere

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UfoereHendelseFordeler(
    private val kafkaProduser: KafkaProdusent<String, JsonMessage>,
) {
    private val logger: Logger = LoggerFactory.getLogger(UfoereHendelseFordeler::class.java)
    private val sikkerLogg: Logger = sikkerlogger()

    fun haandterHendelse(hendelse: UfoereHendelse) {
        try {
            val ageInYears = hendelse.alderVedVirkningstidspunkt / 12
            if (ageInYears in 18..21) {
                logger.info("Bruker er mellom 18 og 21 på på virkningstidspunktet. Sender ufoerehendelse til behandling?")
                // TODO: Gjør noe med denne hendelsen
            } else {
                sikkerLogg.info(
                    "Ufoerehendelse med personidentifikator=${hendelse.personidentifikator} " +
                        "og alderVedVirkningstidspunkt=${hendelse.alderVedVirkningstidspunkt} " +
                        "er ikke innenfor aldersgruppen 18-21. Hendelsen blir ikke sendt videre.",
                )
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse.hendelsestype, e)
        }
    }

    private fun loggFeilVedHaandteringAvHendelse(
        hendelsestype: String?,
        e: Exception,
    ) {
        logger.error(
            "Kunne ikke haandtere $hendelsestype. Dette skyldes sannsynligvis at " +
                "ufoerehendelsen ser annerledes ut enn forventet.",
            e,
        )
    }
}
