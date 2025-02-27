package no.nav.etterlatte.sak

import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.GrunnlagUtils.opplysningsbehov
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.YearMonth

interface SakService {
    fun hentSakIdListeForKjoering(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
        sakType: SakType? = null,
        loependeFom: YearMonth? = null,
    ): List<SakId>

    fun finnSaker(ident: String): List<Sak>

    fun finnEllerOpprettSakMedGrunnlag(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer? = null,
    ): Sak

    fun finnGjeldendeEnhet(
        fnr: String,
        type: SakType,
    ): Enhetsnummer

    fun finnSak(
        ident: String,
        type: SakType,
    ): Sak?

    fun finnSak(id: SakId): Sak?

    fun finnFlyktningForSak(id: SakId): Flyktning?

    fun oppdaterSkjerming(
        sakId: SakId,
        skjermet: Boolean,
    )

    fun oppdaterEnhet(
        sak: SakMedEnhet,
        kommentar: String? = null,
    )

    fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    )

    fun sjekkSkjerming(
        fnr: String,
        sakId: SakId,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    )

    fun hentEnkeltSakForPerson(fnr: String): Sak

    suspend fun finnNavkontorForPerson(fnr: String): Navkontor

    fun hentSakerMedIder(sakIder: List<SakId>): Map<SakId, Sak>

    fun finnSakerOmsOgHvisAvdoed(ident: String): List<SakId>

    fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet

    fun oppdaterIdentForSak(
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): Sak

    fun hentSakerMedPleieforholdetOpphoerte(maanedOpphoerte: YearMonth): List<SakId>

    fun settEnhetOmAdressebeskyttet(
        sak: Sak,
        gradering: AdressebeskyttelseGradering,
    )

    fun hentSaksendringer(sakId: SakId): List<SaksendringBegrenset>
}

class ManglerTilgangTilEnhet(
    enheter: List<Enhetsnummer>,
) : UgyldigForespoerselException(
        code = "MANGLER_TILGANG_TIL_ENHET",
        detail = "Mangler tilgang til enhet $enheter",
    )

