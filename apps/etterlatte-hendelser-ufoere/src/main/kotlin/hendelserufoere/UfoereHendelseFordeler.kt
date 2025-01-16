package no.nav.etterlatte.hendelserufoere

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

class UfoereHendelseFordeler(
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(UfoereHendelseFordeler::class.java)

    suspend fun haandterHendelse(hendelse: UfoereHendelse) {
        try {
            logger.info("Mottok hendelse for vedtak av type ${hendelse.vedtaksType} for bruker ${hendelse.personIdent.maskerFnr()}")
            sikkerlogger().info("Mottok hendelse for vedtak av type ${hendelse.vedtaksType} for bruker ${hendelse.personIdent}")

            val alderIMaanederVedVirkningstidspunkt = ChronoUnit.MONTHS.between(hendelse.fodselsdato, hendelse.virkningsdato)
            val alderIAarVedVirkningstidspunkt = ChronoUnit.YEARS.between(hendelse.fodselsdato, hendelse.virkningsdato)

            // Skal inkludere personer fra og med måneden de fyller 18 år og til og med måneden de fyller 21 år
            if (alderIMaanederVedVirkningstidspunkt in ATTEN_AAR_I_MAANEDER..TJUEEN_AAR_I_MAANEDER) {
                logger.info(
                    "Bruker er mellom 18 og 21 år på virkningstidspunktet (alderIAarVedVirkningstidspunkt = " +
                        "$alderIAarVedVirkningstidspunkt, alderIMaanederVedVirkningstidspunkt = " +
                        "$alderIMaanederVedVirkningstidspunkt). Hendelse sendes videre til behandling for sjekk mot " +
                        "løpende barnepensjonssaker.",
                )
                behandlingKlient.postTilBehandling(hendelse)
            } else {
                logger.info(
                    "Hendelse ignoreres da bruker ikke er innenfor intervallet som kvalifiserer for barnepensjon " +
                        "(alderIAarVedVirkningstidspunkt = $alderIAarVedVirkningstidspunkt, " +
                        "alderIMaanederVedVirkningstidspunkt = $alderIMaanederVedVirkningstidspunkt).",
                )
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse, e)
            throw e
        }
    }

    private fun loggFeilVedHaandteringAvHendelse(
        hendelse: UfoereHendelse,
        e: Exception,
    ) {
        logger.error(
            "Kunne ikke håndtere hendelse for ${hendelse.personIdent.maskerFnr()}. " +
                "Dette skyldes sannsynligvis at hendelsen ser annerledes ut enn forventet. Se sikkerlogg for detaljer.",
            e,
        )
        sikkerlogger().error(
            "Kunne ikke håndtere hendelse for ${hendelse.personIdent}. Dette skyldes sannsynligvis at " +
                "hendelsen ser annerledes ut enn forventet: ${hendelse.toJson()}",
            e,
        )
    }

    private companion object {
        private const val ATTEN_AAR_I_MAANEDER = 12 * 18
        private const val TJUEEN_AAR_I_MAANEDER = 12 * 21
    }
}
