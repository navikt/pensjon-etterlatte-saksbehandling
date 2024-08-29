package no.nav.etterlatte.sak

import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

interface SakService {
    fun hentSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<Long>,
        ekskluderteSaker: List<Long>,
        sakType: SakType? = null,
    ): List<Sak>

    fun finnSaker(ident: String): List<Sak>

    fun finnEllerOpprettSakMedGrunnlag(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: String? = null,
    ): Sak

    fun finnGjeldendeEnhet(
        fnr: String,
        type: SakType,
    ): String

    fun finnSak(
        ident: String,
        type: SakType,
    ): Sak?

    fun finnSak(id: Long): Sak?

    fun finnFlyktningForSak(id: Long): Flyktning?

    fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    )

    fun oppdaterEnhetForSaker(saker: List<SakMedEnhet>)

    fun sjekkOmSakerErGradert(sakIder: List<Long>): List<SakMedGradering>

    fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int

    fun hentEnkeltSakForPerson(fnr: String): Sak

    suspend fun finnNavkontorForPerson(fnr: String): Navkontor

    fun hentSakerMedIder(sakIder: List<Long>): Map<Long, Sak>

    fun finnSakerOmsOgHvisAvdoed(ident: String): List<Long>
}

class ManglerTilgangTilEnhet(
    enheter: List<String>,
) : UgyldigForespoerselException(
        code = "MANGLER_TILGANG_TIL_ENHET",
        detail = "Mangler tilgang til enhet $enheter",
    )

