package no.nav.etterlatte.joarkhendelser

import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.joark.SafKlient
import joarkhendelser.pdl.PdlKlient
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.erTemaEtterlatte
import no.nav.etterlatte.joarkhendelser.joark.lagMerknadFraStatus
import no.nav.etterlatte.joarkhendelser.joark.temaTilSakType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Håndterer hendelser fra Joark og behandler de hendelsene som tilhører Team Etterlatte.
 *
 * Skal kun behandle:
 *  EYB: [SakType.BARNEPENSJON]
 *  EYO: [SakType.OMSTILLINGSSTOENAD]
 *
 * @see: https://confluence.adeo.no/display/BOA/Joarkhendelser
 **/
class JoarkHendelseHandler(
    private val behandlingKlient: BehandlingKlient,
    private val safKlient: SafKlient,
    private val pdlKlient: PdlKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(JoarkHendelseHandler::class.java)

    /* TODO:
     *   Etterlatte-søknadene sin journalføringsriver vil bli problematisk for denne consumeren siden enkelte søknader
     *   IKKE ferdigstilles. Må trolig skrive om en del der eller finne på noe lurt for å ikke lage dobbelt opp med
     *   oppgaver til saksbehandler.
     */
    suspend fun haandterHendelse(hendelse: JournalfoeringHendelseRecord) {
        if (!hendelse.erTemaEtterlatte()) {
            logger.info("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            return // Avbryter behandling
        }

        logger.info("Starter behandling av hendelse (id=${hendelse.hendelsesId}) med tema ${hendelse.temaNytt}")

        val sakType = hendelse.temaTilSakType()
        val journalpostId = hendelse.journalpostId

        val journalpost = safKlient.hentJournalpost(journalpostId).journalpost

        if (journalpost == null) {
            // TODO: Hva skal vi gjøre her...?
            logger.error("Fant ingen journalpost med id=$journalpostId")
            return
        } else if (journalpost.erFerdigstilt()) {
            // Hva gjør vi med ferdigstilte journalposter...?
            logger.error("Journalpost med id=$journalpostId er ferdigstilt")
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

            when (val type = hendelse.hendelsesType) {
                "JournalpostMottatt" -> behandleJournalpostMottatt(ident, sakType, hendelse)
                "TemaEndret" -> TODO("Mangler støtte for å behandle journalpost=$journalpostId hendelsesType=$type")
                "EndeligJournalført" -> TODO("Mangler støtte for å behandle journalpost=$journalpostId hendelsesType=$type")
                "JournalpostUtgått" -> TODO("Mangler støtte for å behandle journalpost=$journalpostId hendelsesType=$type")
                else -> throw IllegalArgumentException("Journalpost=$journalpostId har ukjent hendelsesType=$type")
            }
        } catch (e: Exception) {
            // TODO: Fjerne fnr logging før prodsetting
            logger.error("Ukjent feil oppsto ved behandling av journalpost for bruker=${journalpost.bruker}: ", e)
            throw e
        }
    }

    private suspend fun behandleJournalpostMottatt(
        ident: String,
        sakType: SakType,
        hendelse: JournalfoeringHendelseRecord,
    ) {
        val gradering = pdlKlient.hentAdressebeskyttelse(ident)
        logger.info("Bruker=${ident.maskerFnr()} har gradering $gradering")

        logger.info("Oppretter ny ${sakType.name.lowercase()} for bruker=${ident.maskerFnr()} med gradering=$gradering")
        val sakId = behandlingKlient.hentEllerOpprettSak(ident, sakType, gradering)

        logger.info("Oppretter journalføringsoppgave for sak=$sakId")
        val oppgaveId =
            behandlingKlient.opprettOppgave(sakId, hendelse.lagMerknadFraStatus(), hendelse.journalpostId.toString())

        logger.info("Opprettet oppgave (id=$oppgaveId) med sakId=$sakId")
    }
}
