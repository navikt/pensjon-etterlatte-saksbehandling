package no.nav.etterlatte.sak

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.LoggerFactory

interface SakService {
    fun hentSaker(): List<Sak>

    fun finnSaker(person: String): List<Sak>

    fun finnEllerOpprettSak(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: String? = null,
        gradering: AdressebeskyttelseGradering? = null,
        sjekkEnhetMotNorg: Boolean = true,
    ): Sak

    fun finnGjeldeneEnhet(
        fnr: String,
        type: SakType,
    ): String

    fun finnSak(
        person: String,
        type: SakType,
    ): Sak?

    fun finnSak(id: Long): Sak?

    fun finnFlyktningForSak(id: Long): Flyktning?

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    )

    fun oppdaterEnhetForSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>)

    fun sjekkOmSakerErGradert(sakIder: List<Long>): List<SakMedGradering>

    fun oppdaterAdressebeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int

    fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    )

    fun hentEnkeltSakForPerson(fnr: String): Sak

    suspend fun finnNavkontorForPerson(fnr: String): Navkontor
}

class ManglerTilgangTilEnhet(enheter: List<String>) :
    UgyldigForespoerselException(
        code = "MANGLER_TILGANG_TIL_ENHET",
        detail = "Mangler tilgang til enhet $enheter",
    )

class SakServiceImpl(
    private val dao: SakDao,
    private val skjermingKlient: SkjermingKlient,
    private val brukerService: BrukerService,
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun oppdaterFlyktning(
        sakId: Long,
        flyktning: Flyktning,
    ) {
        dao.oppdaterFlyktning(sakId, flyktning)
    }

    override fun hentEnkeltSakForPerson(fnr: String): Sak {
        val saker = finnSakerForPerson(fnr)

        if (saker.isEmpty()) throw PersonManglerSak()

        return saker.filterForEnheter().firstOrNull()
            ?: throw ManglerTilgangTilEnhet(saker.map { it.enhet })
    }

    override suspend fun finnNavkontorForPerson(fnr: String): Navkontor {
        val sak =
            inTransaction {
                hentEnkeltSakForPerson(fnr)
            }
        return brukerService.finnNavkontorForPerson(fnr, sak.sakType)
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
        overstyrendeEnhet: String?,
        gradering: AdressebeskyttelseGradering?,
        sjekkEnhetMotNorg: Boolean,
    ): Sak {
        var sak = finnSakerForPersonOgType(fnr, type)
        if (sak == null) {
            val enhet =
                if (sjekkEnhetMotNorg) {
                    sjekkEnhet(fnr, type, overstyrendeEnhet)
                } else {
                    overstyrendeEnhet!!
                }
            sak = dao.opprettSak(fnr, type, enhet)
        }

        sjekkSkjerming(fnr = fnr, sakId = sak.id)
        gradering?.let {
            oppdaterAdressebeskyttelse(sak.id, it)
        }

        return sak
    }

    override fun finnGjeldeneEnhet(
        fnr: String,
        type: SakType,
    ) = when (val sak = finnSakerForPersonOgType(fnr, type)) {
        null -> sjekkEnhet(fnr, type, null)
        else -> sak.enhet
    }

    private fun sjekkEnhet(
        fnr: String,
        type: SakType,
        enhet: String?,
    ): String {
        val enhetFraNorg = brukerService.finnEnhetForPersonOgTema(fnr, type.tema, type).enhetNr
        if (enhet != null && enhet != enhetFraNorg) {
            logger.info("Finner/oppretter sak med enhet $enhet, selv om geografisk tilknytning tilsier $enhetFraNorg")
        }
        return enhet ?: enhetFraNorg
    }

    override fun oppdaterAdressebeskyttelse(
        sakId: Long,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        return dao.oppdaterAdresseBeskyttelse(sakId, adressebeskyttelseGradering)
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

    override fun finnFlyktningForSak(id: Long): Flyktning? = dao.hentSak(id).sjekkEnhet()?.let { dao.finnFlyktningForSak(id) }

    private fun Sak?.sjekkEnhet() =
        this?.let { sak ->
            listOf(sak).filterForEnheter().firstOrNull()
        }

    private fun List<Sak>.filterForEnheter(): List<Sak> {
        val enheterSomSkalFiltreresBort = ArrayList<String>()
        val appUser = Kontekst.get().AppUser
        if (appUser is SaksbehandlerMedEnheterOgRoller) {
            val bruker = appUser.saksbehandlerMedRoller
            if (!bruker.harRolleStrengtFortrolig()) {
                enheterSomSkalFiltreresBort.add(Enheter.STRENGT_FORTROLIG.enhetNr)
            }
            if (!bruker.harRolleEgenAnsatt()) {
                enheterSomSkalFiltreresBort.add(Enheter.EGNE_ANSATTE.enhetNr)
            }
        }
        return filterSakerForEnheter(enheterSomSkalFiltreresBort, this)
    }

    private fun filterSakerForEnheter(
        enheterSomSkalFiltreres: List<String>,
        saker: List<Sak>,
    ): List<Sak> {
        return saker.filter { it.enhet !in enheterSomSkalFiltreres }
    }
}