class SakServiceImpl(
    private val dao: SakSkrivDao,
    private val lesDao: SakLesDao,
    private val endringerDao: SakendringerDao,
    private val skjermingKlient: SkjermingKlient,
    private val brukerService: BrukerService,
    private val grunnlagService: GrunnlagService,
    private val krrKlient: KrrKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val featureToggle: FeatureToggleService,
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

    override fun hentSakerMedIder(sakIder: List<SakId>): Map<SakId, Sak> {
        val saker = lesDao.hentSakerMedIder(sakIder)
        return saker.associateBy { it.id }
    }

    override fun hentSakIdListeForKjoering(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
        sakType: SakType?,
        loependeFom: YearMonth?,
    ): List<SakId> =
        lesDao
            .hentSaker(
                kjoering,
                antall,
                spesifikkeSaker,
                ekskluderteSaker,
                sakType,
                featureToggle.isEnabled(
                    ReguleringFeatureToggle.OMREGING_REKJOERE_MANUELL_UTEN_OPPGAVE,
                    false,
                ),
            ).also { logger.info("Henta ${it.size} saker før filtrering") }
            .filterForEnheter()
            .also { logger.info("Henta ${it.size} saker etter filtrering") }
            .map(Sak::id)

    private fun finnSakForPerson(
        ident: String,
        sakType: SakType,
    ) = finnSakerForPerson(ident, sakType).let {
        if (it.isEmpty()) {
            null
        } else if (it.size == 1) {
            it.single()
        } else {
            sikkerLogg.error("Fant ${it.size} saker av type $sakType på person: ${it.joinToString()}}")

            throw InternfeilException(
                "Personen har ${it.size} saker av type $sakType. " +
                    "Dette må meldes i Porten for manuell kontroll og opprydding.",
            )
        }
    }

    override fun finnSakerOmsOgHvisAvdoed(ident: String): List<SakId> {
        val saker = finnSakerForPerson(ident, SakType.OMSTILLINGSSTOENAD).filterForEnheter()
        val sakerOgRollerForPerson =
            grunnlagService.hentSakerOgRoller(Folkeregisteridentifikator.of(ident))
        val sakerOgRollerGruppert = sakerOgRollerForPerson.sakiderOgRoller.distinct()
        val avdoedSak = sakerOgRollerGruppert.filter { it.rolle == Saksrolle.AVDOED }
        val sakerForAvdoed = avdoedSak.map { it.sakId }

        return saker.map { it.id } + sakerForAvdoed
    }

    override fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet {
        val sak = lesDao.finnSakMedGraderingOgSkjerming(sakId)
        return sak
    }

    override fun finnSaker(ident: String): List<Sak> = finnSakerForPerson(ident).filterForEnheter()

    private fun finnSakerForPerson(
        ident: String,
        sakType: SakType? = null,
    ): List<Sak> =
        runBlocking {
            pdltjenesterKlient
                .hentPdlFolkeregisterIdenter(ident)
                .identifikatorer
                .flatMap { lesDao.finnSaker(it.folkeregisterident.value, sakType) }
        }

    override fun oppdaterSkjerming(
        sakId: SakId,
        skjermet: Boolean,
    ) {
        dao.oppdaterSkjerming(sakId, skjermet)
    }

    override fun finnEllerOpprettSakMedGrunnlag(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    ): Sak {
        val sak = finnEllerOpprettSak(fnr, type, overstyrendeEnhet)

        leggTilGrunnlag(sak)

        return sak
    }

    private fun leggTilGrunnlag(sak: Sak) {
        val harGrunnlag = grunnlagService.grunnlagFinnesForSak(sak.id)

        if (harGrunnlag) {
            logger.info("Finnes allerede grunnlag på sak=${sak.id}")
            return
        }
        runBlocking {
            logger.info("Fant ingen grunnlag på sak=${sak.id} - oppretter grunnlag")

            val kilde = Grunnlagsopplysning.Gjenny(Fagsaksystem.EY.navn, Tidspunkt.now())
            val spraak = hentSpraak(sak.ident)
            val spraakOpplysning = lagOpplysning(Opplysningstype.SPRAAK, kilde, spraak.verdi.toJsonNode())

            grunnlagService.opprettEllerOppdaterGrunnlagForSak(
                sak.id,
                opplysningsbehov(sak, Persongalleri(sak.ident)),
            )

            grunnlagService.lagreNyeSaksopplysningerBareSak(
                sakId = sak.id,
                nyeOpplysninger = listOf(spraakOpplysning),
            )
            logger.info("Grunnlag opprettet på sak=${sak.id}")
        }
    }

    override fun oppdaterIdentForSak(
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): Sak {
        val identListe = runBlocking { pdltjenesterKlient.hentPdlFolkeregisterIdenter(sak.ident) }

        val alleIdenter = identListe.identifikatorer.map { it.folkeregisterident.value }
        if (sak.ident !in alleIdenter) {
            sikkerLogg.error("Ident ${sak.ident} fra sak ${sak.id} matcher ingen av identene fra PDL: $alleIdenter")
            throw InternfeilException(
                "Ident i sak ${sak.id} stemmer ikke overens med identer vi fikk fra PDL",
            )
        }

        val gjeldendeIdent =
            identListe.identifikatorer.singleOrNull { !it.historisk }?.folkeregisterident
                ?: throw InternfeilException("Sak ${sak.id} har flere eller ingen gyldige identer samtidig. Kan ikke oppdatere ident.")

        dao.oppdaterIdent(sak.id, gjeldendeIdent)
        val oppdatertPersongalleri =
            grunnlagService
                .hentPersongalleri(sak.id)!!
                .copy(soeker = gjeldendeIdent.value)

        runBlocking {
            grunnlagService.opprettEllerOppdaterGrunnlagForSak(sak.id, opplysningsbehov(sak, oppdatertPersongalleri))
        }

        logger.info("Oppdaterte sak ${sak.id} med bruker sin nyeste ident. Se sikkerlogg for detailjer")
        sikkerLogg.info(
            "Oppdaterte sak ${sak.id}: Endret ident fra ${sak.ident} til ${gjeldendeIdent.value}. " +
                "Alle identer fra PDL: ${identListe.identifikatorer.joinToString()}",
        )

        return lesDao.hentSak(sak.id)
            ?: throw InternfeilException("Kunne ikke hente ut sak ${sak.id} som nettopp ble endret")
    }

    override fun hentSakerMedPleieforholdetOpphoerte(maanedOpphoerte: YearMonth): List<SakId> {
        logger.info("Henter saker der dato pleieforholdet opphørte var $maanedOpphoerte")
        return lesDao
            .finnSakerMedPleieforholdOpphoerer(maanedOpphoerte)
            .also {
                logger.info("Fant ${it.size} saker der pleieforholdet opphørte i $maanedOpphoerte")
            }
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
        overstyrendeEnhet: Enhetsnummer?,
    ): Sak {
        var sak = finnSakForPerson(fnr, type)
        if (sak == null) {
            logger.info("Fant ingen sak av type=$type på person ${fnr.maskerFnr()} - oppretter ny sak")

            val enhet = sjekkEnhetFraNorg(fnr, type, overstyrendeEnhet)
            sak = dao.opprettSak(fnr, type, enhet)
        }

        sjekkSkjerming(fnr = fnr, sakId = sak.id, type = type, overstyrendeEnhet = overstyrendeEnhet)

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
        settEnhetOmAdressebeskyttet(sak, hentetGradering)
        sjekkGraderingOgEnhetStemmer(lesDao.finnSakMedGraderingOgSkjerming(sak.id))
        return sak
    }

    override fun settEnhetOmAdressebeskyttet(
        sak: Sak,
        gradering: AdressebeskyttelseGradering,
    ) {
        when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr) {
                    dao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr))
                }
            }

            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                if (sak.enhet != Enheter.STRENGT_FORTROLIG.enhetNr) {
                    dao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG.enhetNr))
                }
            }

            AdressebeskyttelseGradering.FORTROLIG -> return
            AdressebeskyttelseGradering.UGRADERT -> return
        }
    }

    override fun hentSaksendringer(sakId: SakId): List<SaksendringBegrenset> {
        val saksendringer = endringerDao.hentEndringerForSak(sakId)

        // Inntil vi har gått opp om det er greit å vise adressebeskyttelse og skjerming, så ønsker vi ikke å eksponere
        // dette. Verken som egne endringstyper eller som en del av andre endringstyper.
        val saksendringerUtenSensitiveEndringer =
            saksendringer
                .filter {
                    it.endringstype in
                        listOf(
                            Endringstype.OPPRETT_SAK,
                            Endringstype.ENDRE_ENHET,
                            Endringstype.ENDRE_IDENT,
                        )
                }.map(Saksendring::toSaksendringBegrenset)

        return saksendringerUtenSensitiveEndringer
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
    ): Enhetsnummer {
        val sak = finnSakForPerson(fnr, type)

        return when (sak) {
            null -> sjekkEnhetFraNorg(fnr, type, null)
            else -> sak.enhet
        }
    }

    private fun sjekkEnhetFraNorg(
        fnr: String,
        type: SakType,
        enhet: Enhetsnummer?,
    ): Enhetsnummer {
        val enhetFraNorg = brukerService.finnEnhetForPersonOgTema(fnr, type.tema, type).enhetNr
        if (enhet != null && enhet != enhetFraNorg) {
            logger.info("Finner/oppretter sak med enhet $enhet, selv om geografisk tilknytning tilsier $enhetFraNorg")
        }
        return enhet ?: enhetFraNorg
    }

    override fun oppdaterAdressebeskyttelse(
        sakId: SakId,
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
    ) = dao.oppdaterAdresseBeskyttelse(sakId, adressebeskyttelseGradering)

    override fun sjekkSkjerming(
        fnr: String,
        sakId: SakId,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    ) {
        val erSkjermet =
            runBlocking {
                skjermingKlient.personErSkjermet(fnr)
            }
        if (erSkjermet) {
            logger.info("Oppdater egen ansatt for sak $sakId")
            dao.oppdaterEnhet(
                SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr),
            )
        } else {
            val sakMedSkjerming = lesDao.hentSak(sakId)!!
            if (sakMedSkjerming.enhet == Enheter.EGNE_ANSATTE.enhetNr) {
                val enhet = sjekkEnhetFraNorg(fnr, type, overstyrendeEnhet)
                if (enhet == Enheter.EGNE_ANSATTE.enhetNr) {
                    dao.oppdaterEnhet(SakMedEnhet(sakId, Enheter.defaultEnhet.enhetNr))
                } else {
                    dao.oppdaterEnhet(SakMedEnhet(sakId, enhet))
                }
            }
        }

        dao.oppdaterSkjerming(sakId = sakId, skjermet = erSkjermet)
    }

    override fun oppdaterEnhet(
        sak: SakMedEnhet,
        kommentar: String?,
    ) {
        dao.oppdaterEnhet(sak, kommentar)
    }

    override fun finnSak(
        ident: String,
        type: SakType,
    ): Sak? = finnSakForPerson(ident, type).sjekkEnhet()

    override fun finnSak(id: SakId): Sak? = lesDao.hentSak(id).sjekkEnhet()

    override fun finnFlyktningForSak(id: SakId): Flyktning? = lesDao.hentSak(id).sjekkEnhet()?.let { lesDao.finnFlyktningForSak(id) }

    private fun Sak?.sjekkEnhet() =
        this?.let { sak ->
            listOf(sak).filterForEnheter().firstOrNull()
        }

    private fun List<Sak>.filterForEnheter(): List<Sak> {
        val enheterSomSkalFiltreresBort = ArrayList<Enhetsnummer>()
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
        enheterSomSkalFiltreres: List<Enhetsnummer>,
        saker: List<Sak>,
    ): List<Sak> = saker.filter { it.enhet !in enheterSomSkalFiltreres }
}

enum class ReguleringFeatureToggle(
    private val key: String,
) : FeatureToggle {
    OMREGING_REKJOERE_MANUELL_UTEN_OPPGAVE("aarlig-inntektsjustering-la-manuell-behandling"),
    ;

    override fun key() = key
}
