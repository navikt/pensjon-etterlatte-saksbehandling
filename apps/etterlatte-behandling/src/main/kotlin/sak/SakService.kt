package no.nav.etterlatte.sak

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.IngenGeografiskOmraadeFunnetForEnhet
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilknytning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.tilgangsstyring.filterForEnheter
import org.slf4j.LoggerFactory

interface SakService {
    fun oppdaterUtenlandstilknytning(
        sakId: Long,
        utenlandstilknytning: Utenlandstilknytning,
    )

    fun hentSaker(): List<Sak>

    fun finnSaker(person: String): List<Sak>

    fun finnEllerOpprettSak(
        fnr: String,
        type: SakType,
        enhet: String? = null,
        gradering: AdressebeskyttelseGradering? = null,
    ): Sak

    fun finnSak(
        person: String,
        type: SakType,
    ): Sak?

    fun finnSak(id: Long): Sak?

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    )

    fun finnEnhetForPersonOgTema(
        fnr: String,
        tema: String,
        saktype: SakType,
    ): ArbeidsFordelingEnhet

    fun oppdaterEnhetForSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>)

    fun sjekkOmSakerErGradert(sakIder: List<Long>): List<SakMedGradering>

    fun oppdaterAdressebeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int

    fun hentSakMedUtenlandstilknytning(fnr: String): SakMedUtenlandstilknytning

    fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    )
}

class BrukerManglerSak(message: String) : Exception(message)

class BrukerHarMerEnnEnSak(message: String) : Exception(message)

class SakServiceImpl(
    private val dao: SakDao,
    private val pdlKlient: PdlKlient,
    private val norg2Klient: Norg2Klient,
    private val skjermingKlient: SkjermingKlient,
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun oppdaterUtenlandstilknytning(
        sakId: Long,
        utenlandstilknytning: Utenlandstilknytning,
    ) {
        dao.oppdaterUtenlandstilknytning(sakId, utenlandstilknytning)
    }

    override fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    ) {
        dao.oppdaterFlyktning(sakId, flyktning)
    }

    override fun hentSaker(): List<Sak> {
        return dao.hentSaker().filterForEnheter()
    }

    private fun finnSakerForPerson(person: String) = dao.finnSaker(person)

    private fun finnSakerForPersonOgType(
        person: String,
        type: SakType,
    ) = finnSakerForPerson(person).find {
        it.sakType == type
    }

    override fun finnSaker(person: String): List<Sak> {
        return finnSakerForPerson(person).filterForEnheter()
    }

    override fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ) {
        dao.markerSakerMedSkjerming(sakIder, skjermet)
    }

    override fun finnEllerOpprettSak(
        fnr: String,
        type: SakType,
        enhet: String?,
        gradering: AdressebeskyttelseGradering?,
    ): Sak {
        val enhetFraNorg = finnEnhetForPersonOgTema(fnr, type.tema, type).enhetNr
        if (enhet != null && enhet != enhetFraNorg) {
            logger.info("Finner/oppretter sak med enhet $enhet, selv om geografisk tilknytning tilsier $enhetFraNorg")
        }

        val sak = finnSakerForPersonOgType(fnr, type) ?: dao.opprettSak(fnr, type, enhet ?: enhetFraNorg)
        sjekkSkjerming(fnr = fnr, sakId = sak.id)
        gradering?.let {
            oppdaterAdressebeskyttelse(sak.id, it)
        }

        return sak
    }

    override fun oppdaterAdressebeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        return dao.oppdaterAdresseBeskyttelse(sakId, adressebeskyttelseGradering)
    }

    override fun hentSakMedUtenlandstilknytning(fnr: String): SakMedUtenlandstilknytning {
        val sakerForPerson = dao.finnSaker(fnr)

        val sak =
            when (sakerForPerson.size) {
                0 -> throw BrukerManglerSak("Ingen saker funnet for maskert person: ${fnr.maskerFnr()}")
                1 -> sakerForPerson[0]
                else -> {
                    logger.error(
                        "Person har mer enn en sak, hvilken skal vise utenlandstilknytning? " +
                            "Person ${fnr.maskerFnr()} har flere enn en sak ider ${sakerForPerson.map { it.id }} . Må håndteres",
                    )
                    throw BrukerHarMerEnnEnSak(
                        "Person ${fnr.maskerFnr()} har flere enn en sak ider ${sakerForPerson.map { it.id }} . Må håndteres",
                    )
                }
            }

        return dao.hentUtenlandstilknytningForSak(sak.id)!!
    }

    private fun sjekkSkjerming(
        fnr: String,
        sakId: Long,
    ) {
        val erSkjermet =
            runBlocking {
                skjermingKlient.personErSkjermet(fnr)
            }
        if (erSkjermet) {
            dao.oppdaterEnheterPaaSaker(
                listOf(GrunnlagsendringshendelseService.SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr)),
            )
        }
        dao.markerSakerMedSkjerming(sakIder = listOf(sakId), skjermet = erSkjermet)
    }

    override fun oppdaterEnhetForSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>) {
        dao.oppdaterEnheterPaaSaker(saker)
    }

    override fun sjekkOmSakerErGradert(sakIder: List<Long>): List<SakMedGradering> {
        return dao.finnSakerMedGraderingOgSkjerming(sakIder)
    }

    override fun finnSak(
        person: String,
        type: SakType,
    ): Sak? {
        return finnSakerForPersonOgType(person, type).sjekkEnhet()
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id).sjekkEnhet()
    }

    override fun finnEnhetForPersonOgTema(
        fnr: String,
        tema: String,
        saktype: SakType,
    ): ArbeidsFordelingEnhet {
        val tilknytning = pdlKlient.hentGeografiskTilknytning(fnr, saktype)
        val geografiskTilknytning = tilknytning.geografiskTilknytning()

        return when {
            tilknytning.ukjent ->
                ArbeidsFordelingEnhet(
                    Enheter.defaultEnhet.navn,
                    Enheter.defaultEnhet.enhetNr,
                )

            geografiskTilknytning == null -> throw IngenGeografiskOmraadeFunnetForEnhet(
                Folkeregisteridentifikator.of(fnr),
                tema,
            ).also {
                logger.warn(it.message)
            }

            else -> finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
        }
    }

    private fun finnEnhetForTemaOgOmraade(
        tema: String,
        omraade: String,
    ) = norg2Klient.hentEnheterForOmraade(tema, omraade).firstOrNull() ?: throw IngenEnhetFunnetException(omraade, tema)

    private fun List<Sak>.filterForEnheter() =
        this.filterSakerForEnheter(
            Kontekst.get().AppUser,
        )

    private fun Sak?.sjekkEnhet() =
        this?.let { sak ->
            listOf(sak).filterForEnheter().firstOrNull()
        }
}

fun List<Sak>.filterSakerForEnheter(user: User) =
    this.filterForEnheter(user) { item, enheter ->
        enheter.contains(item.enhet)
    }
