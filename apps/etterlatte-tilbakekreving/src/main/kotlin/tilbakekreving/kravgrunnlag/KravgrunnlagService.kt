package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.tilbakekreving.KravOgVedtakstatus
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import org.slf4j.LoggerFactory

class KanIkkeHaandtereStatusException(
    message: String,
) : Exception(message)

class KravgrunnlagService(
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun haandterKravgrunnlag(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info("Håndterer kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value} med status ${kravgrunnlag.status}")

            when (kravgrunnlag.status) {
                KravgrunnlagStatus.NY -> opprettTilbakekreving(kravgrunnlag)
                KravgrunnlagStatus.ENDR -> endreTilbakekreving(kravgrunnlag)
                else -> {
                    throw KanIkkeHaandtereStatusException(
                        "Kravgrunnlag for sak ${kravgrunnlag.sakId.value} hadde status ${kravgrunnlag.status}, " +
                            "denne statusen er ikke støttet og må håndteres manuelt. Kravgrunnlaget " +
                            "er lagret i databasen.",
                    )
                }
            }
        }
    }

    fun haandterKravOgVedtakStatus(kravOgVedtakstatus: KravOgVedtakstatus) {
        runBlocking {
            logger.info("Håndterer krav og vedtaksstatus med status ${kravOgVedtakstatus.status}")

            when (kravOgVedtakstatus.status) {
                KravgrunnlagStatus.SPER -> settTilbakekrevingPaaVent(kravOgVedtakstatus)
                KravgrunnlagStatus.ENDR -> endreTilbakekreving(kravOgVedtakstatus)
                KravgrunnlagStatus.AVSL -> avsluttTilbakekreving(kravOgVedtakstatus)
                else -> {
                    throw KanIkkeHaandtereStatusException(
                        "KravOgVedtakstatus for sak ${kravOgVedtakstatus.sakId.value} hadde status " +
                            "${kravOgVedtakstatus.status}, denne statusen er ikke støttet og må håndteres " +
                            "manuelt. Payload er lagret i databasen.",
                    )
                }
            }
        }
    }

    private fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info(
                "Oppretter tilbakekreving i sak ${kravgrunnlag.sakId.value} fra " +
                    "kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value}",
            )
            behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
        }
    }

    private fun settTilbakekrevingPaaVent(kravOgVedtakstatus: KravOgVedtakstatus) {
        runBlocking {
            logger.info(
                "Setter tilbakekreving på vent pga endret status i kravgrunnlag " +
                    "for sak ${kravOgVedtakstatus.sakId.value}",
            )
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravOgVedtakstatus.sakId, paaVent = true)
        }
    }

    private fun endreTilbakekreving(kravOgVedtakstatus: KravOgVedtakstatus) {
        runBlocking {
            logger.info(
                "Kravgrunnlag er ikke endret i sak ${kravOgVedtakstatus.sakId.value} " +
                    "- setter oppgave tilbake dersom den har status \"På vent\"",
            )
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravOgVedtakstatus.sakId, paaVent = false)
        }
    }

    private fun endreTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            if (kravgrunnlag.perioder.isEmpty()) {
                // TODO Undersøke om dette faktisk kan skje eller om den er dekket av kravogvedtakstatus endring
                logger.info(
                    "Kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value} er ikke endret i sak ${kravgrunnlag.sakId.value}" +
                        " - setter oppgave tilbake dersom den har status \"På vent\"",
                )
                behandlingKlient.endreOppgaveStatusForTilbakekreving(kravgrunnlag.sakId, paaVent = false)
            } else {
                logger.info(
                    "Kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value} er endret i sak ${kravgrunnlag.sakId.value}" +
                        " avbryter eksisterende tilbakekreving og oppretter ny",
                )
                behandlingKlient.avbrytTilbakekreving(kravgrunnlag.sakId)
                behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
            }
        }
    }

    private fun avsluttTilbakekreving(kravOgVedtakstatus: KravOgVedtakstatus) {
        runBlocking {
            logger.info(
                "Kravgrunnlag for sak ${kravOgVedtakstatus.sakId.value} har blitt nullet ut og er ikke lenger aktuelt - " +
                    "avbryter tilbakekreving",
            )
            behandlingKlient.avbrytTilbakekreving(kravOgVedtakstatus.sakId)
        }
    }
}
