package no.nav.etterlatte.joarkhendelser

import isDev
import joarkhendelser.joark.SafKlient
import joarkhendelser.pdl.PdlTjenesterKlient
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingService
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.HendelseType
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.joarkhendelser.joark.Kanal
import no.nav.etterlatte.joarkhendelser.joark.erTemaEtterlatte
import no.nav.etterlatte.joarkhendelser.joark.lagMerknadFraStatus
import no.nav.etterlatte.joarkhendelser.joark.temaTilSakType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
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
    private val behandlingService: BehandlingService,
    private val safKlient: SafKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
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
            throw NullPointerException("Fant ingen journalpost med id=$journalpostId")
        } else if (journalpost.erFerdigstilt()) {
            logger.info(
                "Journalpost med id=$journalpostId er allerede ferdigstilt og tilknyttet sak (${journalpost.sak})",
            )
            return
        }

        try {
            if (journalpost.bruker == null) {
                // TODO:
                //  Burde vi lage oppgave på dette? Alt krever SakID, så hvordan skal det fungere hvis bruker mangler?

                if (isDev()) {
                    logger.error("Journalpost med id=$journalpostId mangler bruker!")
                    return // Ignorer hvis miljø er dev. Skal i teorien ikke være et problem i produksjon.
                } else {
                    throw IllegalStateException("Journalpost med id=$journalpostId mangler bruker!")
                }
            } else if (journalpost.bruker.type == BrukerIdType.ORGNR) {
                // TODO:
                //  Må vi lage støtte for ORGNR...?
                throw IllegalStateException("Journalpost med id=$journalpostId har brukerId av typen ${BrukerIdType.ORGNR}")
            }

            val ident =
                when (val pdlIdentifikator = pdlTjenesterKlient.hentPdlIdentifikator(journalpost.bruker.id)) {
                    is PdlIdentifikator.FolkeregisterIdent -> pdlIdentifikator.folkeregisterident.value
                    is PdlIdentifikator.Npid -> {
                        throw IllegalStateException("Bruker tilknyttet journalpost=$journalpostId har kun NPID!")
                    }

                    null -> throw IllegalStateException(
                        "Ident tilknyttet journalpost=$journalpostId er null i PDL – avbryter behandling",
                    )
                }

            when (val type = hendelse.hendelsesType) {
                HendelseType.JOURNALPOST_MOTTATT -> {
                    behandlingService.opprettOppgave(
                        ident,
                        sakType,
                        hendelse.lagMerknadFraStatus(journalpost.kanal),
                        hendelse.journalpostId.toString(),
                    )
                }

                HendelseType.TEMA_ENDRET -> {
                    behandlingService.opprettOppgave(
                        ident,
                        sakType,
                        "Tema endret fra ${hendelse.temaGammelt} til ${hendelse.temaNytt}",
                        hendelse.journalpostId.toString(),
                    )
                }

                HendelseType.ENDELIG_JOURNALFOERT ->
                    behandleEndeligJournalfoert(ident, sakType, journalpost)

                // TODO: Må avklare om dette er noe vi faktisk trenger å behandle
                HendelseType.JOURNALPOST_UTGAATT -> {
                    behandlingService.opprettOppgave(
                        ident,
                        sakType,
                        "Journalpost har utgått",
                        hendelse.journalpostId.toString(),
                    )
                }
                else -> throw IllegalArgumentException("Journalpost=$journalpostId har ukjent hendelsesType=$type")
            }
        } catch (e: Exception) {
            logger.error("Ukjent feil ved behandling av hendelse=${hendelse.hendelsesId}. Se sikkerlogg for mer detaljer.")
            sikkerlogger().error("Ukjent feil oppsto ved behandling av journalpost for bruker=${journalpost.bruker}: ", e)
            throw e
        }
    }

    /**
     * Journalposthendelse har status "EndeligJournalfoert"
     * Sjekker at journalposten er tilknyttet en sak i Gjenny
     **/
    private suspend fun behandleEndeligJournalfoert(
        ident: String,
        sakType: SakType,
        journalpost: Journalpost,
    ) {
        val sakId = behandlingService.hentSak(ident, sakType)

        if (journalpost.sak?.fagsakId == null || sakId == null) {
            logger.info("Journalpost ${journalpost.journalpostId} er ikke tilknyttet sak i Gjenny")

            val merknad =
                if (journalpost.kanal == Kanal.EESSI) {
                    journalpost.tittel ?: "Dokument fra EESSI/Rina er uten tittel"
                } else {
                    "Kontroller kobling til sak"
                }

            behandlingService.opprettOppgave(ident, sakType, merknad, journalpost.journalpostId)
        } else if (journalpost.sak.fagsakId == sakId.toString() && journalpost.sak.tema == sakType.tema) {
            logger.info(
                "Journalpost ${journalpost.journalpostId} er allerede tilknyttet" +
                    " eksisterende sak (id=$sakId, type=$sakType)",
            )
            return
        } else {
            logger.info("Uhåndtert tilstand av journalpost=${journalpost.journalpostId}")
        }
    }
}
