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
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
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
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
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

    fun sjekkSkjerming(
        fnr: String,
        sakId: SakId,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    )

    fun hentSakHvisSaksbehandlerHarTilgang(fnr: String): Sak

    suspend fun finnNavkontorForPerson(fnr: String): Navkontor

    fun hentSakerMedIder(sakIder: List<SakId>): Map<SakId, Sak>

    fun finnSakerOmsOgHvisAvdoed(ident: String): List<SakId>

    fun oppdaterIdentForSak(
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): Sak

    fun hentSakerMedPleieforholdetOpphoerte(maanedOpphoerte: YearMonth): List<SakId>

    fun hentSakerBpFylt18AarIMaaned(maanedFyller18: YearMonth): List<SakId>

    fun hentSakerMedSkjerming(sakType: SakType): List<SakId>

    fun hentSaksendringer(sakId: SakId): List<Saksendring>

    fun oppdaterEnhet(
        sak: SakMedEnhet,
        kommentar: String? = null,
    )

    fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet
}

class ManglerTilgangTilEnhet(
    enheter: List<Enhetsnummer>,
) : UgyldigForespoerselException(
        code = "MANGLER_TILGANG_TIL_ENHET",
        detail = "Mangler tilgang til enhet $enheter",
    )

class KanIkkEndreSpesialenhet(
    detail: String,
) : UgyldigForespoerselException(
        code = "KAN_IKK_ENDRE_SPESIAL_ENHET",
        detail = detail,
    )