class SakServiceImpl(
    private val dao: SakSkrivDao,
    private val lesDao: SakLesDao,
    private val skjermingKlient: SkjermingKlient,
    private val brukerService: BrukerService,
    private val grunnlagService: GrunnlagService,
    private val krrKlient: KrrKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

    override fun hentSakerMedIder(sakIder: List<Long>): Map<Long, Sak> {
        val saker = lesDao.hentSakerMedIder(sakIder)
        return saker.associateBy { it.id }
    }

    override fun hentSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<Long>,
        ekskluderteSaker: List<Long>,
        sakType: SakType?,
    ): List<Sak> =
        lesDao
            .hentSaker(kjoering, antall, spesifikkeSaker, ekskluderteSaker, sakType)
            .also { logger.info("Henta ${it.size} saker før filtrering") }
            .filterForEnheter()
            .also { logger.info("Henta ${it.size} saker etter filtrering") }

    private fun finnSakForPerson(
        ident: String,
        sakType: SakType,
    ) = finnSakerForPerson(ident, sakType).let {
        if (it.isEmpty()) {
            null
        } else {
            it.single()
        }
    }

    override fun finnSakerOmsOgHvisAvdoed(ident: String): List<Long> {
        val saker = finnSakerForPerson(ident, SakType.OMSTILLINGSSTOENAD).filterForEnheter()
        val sakerOgRollerForPerson = runBlocking { grunnlagService.hentAlleSakerForPerson(ident) }
        val sakerOgRollerGruppert = sakerOgRollerForPerson.sakiderOgRoller.distinct()
        val avdoedSak = sakerOgRollerGruppert.filter { it.rolle == Saksrolle.AVDOED }
        val sakerForAvdoed = avdoedSak.map { it.sakId }

        return saker.map { it.id } + sakerForAvdoed
    }

    override fun finnSaker(ident: String): List<Sak> = finnSakerForPerson(ident).filterForEnheter()

    private fun finnSakerForPerson(
        ident: String,
        sakType: SakType? = null,
    ) = lesDao.finnSaker(ident, sakType)

    override fun markerSakerMedSkjerming(
        sakIder: List<Long>,
        skjermet: Boolean,
    ) {
        dao.markerSakerMedSkjerming(sakIder, skjermet)
    }

    override fun finnEllerOpprettSakMedGrunnlag(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: String?,
    ): Sak {
        val sak = finnEllerOpprettSak(fnr, type, overstyrendeEnhet)

        runBlocking { grunnlagService.leggInnNyttGrunnlagSak(sak, Persongalleri(sak.ident)) }
        val kilde = Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now())
        val spraak = hentSpraak(sak.ident)
        val spraakOpplysning = lagOpplysning(Opplysningstype.SPRAAK, kilde, spraak.verdi.toJsonNode())
        runBlocking {
            grunnlagService.leggTilNyeOpplysningerBareSak(
                sakId = sak.id,
                opplysninger = NyeSaksopplysninger(sak.id, listOf(spraakOpplysning)),
            )
        }
        return sak
    }

    private fun hentSpraak(fnr: String): Spraak {
        val kontaktInfo =
            runBlocking {
                krrKlient.hentDigitalKontaktinformasjon(fnr)
            }

        return kontaktInfo
            ?.spraak
            ?.toLowerCasePreservingASCIIRules()
            ?.let {
                when (it) {
                    "nb" -> Spraak.NB
                    "nn" -> Spraak.NN
                    "en" -> Spraak.EN
                    else -> Spraak.NB
                }
            } ?: Spraak.NB
    }

    private fun finnEllerOpprettSak(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: String?,
    ): Sak {
        var sak = finnSakForPerson(fnr, type)
        if (sak == null) {
            val enhet = sjekkEnhetFraNorg(fnr, type, overstyrendeEnhet)
            sak = dao.opprettSak(fnr, type, enhet)
        }

        sjekkSkjerming(fnr = fnr, sakId = sak.id)
        val hentetGradering =
            runBlocking {
                pdltjenesterKlient.hentAdressebeskyttelseForPerson(
                    HentAdressebeskyttelseRequest(
                        PersonIdent(sak.ident),
                        sak.sakType,
                    ),
                )
            }
        oppdaterAdressebeskyttelse(sak.id, hentetGradering)
        settEnhetOmAdresebeskyttet(sak, hentetGradering)
        sjekkGraderingOgEnhetStemmer(lesDao.finnSakMedGraderingOgSkjerming(sak.id))
        return sak
    }

    private fun settEnhetOmAdresebeskyttet(
        sak: Sak,
        gradering: AdressebeskyttelseGradering,
    ) {
        when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr) {
                    dao.oppdaterEnheterPaaSaker(
                        listOf(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr)),
                    )
                }
            }

            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG.enhetNr) {
                    dao.oppdaterEnheterPaaSaker(
                        listOf(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG.enhetNr)),
                    )
                }
            }

            AdressebeskyttelseGradering.FORTROLIG -> return
            AdressebeskyttelseGradering.UGRADERT -> return
        }
    }

    private fun sjekkGraderingOgEnhetStemmer(sak: SakMedGraderingOgSkjermet) {
        sak.gradertEnhetsnummerErIkkeAlene()
        sak.egenAnsattStemmer()
        sak.graderingerStemmer()
    }

    private fun SakMedGraderingOgSkjermet.gradertEnhetsnummerErIkkeAlene() {
        if (this.enhetNr == Enheter.STRENGT_FORTROLIG.enhetNr &&
            this.adressebeskyttelseGradering !in
            listOf(
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
            )
        ) {
            logger.error("Sak har fått satt feil gradering basert enhetsnummer, se sikkerlogg.")
            sikkerLogg.info("Sakid: ${this.id} har fått satt feil gradering basert enhetsnummer")
        }
    }

    private fun SakMedGraderingOgSkjermet.graderingerStemmer() {
        when (this.adressebeskyttelseGradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> {
                if (this.enhetNr != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr) {
                    logger.error("Sak har fått satt feil enhetsnummer basert på gradering, se sikkerlogg.")
                    sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering strengt fortrolig")
                }
            }

            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                if (this.enhetNr != Enheter.STRENGT_FORTROLIG.enhetNr) {
                    logger.error("Sak har fått satt feil enhetsnummer basert på gradering, se sikkerlogg.")
                    sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering strengt fortrolig")
                }
            }

            AdressebeskyttelseGradering.FORTROLIG, AdressebeskyttelseGradering.UGRADERT, null -> return
        }
    }

    private fun SakMedGraderingOgSkjermet.egenAnsattStemmer() {
        if (this.erSkjermet == true) {
            if (this.enhetNr != Enheter.EGNE_ANSATTE.enhetNr) {
                logger.error("Sak har fått satt feil enhetsnummer basert på skjermingen, se sikkerlogg.")
                sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering skjerming(egen ansatt)")
            }
        }
        if (this.enhetNr == Enheter.EGNE_ANSATTE.enhetNr && this.erSkjermet != true) {
            logger.error("Sak mangler skjerming, se sikkerlogg.")
            sikkerLogg.info("Sakid: ${this.id} har fått satt feil skjerming(egen ansatt)")
        }
    }

    override fun finnGjeldendeEnhet(
        fnr: String,
        type: SakType,
    ): String {
        val sak = finnSakForPerson(fnr, type)

        return when (sak) {
            null -> sjekkEnhetFraNorg(fnr, type, null)
            else -> sak.enhet
        }
    }

    private fun sjekkEnhetFraNorg(
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
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int = dao.oppdaterAdresseBeskyttelse(sakId, adressebeskyttelseGradering)

    private fun sjekkSkjerming(
        fnr: String,
        sakId: SakId,
    ) {
        val erSkjermet =
            runBlocking {
                skjermingKlient.personErSkjermet(fnr)
            }
        if (erSkjermet) {
            dao.oppdaterEnheterPaaSaker(
                listOf(SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr)),
            )
        }
        dao.markerSakerMedSkjerming(sakIder = listOf(sakId), skjermet = erSkjermet)
    }

    override fun oppdaterEnhetForSaker(saker: List<SakMedEnhet>) {
        dao.oppdaterEnheterPaaSaker(saker)
    }

    override fun sjekkOmSakerErGradert(sakIder: List<Long>): List<SakMedGradering> = lesDao.finnSakerMedGraderingOgSkjerming(sakIder)

    override fun finnSak(
        ident: String,
        type: SakType,
    ): Sak? = finnSakForPerson(ident, type).sjekkEnhet()

    override fun finnSak(id: Long): Sak? = lesDao.hentSak(id).sjekkEnhet()

    override fun finnFlyktningForSak(id: Long): Flyktning? = lesDao.hentSak(id).sjekkEnhet()?.let { lesDao.finnFlyktningForSak(id) }

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
    ): List<Sak> = saker.filter { it.enhet !in enheterSomSkalFiltreres }
}
