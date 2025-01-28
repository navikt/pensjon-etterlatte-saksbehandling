package no.nav.etterlatte.tilgangsstyring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sak.PersonManglerSak
import no.nav.etterlatte.sak.SakService

class OppdaterTilgangService(
    private val sakService: SakService,
    private val skjermingKlient: SkjermingKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
) {
    /**
     * Her skal den høyeste graderingen aldri bli overskredet
     * Fra høyeste til lavest: STRENGT_FORTROLIG_UTLAND/STRENGT_FORTROLIG, FORTROLIG, EGEN_ANSATT
     * NB: Saker uten behandling vil ikke trenge dette
     **/
    fun haandtergraderingOgEgenAnsatt(
        sakId: SakId,
        persongalleri: Persongalleri,
    ) {
        val sak = sakService.finnSak(sakId) ?: throw PersonManglerSak()
        val alleIdenter = persongalleri.hentAlleIdentifikatorer()

        val identerMedGradering =
            alleIdenter
                .filter { Folkeregisteridentifikator.isValid(it) }
                .map { hentGraderingForIdent(it, sak) }

        if (identerMedGradering.any { it.harAdressebeskyttelse() }) {
            val hoyesteGradering = identerMedGradering.hentPrioritertGradering()
            sakService.oppdaterAdressebeskyttelse(sakId, hoyesteGradering)
            sakService.settEnhetOmAdresebeskyttet(sak, hoyesteGradering)
        } else {
            val egenAnsattSkjerming = alleIdenter.map { fnr -> sjekkOmIdentErSkjermet(fnr) }
            if (egenAnsattSkjerming.any { it }) {
                sakService.markerSakerMedSkjerming(listOf(sakId), true)
                sakService.oppdaterEnhetForSaker(listOf(SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr)))
            }
        }
    }

    private fun hentGraderingForIdent(
        fnr: String,
        sak: Sak,
    ): AdressebeskyttelseGradering =
        runBlocking {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(fnr),
                    sak.sakType,
                ),
            )
        }

    private fun sjekkOmIdentErSkjermet(fnr: String): Boolean =
        runBlocking {
            skjermingKlient.personErSkjermet(fnr)
        }
}
