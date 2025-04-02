package no.nav.etterlatte.sak

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.ktor.token.Systembruker

interface SakTilgang {
    fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    )

    fun settEnhetOmAdressebeskyttet(
        sak: Sak,
        gradering: AdressebeskyttelseGradering,
    )

    fun oppdaterSkjerming(
        sakId: SakId,
        skjermet: Boolean,
    )

    fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet
}

class SakTilgangImpl(
    private val skrivDao: SakSkrivDao,
    private val lesDao: SakLesDao,
) : SakTilgang {
    override fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet = lesDao.finnSakMedGraderingOgSkjerming(sakId)

    override fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ) = skrivDao.oppdaterAdresseBeskyttelse(sakId, adressebeskyttelseGradering)

    override fun settEnhetOmAdressebeskyttet(
        sak: Sak,
        gradering: AdressebeskyttelseGradering,
    ) {
        when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr) {
                    skrivDao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr))
                }
            }

            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG.enhetNr) {
                    skrivDao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG.enhetNr))
                }
            }

            AdressebeskyttelseGradering.FORTROLIG -> return
            AdressebeskyttelseGradering.UGRADERT -> return
        }
    }

    override fun oppdaterSkjerming(
        sakId: SakId,
        skjermet: Boolean,
    ) {
        skrivDao.oppdaterSkjerming(sakId, skjermet)
    }
}
