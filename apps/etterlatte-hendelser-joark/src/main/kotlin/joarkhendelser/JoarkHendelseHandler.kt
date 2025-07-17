package no.nav.etterlatte.joarkhendelser

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
        logger.info(
            "Starter behandling av hendelse (id=$hendelseId, journalpostId=$journalpostId, temaNytt=$temaNytt, temaGammelt=$temaGammelt)",
        )

        try {
            if (journalpost.bruker == null) {
                logger.warn("Bruker mangler på journalpost id=$journalpost")
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, temaNytt)
                return
            }

            val ident = hentFolkeregisterIdent(journalpostId, journalpost.bruker)

            val sakType = hendelse.temaTilSakType()

            when (ident) {
                is PdlIdentifikator.FolkeregisterIdent -> {
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

                        HendelseType.ENDELIG_JOURNALFOERT ->
                            behandleEndeligJournalfoert(ident.folkeregisterident.value, sakType, journalpost)

                        HendelseType.JOURNALPOST_UTGAATT -> {
                            logger.info("Journalpost $journalpostId har status=${journalpost.journalstatus}")

                            behandlingService.avbrytOppgaverTilknyttetJournalpost(journalpostId)
                        }

                        else -> throw IllegalArgumentException("Journalpost=$journalpostId har ukjent hendelsesType=$type")
                    }
                }
                is PdlIdentifikator.Npid -> {
                    oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, hendelse.temaTilSakType().tema)
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

        return when (val pdlIdentifikator = pdlTjenesterKlient.hentPdlIdentifikator(bruker.id)) {
            is PdlIdentifikator.FolkeregisterIdent -> pdlIdentifikator
            is PdlIdentifikator.Npid -> pdlIdentifikator

            null -> throw IllegalStateException(
                "Ident tilknyttet journalpost=$journalpostId er null i PDL – avbryter behandling",
            )
        }
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
