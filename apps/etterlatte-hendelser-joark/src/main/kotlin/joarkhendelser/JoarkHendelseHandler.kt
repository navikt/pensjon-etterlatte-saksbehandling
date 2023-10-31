package no.nav.etterlatte.joarkhendelser

import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.joark.SafKlient
import joarkhendelser.pdl.PdlKlient
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JoarkHendelseHandler(
    private val behandlingKlient: BehandlingKlient,
    private val safKlient: SafKlient,
    private val pdlKlient: PdlKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(JoarkHendelseHandler::class.java)

    suspend fun haandterHendelse(record: ConsumerRecord<String, JournalfoeringHendelseRecord>) {
        val hendelse = record.value()

        if (!hendelse.erTemaEtterlatte()) {
            logger.info("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            return
        } else if (hendelse.erFerdigstilt()) {
            // Hva gjør vi med ferdigstilte journalposter...?
            logger.error("Journalpost med id=${hendelse.journalpostId} er ferdigstilt")
            return
        }

        logger.info("Starter behandling av hendelse (id=${hendelse.hendelsesId}) med tema ${hendelse.temaNytt}")

        val sakType = hentSakTypeFraTema(hendelse.temaNytt)

        try {
            val journalpostId = hendelse.journalpostId

            val journalpost = safKlient.hentJournalpost(journalpostId).journalpost

            if (journalpost == null) {
                // TODO: Hva skal vi gjøre her...?
                logger.error("Fant ingen journalpost med id=$journalpostId")
                return
            }

            val ident =
                when (journalpost.bruker?.type) {
                    BrukerIdType.FNR -> journalpost.bruker.id
                    BrukerIdType.ORGNR -> {
                        // Opprette oppgave til saksbehandler for å knytte til fnr og sak
                        logger.error("Kan ikke behandle brukerId av type ${BrukerIdType.ORGNR}")
                        return // TODO... Kaste exception...?
                    }

                    BrukerIdType.AKTOERID -> {
                        // Opprette oppgave til saksbehandler for å knytte til fnr og sak
                        logger.error("Kan ikke behandle brukerId av type ${BrukerIdType.AKTOERID}")
                        return
                    }

                    else -> throw NullPointerException("Bruker er NULL på journalpost=$journalpostId")
                }

            if (pdlKlient.hentPdlIdentifikator(ident) == null) {
                logger.info("Ident=${ident.maskerFnr()} er null i PDL – avbryter behandling")
                return
            }

            val gradering = pdlKlient.hentAdressebeskyttelse(ident)
            logger.info("Bruker=${ident.maskerFnr()} har gradering $gradering")

            val sakId = behandlingKlient.hentEllerOpprettSak(ident, sakType, gradering)

            logger.info("Oppretter journalføringsoppgave for sak=$sakId")
            val oppgaveId = behandlingKlient.opprettOppgave(sakId)

            logger.info("Opprettet oppgave (id=$oppgaveId) med sakId=$sakId")
        } catch (e: Exception) {
            logger.error("Ukjent feil oppsto: ", e)
            throw e
        }
    }

    private fun hentSakTypeFraTema(tema: String): SakType =
        when (tema) {
            "EYO" -> SakType.OMSTILLINGSSTOENAD
            "EYB" -> SakType.BARNEPENSJON
            else -> throw IllegalArgumentException("Ugyldig tema $tema")
        }
}

private fun JournalfoeringHendelseRecord.erTemaEtterlatte(): Boolean =
    temaNytt == SakType.BARNEPENSJON.tema ||
        temaNytt == SakType.OMSTILLINGSSTOENAD.tema

private fun JournalfoeringHendelseRecord.erFerdigstilt(): Boolean = journalpostStatus == "FERDIGSTILT"
