package no.nav.etterlatte.grunnlagsendring

import institusjonsopphold.KafkaOppholdHendelse
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.common.klienter.hentAnsvarligeForeldre
import no.nav.etterlatte.common.klienter.hentBarn
import no.nav.etterlatte.common.klienter.hentDoedsdato
import no.nav.etterlatte.common.klienter.hentUtland
import no.nav.etterlatte.common.klienter.hentVergemaal
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.util.*

class GrunnlagsendringshendelseService(
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val generellBehandlingService: GenerellBehandlingService,
    private val pdlKlient: PdlKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val tilgangService: TilgangService,
    private val sakService: SakService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGyldigeHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sakId)
    }

    fun hentAlleHendelserForSak(sakId: Long) = inTransaction {
        logger.info("Henter alle relevante hendelser for sak $sakId")
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.relevantForSaksbehandler().toList()
        )
    }

    private fun ikkeVurderteHendelser(minutterGamle: Long): List<Grunnlagsendringshendelse> = inTransaction {
        grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            minutter = minutterGamle
        )
    }

    fun opprettDoedshendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForPerson(doedshendelse.avdoedFnr, GrunnlagsendringsType.DOEDSFALL)
    }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForPerson(utflyttingsHendelse.fnr, GrunnlagsendringsType.UTFLYTTING)
    }

    fun opprettForelderBarnRelasjonHendelse(
        forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse
    ): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForPerson(
            forelderBarnRelasjonHendelse.fnr,
            GrunnlagsendringsType.FORELDER_BARN_RELASJON
        )
    }

    fun opprettVergemaalEllerFremtidsfullmakt(
        vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt
    ): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForPerson(
            vergeMaalEllerFremtidsfullmakt.fnr,
            GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT
        )
    }

    fun opprettInstitusjonsOppholdhendelse(oppholdsHendelse: KafkaOppholdHendelse): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForPersonUtenSamsvarSjekketAvJobb(
            fnr = oppholdsHendelse.norskident,
            grunnlagendringType = GrunnlagsendringsType.INSTITUSJONSOPPHOLD,
            samsvar = SamsvarMellomKildeOgGrunnlag.INSTITUSJONSOPPHOLD(samsvar = false, type = oppholdsHendelse.type)
        )
    }

    fun opprettEndretGrunnbeloepHendelse(sakId: Long): List<Grunnlagsendringshendelse> {
        return opprettHendelseAvTypeForSak(
            sakId,
            GrunnlagsendringsType.GRUNNBELOEP
        )
    }

    suspend fun oppdaterAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse) {
        val gradering = adressebeskyttelse.adressebeskyttelseGradering
        val sakIder = grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr)

        oppdaterEnheterForsaker(fnr = adressebeskyttelse.fnr, gradering = gradering)

        sakIder.forEach { sakId ->
            tilgangService.oppdaterAdressebeskyttelse(
                sakId,
                gradering
            )
            sikkerLogg.info("Oppdaterte adressebeskyttelse for sakId=$sakId med gradering=$gradering")
        }

        if (sakIder.isNotEmpty() && gradering != AdressebeskyttelseGradering.UGRADERT) {
            logger.error("Vi har en eller flere saker som er beskyttet med gradering ($gradering), se sikkerLogg.")
        }
    }

    private fun oppdaterEnheterForsaker(fnr: String, gradering: AdressebeskyttelseGradering) {
        val finnSaker = sakService.finnSaker(fnr)
        val sakerMedNyEnhet = finnSaker.map {
            SakMedEnhet(it.id, finnEnhetFraGradering(fnr, gradering, it.sakType))
        }

        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
    }

    private fun finnEnhetFraGradering(fnr: String, gradering: AdressebeskyttelseGradering, sakType: SakType): String {
        return when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Enheter.STRENGT_FORTROLIG.enhetNr
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr
            AdressebeskyttelseGradering.FORTROLIG -> {
                sakService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }
            AdressebeskyttelseGradering.UGRADERT -> {
                sakService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }
        }
    }

    data class SakMedEnhet(val id: Long, val enhet: String)

    private fun opprettHendelseAvTypeForPersonUtenSamsvarSjekketAvJobb(
        fnr: String,
        grunnlagendringType: GrunnlagsendringsType,
        samsvar: SamsvarMellomKildeOgGrunnlag
    ): List<Grunnlagsendringshendelse> {
        val tidspunktForMottakAvHendelse = Tidspunkt.now().toLocalDatetimeUTC()

        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakerOgRoller }
        val sakerForSoeker = sakerOgRoller.filter { Saksrolle.SOEKER == it.rolle }

        return sakerForSoeker.let {
            inTransaction {
                it.filter { rolleOgSak ->
                    !hendelseEksistererFraFoer(
                        rolleOgSak.sakId,
                        fnr,
                        grunnlagendringType
                    )
                }
                    .map { rolleOgSak ->
                        val hendelseId = UUID.randomUUID()
                        logger.info(
                            "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                                "type $grunnlagendringType på sak med id=${rolleOgSak.sakId}"
                        )
                        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                            Grunnlagsendringshendelse(
                                id = hendelseId,
                                sakId = rolleOgSak.sakId,
                                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                                type = grunnlagendringType,
                                opprettet = tidspunktForMottakAvHendelse,
                                hendelseGjelderRolle = rolleOgSak.rolle,
                                gjelderPerson = fnr,
                                samsvarMellomKildeOgGrunnlag = samsvar
                            )
                        )
                    }
            }
        }
    }

    private fun opprettHendelseAvTypeForPerson(
        fnr: String,
        grunnlagendringType: GrunnlagsendringsType,
        grunnlagsEndringsStatus: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB
    ): List<Grunnlagsendringshendelse> {
        val tidspunktForMottakAvHendelse = Tidspunkt.now().toLocalDatetimeUTC()
        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakerOgRoller }

        return sakerOgRoller.let {
            inTransaction {
                it.filter { rolleOgSak ->
                    !hendelseEksistererFraFoer(
                        rolleOgSak.sakId,
                        fnr,
                        grunnlagendringType
                    )
                }
                    .map { rolleOgSak ->
                        val hendelseId = UUID.randomUUID()
                        logger.info(
                            "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                                "type $grunnlagendringType på sak med id=${rolleOgSak.sakId}"
                        )
                        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                            Grunnlagsendringshendelse(
                                id = hendelseId,
                                sakId = rolleOgSak.sakId,
                                status = grunnlagsEndringsStatus,
                                type = grunnlagendringType,
                                opprettet = tidspunktForMottakAvHendelse,
                                hendelseGjelderRolle = rolleOgSak.rolle,
                                gjelderPerson = fnr
                            )
                        )
                    }
            }
        }
    }

    private fun opprettHendelseAvTypeForSak(
        sakId: Long,
        grunnlagendringType: GrunnlagsendringsType,
        grunnlagsEndringsStatus: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB
    ): List<Grunnlagsendringshendelse> {
        return inTransaction {
            if (hendelseEksistererFraFoer(sakId, null, grunnlagendringType)) {
                emptyList()
            } else {
                val hendelseId = UUID.randomUUID()
                logger.info(
                    "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                        "type $grunnlagendringType på sak med id=$sakId"
                )
                listOf(
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                        Grunnlagsendringshendelse(
                            id = hendelseId,
                            sakId = sakId,
                            status = grunnlagsEndringsStatus,
                            type = grunnlagendringType,
                            opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                            hendelseGjelderRolle = Saksrolle.SOEKER,
                            gjelderPerson = sakService.finnSak(sakId)?.ident!!
                        )
                    )

                )
            }
        }
    }

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) {
        ikkeVurderteHendelser(minutterGamle)
            .forEach { hendelse ->
                try {
                    verifiserOgHaandterHendelse(hendelse)
                } catch (e: Exception) {
                    logger.error(
                        "Kunne ikke sjekke opp for hendelsen med id=${hendelse.id} på sak ${hendelse.sakId} " +
                            "på grunn av feil",
                        e
                    )
                }
            }
    }

    private fun verifiserOgHaandterHendelse(
        hendelse: Grunnlagsendringshendelse
    ) {
        val personRolle = hendelse.hendelseGjelderRolle.toPersonrolle()
        val sak = inTransaction {
            sakService.finnSak(hendelse.sakId)!!
        }
        val pdlData = pdlKlient.hentPdlModell(hendelse.gjelderPerson, personRolle, sak.sakType)
        val grunnlag = runBlocking {
            grunnlagKlient.hentGrunnlag(hendelse.sakId)
        }
        val samsvarMellomPdlOgGrunnlag = finnSamsvarForHendelse(hendelse, pdlData, grunnlag)
        if (!samsvarMellomPdlOgGrunnlag.samsvar) {
            oppdaterHendelseSjekket(hendelse, samsvarMellomPdlOgGrunnlag)
        } else {
            forkastHendelse(hendelse.id, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun oppdaterHendelseSjekket(
        hendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag
    ) {
        `siste ikke-avbrutte behandling uten manuelt opphoer`(hendelse.sakId, samsvarMellomKildeOgGrunnlag, hendelse.id)
            ?: return
        inTransaction {
            logger.info(
                "Grunnlagsendringshendelse for ${hendelse.type} med id ${hendelse.id} er naa sjekket av jobb " +
                    "naa sjekket av jobb, og informasjonen i pdl og grunnlag samsvarer ikke. " +
                    "Hendelsen forkastes derfor ikke."
            )
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelse.id,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag
            )
        }
    }

    private fun finnSamsvarForHendelse(
        hendelse: Grunnlagsendringshendelse,
        pdlData: PersonDTO,
        grunnlag: Grunnlag?
    ): SamsvarMellomKildeOgGrunnlag {
        val rolle = hendelse.hendelseGjelderRolle
        val fnr = hendelse.gjelderPerson

        return when (hendelse.type) {
            GrunnlagsendringsType.DOEDSFALL -> {
                samsvarDoedsdatoer(
                    doedsdatoPdl = pdlData.hentDoedsdato(),
                    doedsdatoGrunnlag = grunnlag?.doedsdato(rolle, fnr)?.verdi
                )
            }

            GrunnlagsendringsType.UTFLYTTING -> {
                samsvarUtflytting(
                    utflyttingPdl = pdlData.hentUtland(),
                    utflyttingGrunnlag = grunnlag?.utland(rolle, fnr)
                )
            }

            GrunnlagsendringsType.FORELDER_BARN_RELASJON -> {
                if (rolle.toPersonrolle() == PersonRolle.BARN) {
                    samsvarAnsvarligeForeldre(
                        ansvarligeForeldrePdl = pdlData.hentAnsvarligeForeldre(),
                        ansvarligeForeldreGrunnlag = grunnlag?.ansvarligeForeldre(rolle, fnr)
                    )
                } else {
                    samsvarBarn(
                        barnPdl = pdlData.hentBarn(),
                        barnGrunnlag = grunnlag?.barn(rolle)
                    )
                }
            }

            GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT -> {
                val pdlVergemaal = pdlData.hentVergemaal()
                val grunnlagVergemaal = grunnlag?.vergemaalellerfremtidsfullmakt(rolle)
                SamsvarMellomKildeOgGrunnlag.VergemaalEllerFremtidsfullmaktForhold(
                    fraPdl = pdlVergemaal,
                    fraGrunnlag = grunnlagVergemaal,
                    samsvar = pdlVergemaal erLikRekkefoelgeIgnorert grunnlagVergemaal
                )
            }
            GrunnlagsendringsType.GRUNNBELOEP -> SamsvarMellomKildeOgGrunnlag.Grunnbeloep(samsvar = false)
            GrunnlagsendringsType.INSTITUSJONSOPPHOLD ->
                throw IllegalStateException("Denne hendelsen skal gå rett til oppgavelisten og aldri komme hit")
        }
    }

    private fun forkastHendelse(hendelseId: UUID, samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag) =
        inTransaction {
            logger.info("Forkaster grunnlagsendringshendelse med id $hendelseId.")
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.FORKASTET,
                samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag
            )
        }

    private fun `siste ikke-avbrutte behandling uten manuelt opphoer`(
        sakId: Long,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
        hendelseId: UUID
    ): Behandling? {
        val behandlingerISak = generellBehandlingService.hentBehandlingerISak(sakId)
        // Har vi en eksisterende behandling som ikke er avbrutt?
        val sisteBehandling = behandlingerISak
            .`siste ikke-avbrutte behandling`()
            ?: run {
                logger.info(
                    "Forkaster hendelse med id=$hendelseId fordi vi " +
                        "ikke har noen behandlinger som ikke er avbrutt"
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
        hendelsesType: GrunnlagsendringsType
    ): Boolean {
        return grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
        ).any {
            (fnr == null) || it.gjelderPerson == fnr && it.type == hendelsesType
        }
    }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }
}