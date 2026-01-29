package no.nav.etterlatte.joarkhendelser

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingService
import no.nav.etterlatte.joarkhendelser.config.sikkerLogg
import no.nav.etterlatte.joarkhendelser.joark.Bruker
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.Error
import no.nav.etterlatte.joarkhendelser.joark.HendelseType
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.joarkhendelser.joark.Kanal
import no.nav.etterlatte.joarkhendelser.joark.SafKlient
import no.nav.etterlatte.joarkhendelser.joark.erGammeltTemaEtterlatte
import no.nav.etterlatte.joarkhendelser.joark.erNyttTemaEtterlatte
import no.nav.etterlatte.joarkhendelser.joark.lagMerknadFraStatus
import no.nav.etterlatte.joarkhendelser.joark.temaTilSakType
import no.nav.etterlatte.joarkhendelser.oppgave.OppgaveKlient
import no.nav.etterlatte.joarkhendelser.pdl.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.toJson
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class KjenteSkjemaKoder(
    val skjemaKode: String,
) {
    ANKE("NAV 90-00.08 A"),
    ETTERSENDELSE_ANKE("NAVe 90-00.08 A"),
    ETTERSENDELSE_KLAGE("NAVe 90-00.08 K"),
    ;

    val navn = name.lowercase().replace("_", " ")
}

/**
 * Enhet for Nav id og fordeling
 */
const val ENHET_NAV_ID_OG_FORDELING = "4833"

/**
 * Håndterer hendelser fra Joark og behandler de hendelsene som tilhører Team Etterlatte.
 *
 * Skal kun behandle:
 *  EYB: [SakType.BARNEPENSJON]
 *  EYO: [SakType.OMSTILLINGSSTOENAD]
 *
 * OBS! Joark sender kun hendelser for INNGÅENDE journalposter.
 *
 * @see: https://confluence.adeo.no/display/BOA/Joarkhendelser
 **/
