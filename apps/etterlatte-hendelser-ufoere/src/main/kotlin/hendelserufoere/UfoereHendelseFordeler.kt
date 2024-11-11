package no.nav.etterlatte.hendelserufoere

import no.nav.etterlatte.BehandlingKlient
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UfoereHendelseFordeler(
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(UfoereHendelseFordeler::class.java)

    suspend fun haandterHendelse(hendelse: UfoereHendelse) {
        try {
            val ageInYears = hendelse.alderVedVirkningstidspunkt / 12
            if (ageInYears in 18..21) {
                logger.info("Bruker er mellom 18 og 21 på på virkningstidspunktet. Sender ufoerehendelse til behandling?")
                behandlingKlient.postTilBehandling(
                    ufoereHendelse =
                        UfoeretrygdHendelse(
                            hendelseId = 123L,
                            ident = hendelse.personidentifikator,
                        ),
                )
            } else {
                logger.info(
                    "Ufoerehendelse med personidentifikator=${hendelse.personidentifikator.maskerFnr()} " +
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
