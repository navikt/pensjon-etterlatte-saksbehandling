package no.nav.etterlatte.joarkhendelser

import no.nav.etterlatte.joarkhendelser.behandling.BehandlingService
import no.nav.etterlatte.joarkhendelser.joark.Bruker
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.Error
import no.nav.etterlatte.joarkhendelser.joark.HendelseType
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.joarkhendelser.joark.Kanal
import no.nav.etterlatte.joarkhendelser.joark.SafKlient
import no.nav.etterlatte.joarkhendelser.joark.erTemaEtterlatte
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

    suspend fun haandterHendelse(hendelse: JournalfoeringHendelseRecord) {
        val hendelseId = hendelse.hendelsesId
        val journalpostId = hendelse.journalpostId
        val temaNytt = hendelse.temaNytt

        if (!hendelse.erTemaEtterlatte()) {
            logger.debug("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            return // Avbryter behandling
        }

        logger.info(
            "Starter behandling av hendelse (id=$hendelseId, journalpostId=$journalpostId, tema=$temaNytt)",
        )

        val response = safKlient.hentJournalpost(journalpostId)

        val journalpost =
            if (response.errors.isNullOrEmpty()) {
                response.data?.journalpost
            } else {
                throw mapError(response.errors)
            }

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
                logger.warn("Bruker mangler på journalpost id=$journalpost")
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, temaNytt)
                return
            }

            val ident = hentFolkeregisterIdent(journalpostId, journalpost.bruker)

            val sakType = hendelse.temaTilSakType()

            when (val type = hendelse.hendelsesType) {
                HendelseType.JOURNALPOST_MOTTATT -> {
                    behandlingService.opprettOppgave(
                        ident,
                        sakType,
                        hendelse.lagMerknadFraStatus(journalpost.kanal),
                        journalpostId.toString(),
                    )
                }

                HendelseType.TEMA_ENDRET -> {
                    behandlingService.opprettOppgave(
                        ident,
                        sakType,
                        "Tema endret fra ${hendelse.temaGammelt} til $temaNytt",
                        journalpostId.toString(),
                    )
                }

                HendelseType.ENDELIG_JOURNALFOERT ->
                    behandleEndeligJournalfoert(ident, sakType, journalpost)

                // TODO: Må avklare om dette er noe vi faktisk trenger å behandle
                HendelseType.JOURNALPOST_UTGAATT -> {
                    logger.info("Journalpost $journalpostId har status=${journalpost.journalstatus}")

                    behandlingService.avbrytOppgaverTilknyttetJournalpost(journalpostId)
                }

                else -> throw IllegalArgumentException("Journalpost=$journalpostId har ukjent hendelsesType=$type")
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
    ): String {
        if (bruker.type == BrukerIdType.ORGNR) {
            // TODO:
            //  Må vi lage støtte for ORGNR...?
            throw IllegalStateException("Journalpost med id=$journalpostId har brukerId av typen ${BrukerIdType.ORGNR}")
        }

        return when (val pdlIdentifikator = pdlTjenesterKlient.hentPdlIdentifikator(bruker.id)) {
            is PdlIdentifikator.FolkeregisterIdent -> pdlIdentifikator.folkeregisterident.value
            is PdlIdentifikator.Npid -> {
                throw IllegalStateException("Bruker tilknyttet journalpost=$journalpostId har kun NPID!")
            }

            null -> throw IllegalStateException(
                "Ident tilknyttet journalpost=$journalpostId er null i PDL – avbryter behandling",
            )
        }
    }

    private fun mapError(errors: List<Error>): Exception {
        errors.forEach {
            if (errors.all { err -> err.extensions?.code == Error.Code.FORBIDDEN }) {
                logger.warn("${errors.size} feil oppsto ved kall mot saf, alle var tilgangssjekk: ${it.toJson()}")
            } else {
                logger.error("${errors.size} feil oppsto ved kall mot saf: ${it.toJson()}")
            }
        }

        val error = errors.firstOrNull()

        return RuntimeException("Fikk error fra Saf: ${error?.message}")
    }
}
