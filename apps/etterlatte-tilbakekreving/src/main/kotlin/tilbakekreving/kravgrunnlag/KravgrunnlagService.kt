package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import org.slf4j.LoggerFactory

class KravgrunnlagService(private val behandlingKlient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun haandterKravgrunnlag(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info("Håndterer kravgrunnlag ${kravgrunnlag.kravgrunnlagId} med status ${kravgrunnlag.status}")

            when (kravgrunnlag.status) {
                KravgrunnlagStatus.NY -> opprettTilbakekreving(kravgrunnlag)
                KravgrunnlagStatus.SPER -> settTilbakekrevingPaaVent(kravgrunnlag)
                KravgrunnlagStatus.ENDR -> endreTilbakekreving(kravgrunnlag)
                KravgrunnlagStatus.AVSL -> avsluttTilbakekreving(kravgrunnlag)
                else -> {
                    throw Error(
                        "Kravgrunnlag hadde status ${kravgrunnlag.status}, denne statusen er ikke støttet og " +
                            "må håndteres manuelt. Kravgrunnlaget er lagret i databasen.",
                    )
                }
            }
        }
    }

    private fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info("Oppretter tilbakekreving fra kravgrunnlag ${kravgrunnlag.kravgrunnlagId}")
            behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
        }
    }

    private fun settTilbakekrevingPaaVent(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info("Setter tilbakekreving på vent pga endret status i kravgrunnlag ${kravgrunnlag.kravgrunnlagId}")
            behandlingKlient.endreOppgaveStatusForTilbakekreving(kravgrunnlag.sakId, paaVent = true)
        }
    }

    private fun endreTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            if (kravgrunnlag.perioder.isEmpty()) {
                logger.info("Kravgrunnlag er ikke endret - setter oppgave tilbake dersom den har status \"På vent\"")
                behandlingKlient.endreOppgaveStatusForTilbakekreving(kravgrunnlag.sakId, paaVent = false)
            } else {
                logger.info("Kravgrunnlag er endret - avbryter eksisterende tilbakekreving og oppretter ny")
                behandlingKlient.avbrytTilbakekreving(kravgrunnlag.sakId)
                behandlingKlient.opprettTilbakekreving(kravgrunnlag.sakId, kravgrunnlag)
            }
        }
    }

    private fun avsluttTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        runBlocking {
            logger.info("Kravgrunnlag har blitt nullet ut og er ikke lenger aktuelt - avbryter tilbakekreving")
            behandlingKlient.avbrytTilbakekreving(kravgrunnlag.sakId)
        }
    }
}
