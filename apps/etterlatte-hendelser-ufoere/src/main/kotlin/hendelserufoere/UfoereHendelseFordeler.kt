package no.nav.etterlatte.hendelserufoere

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class UfoereHendelseFordeler(
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(UfoereHendelseFordeler::class.java)

    suspend fun haandterHendelse(hendelse: UfoereHendelse) {
        try {
            val attenAarIMaaneder = 12 * 18
            val tjueenAarIMaaneder = 12 * 21

            val datoVedFoedsel = LocalDate.parse(hendelse.fodselsdato)
            val datoVedVirkningstidspunkt = LocalDate.parse(hendelse.virkningsdato)

            val alderVedVirkningstidspunkt = ChronoUnit.MONTHS.between(datoVedFoedsel, datoVedVirkningstidspunkt)

            if (alderVedVirkningstidspunkt in attenAarIMaaneder..tjueenAarIMaaneder) {
                logger.info("Bruker er mellom 18 og 21 på på virkningstidspunktet. Sender ufoerehendelse til behandling")
                behandlingKlient.postTilBehandling(
                    ufoereHendelse = hendelse,
                )
            } else {
                logger.info(
                    "Ufoerehendelse med personidentifikator=${hendelse.personIdent.maskerFnr()} " +
                        "og alderVedVirkningstidspunkt=$alderVedVirkningstidspunkt " +
                        "er ikke innenfor aldersgruppen 18-21. Hendelsen blir ikke sendt videre.",
                )
            }
        } catch (e: Exception) {
            loggFeilVedHaandteringAvHendelse(hendelse.vedtaksType, e)
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