class SakServiceImpl(
    private val skrivDao: SakSkrivDao,
    private val lesDao: SakLesDao,
    private val endringerDao: SakendringerDao,
    private val skjermingKlient: SkjermingKlient,
    private val brukerService: BrukerService,
    private val grunnlagService: GrunnlagService,
    private val krrKlient: KrrKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val featureToggle: FeatureToggleService,
    private val tilgangsService: OppdaterTilgangService,
    private val sakTilgang: SakTilgang,
    private val aldersovergangService: AldersovergangService,
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentGraderingForSak(
        sakId: SakId,
        bruker: Systembruker,
    ): SakMedGraderingOgSkjermet = lesDao.finnSakMedGraderingOgSkjerming(sakId)

    override fun oppdaterEnhet(
        sak: SakMedEnhet,
        kommentar: String?,
    ) {
        if (Enheter.erSpesialTilgangsEnheter(sak.enhet)) {
            throw KanIkkEndreSpesialenhet("Kan ikke endre til spesial enhet")
        }
        skrivDao.oppdaterEnhet(sak, kommentar)
    }

    // Vi må verne om navkontor for de med adressebeskyttelse, siden de er geografisk informasjon
    override fun hentSakHvisSaksbehandlerHarTilgang(fnr: String): Sak {
        val saker = finnSakerForPerson(fnr)
        if (saker.isEmpty()) throw PersonManglerSak()

        return saker.filterForEnheter().firstOrNull()
            ?: throw ManglerTilgangTilEnhet(saker.map { it.enhet })
    }

    override suspend fun finnNavkontorForPerson(fnr: String): Navkontor {
        val sak =
            inTransaction {
                hentSakHvisSaksbehandlerHarTilgang(fnr)
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

    override fun finnEllerOpprettSakMedGrunnlag(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    ): Sak {
        val sak = finnEllerOpprettSak(fnr, type, overstyrendeEnhet)

        leggTilGrunnlag(sak)
        /*
        haandtergraderingOgEgenAnsatt kalles her slik at 3-parts graderinger for egen ansatt
        eller adressebeskyttelse ikke blir overskredet deresom man kaller finnEllerOpprettSakMedGrunnlag.
        Dette vil kun ha effekt for de som har persongallerier, dersom denne metoden blir kalt
        for en sak med persongalleri uten haandtergraderingOgEgenAnsatt forsvinner potensielt
        3-parts beskyttelser slik som DoedshendelseJobService.opprettSakOgLagGrunnlag potensielt gjør.
        Se https://jira.adeo.no/browse/FAGSYSTEM-376040?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258aXNzdWU%3D for konrekt case.
         */
        val persongalleri = grunnlagService.hentPersongalleri(sak.id)
        if (persongalleri != null) {
            tilgangsService.haandtergraderingOgEgenAnsatt(
                sak.id,
                persongalleri,
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id),
            )
        }
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

        skrivDao.oppdaterIdent(sak.id, gjeldendeIdent)
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

    override fun hentSakerBpFylt18AarIMaaned(maanedFyller18: YearMonth): List<SakId> {
        logger.info("Henter saker der bruker har fylt 18 år i $maanedFyller18")
        val foedselsMaaned = maanedFyller18.minusYears(18)
        return aldersovergangService
            .hentSoekereFoedtIEnGittMaaned(foedselsMaaned)
            .map { SakId(it.toLong()) }
    }

    override fun hentSakerMedSkjerming(sakType: SakType): List<SakId> {
        return lesDao.finnSakerMedSkjerming(sakType).map { it.id }
    }

    private suspend fun hentSpraak(fnr: String): Spraak {
        val kontaktInfo = krrKlient.hentDigitalKontaktinformasjon(fnr)

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

    /*
        Hvis man mot formodning gjør denne public må man huske på at
        sjekkene for enhet og gradering/adressebeskyttelse i denne kun er gyldig dersom
        en sak ikke har persongalleri.
        For at ting ikke skal overskride viktige beskyttelser for 3-part så
        må haandtergraderingOgEgenAnsatt() kalles på tilsvarende måte som i finnEllerOpprettSakMedGrunnlag().
     */
    private fun finnEllerOpprettSak(
        fnr: String,
        type: SakType,
        overstyrendeEnhet: Enhetsnummer?,
    ): Sak {
        var sak = finnSakForPerson(fnr, type)
        if (sak == null) {
            logger.info("Fant ingen sak av type=$type på person ${fnr.maskerFnr()} - oppretter ny sak")

            val enhet = sjekkEnhetFraNorg(fnr, type, overstyrendeEnhet)
            sak = skrivDao.opprettSak(fnr, type, enhet)
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
        sakTilgang.oppdaterAdressebeskyttelse(sak.id, hentetGradering)
        sakTilgang.settEnhetOmAdressebeskyttet(sak, hentetGradering)

        val oppdatertSak = lesDao.hentSak(sak.id) ?: throw InternfeilException("Sak ${sak.id} finnes ikke")
        sjekkGraderingOgEnhetStemmer(oppdatertSak)

        return oppdatertSak
    }

    override fun hentSaksendringer(sakId: SakId): List<Saksendring> {
        // Sjekk om saksbehandler kan hente historikk for sak
        val sak = lesDao.hentSak(sakId).sjekkEnhet()
        return sak?.let { endringerDao.hentEndringerForSak(it.id) } ?: emptyList()
    }

    private fun sjekkGraderingOgEnhetStemmer(sak: Sak) {
        sak.gradertEnhetsnummerErIkkeAlene()
        sak.egenAnsattStemmer()
        sak.graderingerStemmer()
    }

    private fun Sak.gradertEnhetsnummerErIkkeAlene() {
        if (this.enhet == Enheter.STRENGT_FORTROLIG.enhetNr &&
            this.adressebeskyttelse !in
            listOf(
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
            )
        ) {
            logger.error("Sak har fått satt feil gradering basert enhetsnummer, se sikkerlogg.")
            sikkerLogg.info("Sakid: ${this.id} har fått satt feil gradering basert enhetsnummer")
        }
    }

    private fun Sak.graderingerStemmer() {
        when (this.adressebeskyttelse) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> {
                if (this.enhet != Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr) {
                    logger.error("Sak har fått satt feil enhetsnummer basert på gradering, se sikkerlogg.")
                    sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering strengt fortrolig")
                }
            }

            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> {
                if (this.enhet != Enheter.STRENGT_FORTROLIG.enhetNr) {
                    logger.error("Sak har fått satt feil enhetsnummer basert på gradering, se sikkerlogg.")
                    sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering strengt fortrolig")
                }
            }

            AdressebeskyttelseGradering.FORTROLIG, AdressebeskyttelseGradering.UGRADERT, null -> return
        }
    }

    private fun Sak.egenAnsattStemmer() {
        if (this.erSkjermet == true) {
            if (this.enhet != Enheter.EGNE_ANSATTE.enhetNr) {
                logger.error("Sak har fått satt feil enhetsnummer basert på skjermingen, se sikkerlogg.")
                sikkerLogg.info("Sakid: ${this.id} har fått satt feil enhetsnummer basert på gradering skjerming(egen ansatt)")
            }
        }
        if (this.enhet == Enheter.EGNE_ANSATTE.enhetNr && this.erSkjermet != true) {
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
            logger.info("Oppdater egen ansatt for sak: $sakId")
            skrivDao.oppdaterEnhet(
                SakMedEnhet(sakId, Enheter.EGNE_ANSATTE.enhetNr),
            )
        } else {
            logger.info("Oppdater egen ansatt for sak, ikke skjermet. Setter ny enhet for sak: $sakId")
            val sakMedSkjerming = lesDao.hentSak(sakId)!!
            if (sakMedSkjerming.enhet == Enheter.EGNE_ANSATTE.enhetNr) {
                val enhet = sjekkEnhetFraNorg(fnr, type, overstyrendeEnhet)
                if (enhet == Enheter.EGNE_ANSATTE.enhetNr) {
                    skrivDao.oppdaterEnhet(SakMedEnhet(sakId, Enheter.defaultEnhet.enhetNr))
                } else {
                    skrivDao.oppdaterEnhet(SakMedEnhet(sakId, enhet))
                }
            }
        }

        skrivDao.oppdaterSkjerming(sakId = sakId, skjermet = erSkjermet)
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
