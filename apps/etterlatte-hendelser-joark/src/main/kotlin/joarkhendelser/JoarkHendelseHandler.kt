package no.nav.etterlatte.joarkhendelser

import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.joark.SafKlient
import joarkhendelser.pdl.PdlKlient
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
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

        if (!hendelseKanBehandles(hendelse)) return

        val sakType = hentSakTypeFraTema(hendelse.temaNytt)
        val journalpostId = hendelse.journalpostId

        val journalpost = safKlient.hentJournalpost(journalpostId).journalpost

        if (journalpost == null) {
            // TODO: Hva skal vi gjøre her...?
            logger.error("Fant ingen journalpost med id=$journalpostId")
            return
        } else if (journalpost.erFerdigstilt()) {
            // Hva gjør vi med ferdigstilte journalposter...?
            logger.error("Journalpost med id=${hendelse.journalpostId} er ferdigstilt")
            return
        }

        try {
            if (journalpost.bruker == null) {
                logger.error("Journalpost med id=$journalpostId mangler bruker!")
                return
            } else if (journalpost.bruker.type == BrukerIdType.ORGNR) {
                // TODO:
                //  Opprette oppgave til saksbehandler for å knytte til fnr og sak...?
                //  Må vi lage støtte for ORGNR...?
                logger.error("Journalpost med id=$journalpostId har brukerId av typen ${BrukerIdType.ORGNR}")
                return
            }

            val pdlIdentifikator =
                pdlKlient.hentPdlIdentifikator(journalpost.bruker.id)

            val ident =
                when (pdlIdentifikator) {
                    is PdlIdentifikator.FolkeregisterIdent -> pdlIdentifikator.folkeregisterident.value
                    is PdlIdentifikator.Npid -> {
                        logger.error("Ignorerer journalføringshendelse med NPID=${pdlIdentifikator.npid.ident}")
                        return
                    }

                    null -> {
                        logger.error("Ident tilknyttet journalpost=$journalpostId er null i PDL – avbryter behandling")
                        return
                    }
                }

            val gradering = pdlKlient.hentAdressebeskyttelse(ident)
            logger.info("Bruker=${ident.maskerFnr()} har gradering $gradering")

            logger.info("Oppretter ny ${sakType.name.lowercase()} for bruker=${ident.maskerFnr()} med gradering=$gradering")
            val sakId = behandlingKlient.hentEllerOpprettSak(ident, sakType, gradering)

            logger.info("Oppretter journalføringsoppgave for sak=$sakId")
            val oppgaveId =
                behandlingKlient.opprettOppgave(sakId, hendelse.journalpostStatusReadable(), journalpostId.toString())

            logger.info("Opprettet oppgave (id=$oppgaveId) med sakId=$sakId")
        } catch (e: Exception) {
            // TODO: Fjerne fnr logging før prodsetting
            logger.error("Ukjent feil oppsto ved behandling av journalpost for bruker=${journalpost.bruker}: ", e)
            throw e
        }
    }

    private fun hentSakTypeFraTema(tema: String): SakType =
        when (tema) {
            "EYO" -> SakType.OMSTILLINGSSTOENAD
            "EYB" -> SakType.BARNEPENSJON
            else -> throw IllegalArgumentException("Ugyldig tema $tema")
        }

    private fun hendelseKanBehandles(hendelse: JournalfoeringHendelseRecord): Boolean {
        return if (!hendelse.erTemaEtterlatte()) {
            logger.info("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            false
        } else if (hendelse.hendelsesType != "JournalpostMottatt") {
            logger.warn(
                "Hendelse (id=${hendelse.hendelsesId}) har hendelsestype=${hendelse.hendelsesType} og håndteres ikke",
            )
            false
        } else {
            logger.info("Starter behandling av hendelse (id=${hendelse.hendelsesId}) med tema ${hendelse.temaNytt}")
            true
        }
    }
}

private fun JournalfoeringHendelseRecord.erTemaEtterlatte(): Boolean =
    temaNytt == SakType.BARNEPENSJON.tema ||
        temaNytt == SakType.OMSTILLINGSSTOENAD.tema

private fun JournalfoeringHendelseRecord.journalpostStatusReadable(): String =
    when (journalpostStatus) {
        "MOTTATT" -> "Mottatt"
        "JOURNALFOERT" -> "Ferdigstilt"
        "UKJENT_BRUKER" -> "Ukjent bruker"
        "UTGAAR" -> "Feil ifm. mottak eller journalføring"
        "OPPLASTING_DOKUMENT" -> throw IllegalArgumentException("Status $journalpostStatus tilhører dagpenger!")
        else -> throw IllegalArgumentException("Ukjent journalpostStatus $journalpostStatus")
    }
