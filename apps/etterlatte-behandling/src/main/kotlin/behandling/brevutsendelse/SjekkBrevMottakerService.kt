package no.nav.etterlatte.behandling.brevutsendelse

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.GYLDIG_MOTTAKER
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.UGYLDIG_MOTTAKER_UTDATERTE_PERSON_OPPLYSNINGER
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.UGYLDIG_MOTTAKER_UTDATERT_IDENT
import no.nav.etterlatte.behandling.brevutsendelse.SjekkGyldigBrevMottakerResultat.UGYLDIG_MOTTAKER_VERGEMAAL
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.LoggerFactory

enum class SjekkGyldigBrevMottakerResultat {
    UGYLDIG_MOTTAKER_UTDATERT_IDENT,
    UGYLDIG_MOTTAKER_UTDATERTE_PERSON_OPPLYSNINGER,
    UGYLDIG_MOTTAKER_VERGEMAAL,
    GYLDIG_MOTTAKER,
}

class SjekkBrevMottakerService(
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sjekkOmPersonErGyldigBrevmottaker(sak: Sak): SjekkGyldigBrevMottakerResultat {
        logger.info("Sjekker om person ${sak.ident.maskerFnr()} er en gyldig brev mottaker")
        sikkerlogger().info("Sjekker om person ${sak.ident} er en gyldig brev mottaker")

        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(sak.id)
                ?: throw InternfeilException("Fant ingen iverksatt behandling i sak ${sak.id}")

        // Sjekker ident
        hentPdlPersonident(sak).let { sisteIdentifikatorPdl ->
            val sisteIdent =
                when (sisteIdentifikatorPdl) {
                    is PdlIdentifikator.FolkeregisterIdent -> sisteIdentifikatorPdl.folkeregisterident.value
                    is PdlIdentifikator.Npid -> sisteIdentifikatorPdl.npid.ident
                }
            if (sak.ident != sisteIdent) {
                return UGYLDIG_MOTTAKER_UTDATERT_IDENT
            }
        }

        val opplysningerPdl = hentPdlPersonopplysning(sak)
        val opplysningerGjenny = hentOpplysningerGjenny(sak, sisteIverksatteBehandling)

        // Sjekker vergemål
        if (!opplysningerPdl.vergemaalEllerFremtidsfullmakt.isNullOrEmpty()) {
            return UGYLDIG_MOTTAKER_VERGEMAAL
        }

        val opplysningerErUendretIPdl =
            with(opplysningerGjenny) {
                fornavn == opplysningerPdl.fornavn &&
                    mellomnavn == opplysningerPdl.mellomnavn &&
                    etternavn == opplysningerPdl.etternavn &&
                    foedselsdato == opplysningerPdl.foedselsdato &&
                    erLikeVergemaal(vergemaalEllerFremtidsfullmakt, opplysningerPdl.vergemaalEllerFremtidsfullmakt)
            }

        // Sjekker for utdaterte personopplysninger
        if (!opplysningerErUendretIPdl) {
            logger.info(
                "Personopplysninger i PDL og Gjenny er forskjellig i sak ${sak.id}. " +
                    "Det opprettes en oppgave for manuell håndtering. Se sikkerlogg for detaljer om hva som er forskjellig.",
            )
            sikkerlogger().info(
                "Personopplysninger i PDL og Gjenny er forskjellig i sak ${sak.id}. " +
                    "Personopplysinger i Grunnlag: $opplysningerGjenny, " +
                    "Personopplysinger i PDL: $opplysningerPdl",
            )
            return UGYLDIG_MOTTAKER_UTDATERTE_PERSON_OPPLYSNINGER
        }

        return GYLDIG_MOTTAKER
    }

    private fun hentPdlPersonident(sak: Sak) =
        runBlocking {
            pdlTjenesterKlient.hentPdlIdentifikator(sak.ident)
                ?: throw InternfeilException("Fant ikke ident fra PDL for sak ${sak.id}")
        }

    private fun hentPdlPersonopplysning(sak: Sak): Person {
        // Mottaker av ytelsen
        val rolle =
            when (sak.sakType) {
                SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                SakType.BARNEPENSJON -> PersonRolle.BARN
            }

        return pdlTjenesterKlient
            .hentPdlModellForSaktype(sak.ident, rolle, sak.sakType)
            .toPerson()
    }

    private fun hentOpplysningerGjenny(
        sak: Sak,
        behandling: Behandling,
    ): Person =
        grunnlagService
            .hentPersonopplysninger(
                behandling.id,
                sak.sakType,
            ).soeker
            ?.opplysning ?: throw InternfeilException("Fant ikke opplysninger for sak=${sak.id}")

    private fun erLikeVergemaal(
        vergerEn: List<VergemaalEllerFremtidsfullmakt>?,
        vergerTo: List<VergemaalEllerFremtidsfullmakt>?,
    ): Boolean {
        if (vergerEn.isNullOrEmpty() && vergerTo.isNullOrEmpty()) {
            return true
        }
        return vergerEn == vergerTo
    }
}
