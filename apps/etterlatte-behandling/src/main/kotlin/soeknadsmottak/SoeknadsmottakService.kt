package no.nav.etterlatte.soeknadsmottak

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstResponse
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringResponse
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class MotstridendeOverlevering(
    mottaksnoekkel: String,
) : ForespoerselException(
        status = 409,
        code = "SOEKNAD_OVERLEVERING_MOTSTRIDENDE",
        detail = "Søknaden $mottaksnoekkel er allerede overlevert med annet innhold",
    )

/**
 * Gjennys del av søknadsmottaket når innsendt-soeknad journalfører selv (B12).
 * Reglene er de samme som i NySoeknadRiver (B4): finn eller opprett OMS-sak, journalfør på ukjent bruker ved
 * ugyldig fnr, og opprett en TILLEGGSINFORMASJON-oppgave uten behandling.
 */
class SoeknadsmottakService(
    private val sakService: SakService,
    private val oppgaveService: OppgaveService,
    private val overleveringDao: SoeknadOverleveringDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /** Kalles i en transaksjon, slik som `personer/saker/{type}` for NySoeknadRiver. */
    fun journalfoeringskontekst(request: JournalfoeringskontekstRequest): JournalfoeringskontekstResponse {
        val sakType = request.soeknadType.sakType
        val fnr = request.foedselsnummer
        if (fnr == null || !Folkeregisteridentifikator.isValid(fnr)) {
            logger.info("Søknad ${request.soeknadType} har ugyldig eller manglende fnr. Journalføres på ukjent bruker.")
            return JournalfoeringskontekstResponse(
                ukjentBruker = true,
                sakId = null,
                sakType = sakType,
                tema = sakType.tema,
                journalfoerendeEnhet = Enheter.defaultEnhet.enhetNr,
            )
        }
        val sak = sakService.finnEllerOpprettSakMedGrunnlag(fnr = fnr, type = sakType)
        return JournalfoeringskontekstResponse(
            ukjentBruker = false,
            sakId = sak.id,
            sakType = sak.sakType,
            tema = sak.sakType.tema,
            journalfoerendeEnhet = sak.enhet,
        )
    }

    /**
     * Kalles i en transaksjon. Nøkkel og oppgave committer sammen, så et nytt forsøk, et parallelt kall eller
     * et kall etter tapt svar gir samme oppgave. Samme nøkkel med annet innhold avvises.
     */
    fun overlever(request: SoeknadOverleveringRequest): SoeknadOverleveringResponse {
        val sak =
            sakService.finnSak(request.sakId)
                ?: throw UgyldigForespoerselException("SOEKNAD_OVERLEVERING_UKJENT_SAK", "Fant ikke sak ${request.sakId}")
        if (sak.sakType != request.soeknadType.sakType) {
            throw UgyldigForespoerselException(
                "SOEKNAD_OVERLEVERING_FEIL_SAKTYPE",
                "Sak ${request.sakId} har saktype ${sak.sakType}, forventet ${request.soeknadType.sakType}",
            )
        }

        val overlevering =
            SoeknadOverlevering(
                mottaksnoekkel = request.mottaksnoekkel,
                soeknadType = request.soeknadType,
                sakId = request.sakId,
                journalpostId = request.journalpostId,
                dokumentInfoIder = request.dokumentInfoIder,
                oppgaveId = null,
            )

        if (overleveringDao.reserver(overlevering)) {
            val oppgave =
                oppgaveService.opprettOppgave(
                    referanse = request.journalpostId,
                    sakId = request.sakId,
                    kilde = OppgaveKilde.BRUKERDIALOG,
                    type = OppgaveType.TILLEGGSINFORMASJON,
                    merknad = MERKNAD_UTVIDET_OMS,
                )
            overleveringDao.lagreOppgave(request.mottaksnoekkel, oppgave.id)
            logger.info("Søknad ${request.mottaksnoekkel} overlevert. Opprettet oppgave ${oppgave.id} på sak ${request.sakId}")
            return overlevering.copy(oppgaveId = oppgave.id).tilResponse()
        }

        val eksisterende = checkNotNull(overleveringDao.hent(request.mottaksnoekkel))
        if (eksisterende.copy(oppgaveId = null) != overlevering) {
            throw MotstridendeOverlevering(request.mottaksnoekkel)
        }
        logger.info("Søknad ${request.mottaksnoekkel} var allerede overlevert. Returnerer eksisterende oppgave.")
        return eksisterende.tilResponse()
    }

    private fun SoeknadOverlevering.tilResponse() =
        SoeknadOverleveringResponse(
            mottaksnoekkel = mottaksnoekkel,
            sakId = sakId,
            journalpostId = journalpostId,
            oppgaveId = checkNotNull(oppgaveId) { "Overlevering $mottaksnoekkel mangler oppgave" }.toString(),
        )

    companion object {
        const val MERKNAD_UTVIDET_OMS = "Søknad om utvidet omstillingsstønad mottatt. Søknaden må behandles manuelt."
    }
}
