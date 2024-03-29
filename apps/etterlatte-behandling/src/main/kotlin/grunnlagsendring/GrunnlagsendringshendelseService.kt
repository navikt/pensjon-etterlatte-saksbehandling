package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
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
    private val doedshendelseService: DoedshendelseService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGyldigeHendelserForSak(sakId: Long) = grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sakId)

    fun hentAlleHendelserForSak(sakId: Long): List<Grunnlagsendringshendelse> {
        logger.info("Henter alle relevante hendelser for sak $sakId")
        return grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.relevantForSaksbehandler(),
        )
    }

    fun hentAlleHendelserForSakAvType(
        sakId: Long,
        type: GrunnlagsendringsType,
    ) = inTransaction {
        logger.info("Henter alle relevante hendelser for sak $sakId")
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISakAvType(
            sakId,
            GrunnlagsendringStatus.relevantForSaksbehandler(),
            type,
        )
    }

    fun lukkHendelseMedKommentar(
        hendelse: Grunnlagsendringshendelse,
        saksbehandler: Saksbehandler,
    ) {
        logger.info("Lukker hendelse med id ${hendelse.id}")

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
                throw KunneIkkeLukkeOppgaveForhendelse(
                    e.message ?: "Kunne ikke ferdigstille oppgaven for hendelsen på grunn av feil",
                )
            }
        }
    }

    fun settHendelseTilHistorisk(behandlingId: UUID) {
        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringHistorisk(behandlingId)
    }

    private fun opprettBostedhendelse(bostedsadresse: Bostedsadresse): List<Grunnlagsendringshendelse> {
        return inTransaction { opprettHendelseAvTypeForPerson(bostedsadresse.fnr, GrunnlagsendringsType.BOSTED) }
    }

    fun opprettDoedshendelse(doedshendelse: DoedshendelsePdl): List<Grunnlagsendringshendelse> {
        if (doedshendelseService.kanBrukeDeodshendelserJob()) {
            try {
                doedshendelseService.opprettDoedshendelseForBeroertePersoner(doedshendelse)
            } catch (e: Exception) {
                logger.error("Noe gikk galt ved opprettelse av dødshendelse i behandling.", e)
            }
        }

        if (!doedshendelseService.kanSendeBrevOgOppretteOppgave()) {
            return inTransaction {
                opprettHendelseAvTypeForPerson(doedshendelse.fnr, GrunnlagsendringsType.DOEDSFALL)
            }
        }

        return emptyList()
    }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse): List<Grunnlagsendringshendelse> {
        return inTransaction {
            opprettHendelseAvTypeForPerson(
                utflyttingsHendelse.fnr,
                GrunnlagsendringsType.UTFLYTTING,
            )
        }
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
        return inTransaction {
            opprettHendelseAvTypeForSak(
                sakId,
                GrunnlagsendringsType.GRUNNBELOEP,
            )
        }
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

        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakiderOgRoller }
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

    data class SakOgRolle(
        val sak: Sak,
        val sakiderOgRolle: SakidOgRolle,
    )

    fun opprettHendelseAvTypeForPerson(
        fnr: String,
        grunnlagendringType: GrunnlagsendringsType,
    ): List<Grunnlagsendringshendelse> {
        val grunnlagsEndringsStatus: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB
        val tidspunktForMottakAvHendelse = Tidspunkt.now().toLocalDatetimeUTC()
        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakiderOgRoller }

        val sakerOgRollerGruppert = sakerOgRoller.distinct()

        return sakerOgRollerGruppert
            .map { sakiderOgRoller -> Pair(sakService.finnSak(sakiderOgRoller.sakId), sakiderOgRoller) }
            .filter { rollerogSak -> rollerogSak.first != null }
            .map { SakOgRolle(it.first!!, it.second) }
            .map { rolleOgSak ->
                val hendelse =
                    Grunnlagsendringshendelse(
                        id = UUID.randomUUID(),
                        sakId = rolleOgSak.sak.id,
                        status = grunnlagsEndringsStatus,
                        type = grunnlagendringType,
                        opprettet = tidspunktForMottakAvHendelse,
                        hendelseGjelderRolle = rolleOgSak.sakiderOgRolle.rolle,
                        gjelderPerson = fnr,
                    )
                logger.info(
                    "Oppretter grunnlagsendringshendelse med id=${hendelse.id} for hendelse av " +
                        "type $grunnlagendringType på sak med id=${rolleOgSak.sak.id}",
                )
                grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(hendelse) to rolleOgSak
            }.onEach {
                verifiserOgHaandterHendelse(it.first, it.second.sak)
            }.map { it.first }
    }

    fun opprettDoedshendelseForPerson(grunnlagsendringshendelse: Grunnlagsendringshendelse): OppgaveIntern {
        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(grunnlagsendringshendelse)
        return opprettOppgave(grunnlagsendringshendelse)
    }

    private fun opprettHendelseAvTypeForSak(
        sakId: Long,
        grunnlagendringType: GrunnlagsendringsType,
    ): List<Grunnlagsendringshendelse> {
        val hendelseId = UUID.randomUUID()
        logger.info(
            "Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av " +
                "type $grunnlagendringType på sak med id=$sakId",
        )

        val sak = sakService.finnSak(sakId)
        val hendelse =
            Grunnlagsendringshendelse(
                id = hendelseId,
                sakId = sakId,
                status = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                type = grunnlagendringType,
                opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                hendelseGjelderRolle = Saksrolle.SOEKER,
                gjelderPerson = sak?.ident!!,
            )
        return listOf(grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(hendelse) to sak)
            .onEach {
                verifiserOgHaandterHendelse(it.first, it.second)
            }.map { it.first }
    }

    private fun verifiserOgHaandterHendelse(
        grunnlagsendringshendelse: Grunnlagsendringshendelse,
        sak: Sak,
    ) {
        val personRolle = grunnlagsendringshendelse.hendelseGjelderRolle.toPersonrolle(sak.sakType)
        val pdlData =
            pdltjenesterKlient.hentPdlModellFlereSaktyper(
                grunnlagsendringshendelse.gjelderPerson,
                personRolle,
                sak.sakType,
            )
        val grunnlag =
            runBlocking {
                grunnlagKlient.hentGrunnlag(sak.id)
            }
        try {
            val samsvarMellomPdlOgGrunnlag =
                finnSamsvarForHendelse(grunnlagsendringshendelse, pdlData, grunnlag, personRolle, sak.sakType)
            val erDuplikat =
                erDuplikatHendelse(sak.id, sak.ident, grunnlagsendringshendelse.type, samsvarMellomPdlOgGrunnlag)

            if (!samsvarMellomPdlOgGrunnlag.samsvar) {
                if (erDuplikat) {
                    forkastHendelse(grunnlagsendringshendelse.id, samsvarMellomPdlOgGrunnlag)
                } else {
                    oppdaterHendelseSjekket(grunnlagsendringshendelse, samsvarMellomPdlOgGrunnlag)
                }
            } else {
                forkastHendelse(grunnlagsendringshendelse.id, samsvarMellomPdlOgGrunnlag)
            }
        } catch (e: GrunnlagRolleException) {
            forkastHendelse(
                grunnlagsendringshendelse.id,
                SamsvarMellomKildeOgGrunnlag.FeilRolle(pdlData, grunnlag, false),
            )
        }
    }

    internal fun oppdaterHendelseSjekket(
        hendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ) {
        val bleIkkeForkastet =
            forkastHendelseHvisKunAvbrytteBehandlinger(hendelse.sakId, samsvarMellomKildeOgGrunnlag, hendelse.id)
        if (bleIkkeForkastet) {
            logger.info(
                "Grunnlagsendringshendelse for ${hendelse.type} med id ${hendelse.id} er naa sjekket " +
                    "og informasjonen i pdl og grunnlag samsvarer ikke. " +
                    "Hendelsen vises derfor til saksbehandler.",
            )
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusOgSamsvar(
                hendelseId = hendelse.id,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag,
            )
            opprettOppgave(hendelse)
        }
    }

    private fun opprettOppgave(hendelse: Grunnlagsendringshendelse): OppgaveIntern {
        return oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = hendelse.id.toString(),
            sakId = hendelse.sakId,
            oppgaveKilde = OppgaveKilde.HENDELSE,
            oppgaveType = OppgaveType.VURDER_KONSEKVENS,
            merknad = hendelse.beskrivelse(),
        ).also {
            logger.info("Oppgave for hendelsen med id=${hendelse.id} er opprettet med id=${it.id}")
        }
    }

    internal fun forkastHendelse(
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

    private fun forkastHendelseHvisKunAvbrytteBehandlinger(
        sakId: Long,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
        hendelseId: UUID,
    ): Boolean {
        val behandlingerISak = behandlingService.hentBehandlingerForSak(sakId)

        val kunAvbrutteBehandlinger = behandlingerISak.all { it.status == BehandlingStatus.AVBRUTT }
        if (kunAvbrutteBehandlinger) {
            logger.info(
                "Forkaster hendelse med id=$hendelseId fordi vi " +
                    "ikke har noen behandlinger som ikke er avbrutt",
            )
            forkastHendelse(hendelseId, samsvarMellomKildeOgGrunnlag)
            return false
        } else {
            return true
        }
    }

    internal fun erDuplikatHendelse(
        sakId: Long,
        fnr: String?,
        hendelsesType: GrunnlagsendringsType,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ): Boolean {
        val relevanteHendelser =
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            ).filter {
                (fnr == null) || it.gjelderPerson == fnr && it.type == hendelsesType
            }

        return relevanteHendelser.any { it.samsvarMellomKildeOgGrunnlag == samsvarMellomKildeOgGrunnlag }
    }
}