class JoarkHendelseHandler(
    private val behandlingService: BehandlingService,
    private val safKlient: SafKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(JoarkHendelseHandler::class.java)

    private suspend fun hentJournalpost(journalpostId: Long): Journalpost {
        val response = safKlient.hentJournalpost(journalpostId)

        val journalpost =
            if (response.errors.isNullOrEmpty()) {
                response.data?.journalpost
            } else {
                throw mapError(response.errors, journalpostId)
            }

        if (journalpost == null) {
            throw NullPointerException("Fant ingen journalpost med id=$journalpostId")
        }
        return journalpost
    }

    private fun journalpostErFerdigstilt(journalpost: Journalpost): Boolean {
        if (journalpost.erFerdigstilt()) {
            logger.info(
                "Journalpost med id=${journalpost.journalpostId} er allerede ferdigstilt og tilknyttet sak (${journalpost.sak})",
            )
            return true
        } else {
            return false
        }
    }

    private fun bytterFraEtterlatteTemaTilNoeAnnet(hendelse: JournalfoeringHendelseRecord): Boolean =
        hendelse.erGammeltTemaEtterlatte() && !hendelse.erNyttTemaEtterlatte()

    suspend fun haandterHendelse(hendelse: JournalfoeringHendelseRecord) {
        val hendelseId = hendelse.hendelsesId
        val journalpostId = hendelse.journalpostId
        val temaNytt = hendelse.temaNytt
        val temaGammelt = hendelse.temaGammelt

        if (bytterFraEtterlatteTemaTilNoeAnnet(hendelse)) {
            hentJournalpost(journalpostId) // verifiser at den finnes
            logger.info(
                "Avbryter oppgaver for hendelse (id=$hendelseId, journalpostId=$journalpostId, tema=$temaNytt, temaGammelt=$temaGammelt, type=${hendelse.hendelsesType})",
            )

            behandlingService.avbrytOppgaverTilknyttetJournalpost(journalpostId)
            return
        } else if (!hendelse.erNyttTemaEtterlatte()) {
            logger.debug("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            return // Avbryter behandling
        }

        val journalpost = hentJournalpost(journalpostId)
        if (journalpostErFerdigstilt(journalpost)) {
            return
        }
        if (journalpost.journalfoerendeEnhet != null) {
            // Journalposten har allerede satt en journalførende enhet, og vi vil ikke plukke den opp hvis den skal
            // sendes rett til kabal (når det gjelder en anke på våre tema)
            logger.info(
                "Håndterer en journalpost som har satt journalførende enhet (id=${journalpost.journalpostId}, " +
                    "enhet=${journalpost.journalfoerendeEnhet}, hendelse=${hendelse.hendelsesType})",
            )
            val kjentSkjemakode =
                KjenteSkjemaKoder.entries.find { kjentSkjema -> kjentSkjema.skjemaKode in journalpost.dokumenter.map { it.brevkode } }
            if (kjentSkjemakode != null && journalpost.journalfoerendeEnhet == Enheter.KLAGE_VEST.enhetNr.enhetNr) {
                if (hendelse.hendelsesType == HendelseType.JOURNALPOST_MOTTATT) {
                    logger.info(
                        "Oppretter oppgave til Kabal for journalpost med id=$journalpostId, " +
                            "tema=$temaNytt siden den har satt journalførende enhet=${journalpost.journalfoerendeEnhet} og " +
                            "brevkoden brukt er $kjentSkjemakode",
                    )
                    oppgaveKlient.opprettOppgaveForKabal(journalpost, temaNytt, kjentSkjemakode)
                } else {
                    logger.info(
                        "Journalposthendelsen (${hendelse.hendelsesType}) er ikke mottatt, så vi oppretter " +
                            "ikke en oppgave til Kabal for journalpost $journalpostId, tema=$temaNytt, " +
                            "enhet=${journalpost.journalfoerendeEnhet}",
                    )
                }
                return
            } else {
                // team klage ruter journalposter som de ikke vil vi skal plukke opp ved å sette journalførende enhet
                // det er mulig at vi har ander tilfeller der noen prøver å rute en journalpost utenfor Gjenny
                // så legger på en error slik at vi kan fange det opp og se om vi må håndtere disse journalpostene
                // annerledes.
                logger.warn(
                    "Behandler en journalpost med id=$journalpostId og tema $temaNytt, " +
                        "som har journalførende enhet ${journalpost.journalfoerendeEnhet} satt. " +
                        "Brevkoden brukt er ikke en av de kjente kodene for anke / klager som skal til kabal, " +
                        "og det bør dobbletsjekkes om denne journalposten sendes rett.",
                )
            }
        }

        logger.info(
            "Starter behandling av hendelse (id=$hendelseId, journalpostId=$journalpostId, temaNytt=$temaNytt, temaGammelt=$temaGammelt)",
        )

        try {
            if (journalpost.bruker == null) {
                logger.warn("Bruker mangler på journalpost id=$journalpost")
                val tildeltEnhetsnr = journalpost.journalfoerendeEnhet?.takeIf { it == ENHET_NAV_ID_OG_FORDELING }
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, temaNytt, tildeltEnhetsnr)
                return
            }

            val ident = hentFolkeregisterIdent(journalpostId, journalpost.bruker)

            if (ident !is PdlIdentifikator.FolkeregisterIdent) {
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, temaNytt)
                return
            }

            val sakType = hendelse.temaTilSakType()

            when (val type = hendelse.hendelsesType) {
                HendelseType.JOURNALPOST_MOTTATT -> {
                    behandlingService.opprettOppgave(
                        ident.folkeregisterident.value,
                        sakType,
                        hendelse.lagMerknadFraStatus(journalpost.kanal),
                        journalpostId.toString(),
                    )
                }

                HendelseType.TEMA_ENDRET -> {
                    behandlingService.opprettOppgave(
                        ident.folkeregisterident.value,
                        sakType,
                        "Tema endret fra ${hendelse.temaGammelt} til $temaNytt",
                        journalpostId.toString(),
                    )
                }

                HendelseType.ENDELIG_JOURNALFOERT -> {
                    behandleEndeligJournalfoert(ident.folkeregisterident.value, sakType, journalpost)
                }

                HendelseType.JOURNALPOST_UTGAATT -> {
                    logger.info("Journalpost $journalpostId har status=${journalpost.journalstatus}")

                    behandlingService.avbrytOppgaverTilknyttetJournalpost(journalpostId)
                }

                else -> {
                    throw IllegalArgumentException("Journalpost=$journalpostId har ukjent hendelsesType=$type")
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved behandling av hendelse=$hendelseId (se sikkerlogg for mer info)", e)
            sikkerlogger().error("Feil oppsto ved behandling av journalpost: \n${journalpost.toJson()}: ")
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

    private suspend fun hentFolkeregisterIdent(
        journalpostId: Long,
        bruker: Bruker,
    ): PdlIdentifikator {
        if (bruker.type == BrukerIdType.ORGNR) {
            // TODO:
            //  Må vi lage støtte for ORGNR...?
            throw IllegalStateException("Journalpost med id=$journalpostId har brukerId av typen ${BrukerIdType.ORGNR}")
        }

        val pdlIdentifikator =
            pdlTjenesterKlient.hentPdlIdentifikator(bruker.id)
                ?: throw IllegalStateException(
                    "Ident tilknyttet journalpost=$journalpostId er null i PDL – avbryter behandling",
                )

        return pdlIdentifikator
    }

    private fun mapError(
        errors: List<Error>,
        journalpostId: Long,
    ): Exception {
        errors.forEach {
            if (errors.all { err -> err.extensions?.code == Error.Code.FORBIDDEN }) {
                logger.error("Deny for henting mot saf se sikkerlogg")
                sikkerLogg.error("Deny henting fra SAF for identifikator: $journalpostId. error: ${it.toJson()}")
            } else {
                logger.error("${errors.size} feil oppsto ved kall mot saf se sikkerlogg.")
                sikkerLogg.error("Feil mot saf, id: identifikator: $journalpostId. feil: ${it.toJson()}")
            }
        }

        val error = errors.firstOrNull()

        return RuntimeException("Fikk error fra Saf: ${error?.message}")
    }
}
