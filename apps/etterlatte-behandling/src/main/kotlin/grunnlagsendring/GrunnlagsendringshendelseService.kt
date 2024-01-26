package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.hentAnsvarligeForeldre
import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.common.klienter.hentBostedsadresse
import no.nav.etterlatte.common.klienter.hentDoedsdato
import no.nav.etterlatte.common.klienter.hentSivilstand
import no.nav.etterlatte.common.klienter.hentUtland
import no.nav.etterlatte.common.klienter.hentVergemaal
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class KunneIkkeLukkeOppgaveForhendelse(message: String) :
    UgyldigForespoerselException(
        code = "FEIL_MED_OPPGAVE_UNDER_LUKKING",
        detail = message,
    )

class GrunnlagsendringshendelseService(
    private val oppgaveService: OppgaveService,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val behandlingService: BehandlingService,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sakService: SakService,
    private val brukerService: BrukerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGyldigeHendelserForSak(sakId: Long) = grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sakId)

    fun hentAlleHendelserForSak(sakId: Long): List<Grunnlagsendringshendelse> {
        logger.info("Henter alle relevante hendelser for sak $sakId")
        return grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.relevantForSaksbehandler().toList(),
        )
    }

    fun hentAlleHendelserForSakAvType(
        sakId: Long,
        type: GrunnlagsendringsType,
    ) = inTransaction {
        logger.info("Henter alle relevante hendelser for sak $sakId")
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISakAvType(
            sakId,
            GrunnlagsendringStatus.relevantForSaksbehandler().toList(),
            type,
        )
    }

    fun lukkHendelseMedKommentar(
        hendelse: Grunnlagsendringshendelse,
        saksbehandler: Saksbehandler,
    ) {
        logger.info("Lukker hendelse med id $hendelse.id")

        inTransaction {
            grunnlagsendringshendelseDao.lukkGrunnlagsendringStatus(hendelse = hendelse)
            try {
                val oppgaveForReferanse = oppgaveService.hentEnkeltOppgaveForReferanse(hendelse.id.toString())
                if (oppgaveForReferanse.manglerSaksbehandler()) {
                    oppgaveService.tildelSaksbehandler(oppgaveForReferanse.id, saksbehandler.ident)
                }
                oppgaveService.ferdigStillOppgaveUnderBehandling(
                    hendelse.id.toString(),
                    saksbehandler = saksbehandler,
                )
            } catch (e: Exception) {
                logger.error(
                    "Kunne ikke ferdigstille oppgaven for hendelsen på grunn av feil",
                    e,
                )
                throw KunneIkkeLukkeOppgaveForhendelse(e.message ?: "Kunne ikke ferdigstille oppgaven for hendelsen på grunn av feil")
            }
        }
    }

    fun settHendelseTilHistorisk(behandlingId: UUID) {
        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringHistorisk(behandlingId)
    }

    fun opprettBostedhendelse(bostedsadresse: Bostedsadresse): List<Grunnlagsendringshendelse> {
        return inTransaction { opprettHendelseAvTypeForPerson(bostedsadresse.fnr, GrunnlagsendringsType.BOSTED) }
    }

    fun opprettDoedshendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> {
        return inTransaction { opprettHendelseAvTypeForPerson(doedshendelse.fnr, GrunnlagsendringsType.DOEDSFALL) }
    }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse): List<Grunnlagsendringshendelse> {
        return inTransaction { opprettHendelseAvTypeForPerson(utflyttingsHendelse.fnr, GrunnlagsendringsType.UTFLYTTING) }
    }

    fun opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse): List<Grunnlagsendringshendelse> {
        return inTransaction {
            opprettHendelseAvTypeForPerson(
                forelderBarnRelasjonHendelse.fnr,
                GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            )
        }
    }

    fun opprettVergemaalEllerFremtidsfullmakt(
        vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt,
    ): List<Grunnlagsendringshendelse> {
        return inTransaction {
            opprettHendelseAvTypeForPerson(
                vergeMaalEllerFremtidsfullmakt.fnr,
                GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
            )
        }
    }

    fun opprettSivilstandHendelse(sivilstandHendelse: SivilstandHendelse): List<Grunnlagsendringshendelse> {
        return inTransaction {
            opprettHendelseAvTypeForPerson(
                sivilstandHendelse.fnr,
                GrunnlagsendringsType.SIVILSTAND,
            )
        }
    }

    fun opprettInstitusjonsOppholdhendelse(oppholdsHendelse: InstitusjonsoppholdHendelseBeriket): List<Grunnlagsendringshendelse> {
        return opprettHendelseInstitusjonsoppholdForPersonSjekketAvJobb(
            fnr = oppholdsHendelse.norskident,
            samsvar =
                SamsvarMellomKildeOgGrunnlag.INSTITUSJONSOPPHOLD(
                    samsvar = false,
                    oppholdstype = oppholdsHendelse.institusjonsoppholdsType,
                    oppholdBeriket = oppholdsHendelse,
                ),
        )
    }

    fun opprettEndretGrunnbeloepHendelse(sakId: Long): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForSak(
            sakId,
            GrunnlagsendringsType.GRUNNBELOEP,
        )
    }

    suspend fun oppdaterAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse) {
        val gradering = adressebeskyttelse.adressebeskyttelseGradering
        val sakIder = grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr)

        inTransaction {
            oppdaterEnheterForsaker(fnr = adressebeskyttelse.fnr, gradering = gradering)

            sakIder.forEach { sakId ->
                sakService.oppdaterAdressebeskyttelse(
                    sakId,
                    gradering,
                )
                sikkerLogg.info("Oppdaterte adressebeskyttelse for sakId=$sakId med gradering=$gradering")
            }
        }

        if (sakIder.isNotEmpty() && gradering != AdressebeskyttelseGradering.UGRADERT) {
            logger.error("Vi har en eller flere saker som er beskyttet med gradering ($gradering), se sikkerLogg.")
        }
    }

    fun oppdaterAdresseHendelse(bostedsadresse: Bostedsadresse) {
        logger.info("Oppretter manuell oppgave for Bosted")
        opprettBostedhendelse(bostedsadresse)
    }

    private fun oppdaterEnheterForsaker(
        fnr: String,
        gradering: AdressebeskyttelseGradering,
    ) {
        val finnSaker = sakService.finnSaker(fnr)
        val sakerMedNyEnhet =
            finnSaker.map {
                SakMedEnhet(it.id, finnEnhetFraGradering(fnr, gradering, it.sakType))
            }
        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet)
    }

    private fun finnEnhetFraGradering(
        fnr: String,
        gradering: AdressebeskyttelseGradering,
        sakType: SakType,
    ): String {
        return when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Enheter.STRENGT_FORTROLIG.enhetNr
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr
            AdressebeskyttelseGradering.FORTROLIG -> {
                brukerService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }

            AdressebeskyttelseGradering.UGRADERT -> {
                brukerService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }
        }
    }

    data class SakMedEnhet(val id: Long, val enhet: String)

    private fun opprettHendelseInstitusjonsoppholdForPersonSjekketAvJobb(
        fnr: String,
        samsvar: SamsvarMellomKildeOgGrunnlag,
    ): List<Grunnlagsendringshendelse> {
        val grunnlagendringType: GrunnlagsendringsType = GrunnlagsendringsType.INSTITUSJONSOPPHOLD
        val tidspunktForMottakAvHendelse = Tidspunkt.now().toLocalDatetimeUTC()

        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakerOgRoller }
        val sakerOgRollerGruppert = sakerOgRoller.distinct()

        val sakerForSoeker = sakerOgRollerGruppert.filter { Saksrolle.SOEKER == it.rolle }

        return sakerForSoeker.let {
            inTransaction {
                it.filter { rolleOgSak -> sakService.finnSak(rolleOgSak.sakId) != null }
                it.map { rolleOgSak ->
                    val hendelseId = UUID.randomUUID()
                    logger.info(
                        "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                            "type $grunnlagendringType på sak med id=${rolleOgSak.sakId}",
                    )
                    val hendelse =
                        Grunnlagsendringshendelse(
                            id = hendelseId,
                            sakId = rolleOgSak.sakId,
                            status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                            type = grunnlagendringType,
                            opprettet = tidspunktForMottakAvHendelse,
                            hendelseGjelderRolle = rolleOgSak.rolle,
                            gjelderPerson = fnr,
                            samsvarMellomKildeOgGrunnlag = samsvar,
                        )
                    oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                        referanse = hendelseId.toString(),
                        sakId = rolleOgSak.sakId,
                        oppgaveKilde = OppgaveKilde.HENDELSE,
                        oppgaveType = OppgaveType.VURDER_KONSEKVENS,
                        merknad = hendelse.beskrivelse(),
                    )
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(hendelse)
                }
            }
        }
    }

    fun opprettHendelseAvTypeForPerson(
        fnr: String,
        grunnlagendringType: GrunnlagsendringsType,
    ): List<Grunnlagsendringshendelse> {
        val grunnlagsEndringsStatus: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB
        val tidspunktForMottakAvHendelse = Tidspunkt.now().toLocalDatetimeUTC()
        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakerOgRoller }

        val sakerOgRollerGruppert = sakerOgRoller.distinct()

        return sakerOgRollerGruppert
            .filter { rolleOgSak -> sakService.finnSak(rolleOgSak.sakId) != null }
            .filter { rolleOgSak ->
                !hendelseEksistererFraFoer(
                    rolleOgSak.sakId,
                    fnr,
                    grunnlagendringType,
                )
            }
            .map { rolleOgSak ->
                val hendelseId = UUID.randomUUID()
                logger.info(
                    "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                        "type $grunnlagendringType på sak med id=${rolleOgSak.sakId}",
                )
                val hendelse =
                    Grunnlagsendringshendelse(
                        id = hendelseId,
                        sakId = rolleOgSak.sakId,
                        status = grunnlagsEndringsStatus,
                        type = grunnlagendringType,
                        opprettet = tidspunktForMottakAvHendelse,
                        hendelseGjelderRolle = rolleOgSak.rolle,
                        gjelderPerson = fnr,
                    )
                grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(hendelse)
            }
    }

    private fun opprettHendelseAvTypeForSak(
        sakId: Long,
        grunnlagendringType: GrunnlagsendringsType,
    ): List<Grunnlagsendringshendelse> {
        return inTransaction {
            if (hendelseEksistererFraFoer(sakId, null, grunnlagendringType)) {
                emptyList()
            } else {
                val hendelseId = UUID.randomUUID()
                logger.info(
                    "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                        "type $grunnlagendringType på sak med id=$sakId",
                )
                val hendelse =
                    Grunnlagsendringshendelse(
                        id = hendelseId,
                        sakId = sakId,
                        status = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                        type = grunnlagendringType,
                        opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                        hendelseGjelderRolle = Saksrolle.SOEKER,
                        gjelderPerson = sakService.finnSak(sakId)?.ident!!,
                    )
                listOf(
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(hendelse),
                )
            }
        }
    }

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) =
        inTransaction {
            grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(minutterGamle)
                .forEach { hendelse ->
                    try {
                        verifiserOgHaandterHendelse(hendelse)
                    } catch (e: Exception) {
                        logger.error(
                            "Kunne ikke sjekke opp for hendelsen med id=${hendelse.id} på sak ${hendelse.sakId} " +
                                "på grunn av feil",
                            e,
                        )
                    }
                }
        }

    private fun verifiserOgHaandterHendelse(hendelse: Grunnlagsendringshendelse) {
        val sak = sakService.finnSak(hendelse.sakId)!!
        val personRolle = hendelse.hendelseGjelderRolle.toPersonrolle(sak.sakType)
        val pdlData = pdltjenesterKlient.hentPdlModell(hendelse.gjelderPerson, personRolle, sak.sakType)
        val grunnlag =
            runBlocking {
                grunnlagKlient.hentGrunnlag(hendelse.sakId)
            }
        try {
            val samsvarMellomPdlOgGrunnlag = finnSamsvarForHendelse(hendelse, pdlData, grunnlag, personRolle, sak.sakType)
            if (!samsvarMellomPdlOgGrunnlag.samsvar) {
                oppdaterHendelseSjekket(hendelse, samsvarMellomPdlOgGrunnlag)
            } else {
                forkastHendelse(hendelse.id, samsvarMellomPdlOgGrunnlag)
            }
        } catch (e: GrunnlagRolleException) {
            forkastHendelse(hendelse.id, SamsvarMellomKildeOgGrunnlag.FeilRolle(pdlData, grunnlag, false))
        }
    }

    private fun oppdaterHendelseSjekket(
        hendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ) {
        sisteIkkeAvbrutteBehandlingUtenManueltOpphoer(hendelse.sakId, samsvarMellomKildeOgGrunnlag, hendelse.id)
            ?: return
        logger.info(
            "Grunnlagsendringshendelse for ${hendelse.type} med id ${hendelse.id} er naa sjekket av jobb " +
                "naa sjekket av jobb, og informasjonen i pdl og grunnlag samsvarer ikke. " +
                "Hendelsen forkastes derfor ikke.",
        )
        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = hendelse.id,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
            samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag,
        )
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = hendelse.id.toString(),
            sakId = hendelse.sakId,
            oppgaveKilde = OppgaveKilde.HENDELSE,
            oppgaveType = OppgaveType.VURDER_KONSEKVENS,
            merknad = hendelse.beskrivelse(),
        ).also {
            logger.info("Oppgave for hendelsen med id=${hendelse.id} er opprettet med id=${it.id}")
        }
    }

    private fun finnSamsvarForHendelse(
        hendelse: Grunnlagsendringshendelse,
        pdlData: PersonDTO,
        grunnlag: Grunnlag?,
        personRolle: PersonRolle,
        sakType: SakType,
    ): SamsvarMellomKildeOgGrunnlag {
        val rolle = hendelse.hendelseGjelderRolle
        val fnr = hendelse.gjelderPerson

        return when (hendelse.type) {
            GrunnlagsendringsType.DOEDSFALL -> {
                samsvarDoedsdatoer(
                    doedsdatoPdl = pdlData.hentDoedsdato(),
                    doedsdatoGrunnlag = grunnlag?.doedsdato(rolle, fnr)?.verdi,
                )
            }

            GrunnlagsendringsType.UTFLYTTING -> {
                samsvarUtflytting(
                    utflyttingPdl = pdlData.hentUtland(),
                    utflyttingGrunnlag = grunnlag?.utland(rolle, fnr),
                )
            }

            GrunnlagsendringsType.FORELDER_BARN_RELASJON -> {
                if (personRolle in listOf(PersonRolle.BARN, PersonRolle.TILKNYTTET_BARN)) {
                    samsvarAnsvarligeForeldre(
                        ansvarligeForeldrePdl = pdlData.hentAnsvarligeForeldre(),
                        ansvarligeForeldreGrunnlag = grunnlag?.ansvarligeForeldre(rolle, fnr),
                    )
                } else {
                    samsvarBarn(
                        barnPdl = pdlData.hentBarn(),
                        barnGrunnlag = grunnlag?.barn(rolle),
                    )
                }
            }

            GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT -> {
                val pdlVergemaal = pdlData.hentVergemaal()
                val grunnlagVergemaal = grunnlag?.vergemaalellerfremtidsfullmakt(rolle)
                SamsvarMellomKildeOgGrunnlag.VergemaalEllerFremtidsfullmaktForhold(
                    fraPdl = pdlVergemaal,
                    fraGrunnlag = grunnlagVergemaal,
                    samsvar = pdlVergemaal erLikRekkefoelgeIgnorert grunnlagVergemaal,
                )
            }

            GrunnlagsendringsType.SIVILSTAND -> {
                when (sakType) {
                    SakType.BARNEPENSJON -> samsvarSivilstandBP()
                    SakType.OMSTILLINGSSTOENAD -> {
                        val pdlSivilstand = pdlData.hentSivilstand()
                        val grunnlagSivilstand = grunnlag?.sivilstand(rolle)
                        samsvarSivilstandOMS(pdlSivilstand, grunnlagSivilstand)
                    }
                }
            }

            GrunnlagsendringsType.BOSTED -> {
                val pdlBosted = pdlData.hentBostedsadresse()
                val grunnlagBosted = grunnlag?.bostedsadresse(rolle, fnr)?.verdi
                samsvarBostedsadresse(pdlBosted, grunnlagBosted)
            }

            GrunnlagsendringsType.GRUNNBELOEP -> {
                SamsvarMellomKildeOgGrunnlag.Grunnbeloep(samsvar = false)
            }

            GrunnlagsendringsType.INSTITUSJONSOPPHOLD -> {
                throw IllegalStateException("Denne hendelsen skal gå rett til oppgavelisten og aldri komme hit")
            }
        }
    }

    private fun forkastHendelse(
        hendelseId: UUID,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ) {
        logger.info("Forkaster grunnlagsendringshendelse med id $hendelseId.")
        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusOgSamsvar(
            hendelseId = hendelseId,
            foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
            etterStatus = GrunnlagsendringStatus.FORKASTET,
            samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag,
        )
    }

    private fun sisteIkkeAvbrutteBehandlingUtenManueltOpphoer(
        sakId: Long,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
        hendelseId: UUID,
    ): Behandling? {
        val behandlingerISak = behandlingService.hentBehandlingerForSak(sakId)
        // Har vi en eksisterende behandling som ikke er avbrutt?
        val sisteBehandling =
            behandlingerISak
                .sisteIkkeAvbrutteBehandling()
                ?: run {
                    logger.info(
                        "Forkaster hendelse med id=$hendelseId fordi vi " +
                            "ikke har noen behandlinger som ikke er avbrutt",
                    )
                    forkastHendelse(hendelseId, samsvarMellomKildeOgGrunnlag)
                    return null
                }
        val harAlleredeEtManueltOpphoer = behandlingerISak.any { it.type == BehandlingType.MANUELT_OPPHOER }
        return if (harAlleredeEtManueltOpphoer) {
            logger.info("Forkaster hendelse med id=$hendelseId fordi vi har et manuelt opphør i saken")
            forkastHendelse(hendelseId, samsvarMellomKildeOgGrunnlag)
            null
        } else {
            sisteBehandling
        }
    }

    private fun hendelseEksistererFraFoer(
        sakId: Long,
        fnr: String?,
        hendelsesType: GrunnlagsendringsType,
    ): Boolean {
        return grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB),
        ).any {
            (fnr == null) || it.gjelderPerson == fnr && it.type == hendelsesType
        }
    }

    private fun List<Behandling>.sisteIkkeAvbrutteBehandling() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }
}
