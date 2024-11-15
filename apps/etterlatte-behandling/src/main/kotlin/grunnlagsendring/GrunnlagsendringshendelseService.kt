package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Folkeregisteridentifikatorhendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt.Companion.now
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagsendringshendelseService(
    private val oppgaveService: OppgaveService,
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val behandlingService: BehandlingService,
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val sakService: SakService,
    private val brukerService: BrukerService,
    private val doedshendelseService: DoedshendelseService,
    private val grunnlagsendringsHendelseFilter: GrunnlagsendringsHendelseFilter,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGyldigeHendelserForSak(sakId: SakId) = grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sakId)

    fun hentAlleHendelserForSak(sakId: SakId): List<Grunnlagsendringshendelse> =
        grunnlagsendringshendelseDao
            .hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                GrunnlagsendringStatus.relevantForSaksbehandler(),
            ).also {
                logger.info("Hentet alle relevante hendelser for sak $sakId antall: ${it.size}")
            }

    fun hentAlleHendelserForSakAvType(
        sakId: SakId,
        type: GrunnlagsendringsType,
    ) = inTransaction {
        grunnlagsendringshendelseDao
            .hentGrunnlagsendringshendelserMedStatuserISakAvType(
                sakId,
                GrunnlagsendringStatus.relevantForSaksbehandler(),
                type,
            ).also {
                logger.info("Hentet alle relevante hendelser for sak $sakId av type $type antall hendelser ${it.size}")
            }
    }

    fun arkiverHendelseMedKommentar(
        hendelse: Grunnlagsendringshendelse,
        saksbehandler: Saksbehandler,
    ) {
        logger.info("Arkiverer hendelse med id ${hendelse.id}")

        inTransaction {
            grunnlagsendringshendelseDao.arkiverGrunnlagsendringStatus(hendelse = hendelse)

            oppgaveService.ferdigStillOppgaveUnderBehandling(
                referanse = hendelse.id.toString(),
                type = OppgaveType.VURDER_KONSEKVENS,
                saksbehandler = saksbehandler,
            )
        }
    }

    fun settHendelseTilHistorisk(behandlingId: UUID) {
        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringHistorisk(behandlingId)
    }

    private fun opprettBostedhendelse(bostedsadresse: Bostedsadresse): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(bostedsadresse.fnr, GrunnlagsendringsType.BOSTED)
        }

    fun opprettDoedshendelse(doedshendelse: DoedshendelsePdl) {
        doedshendelseService.opprettDoedshendelseForBeroertePersoner(doedshendelse)
    }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(
                utflyttingsHendelse.fnr,
                GrunnlagsendringsType.UTFLYTTING,
            )
        }

    fun opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(
                forelderBarnRelasjonHendelse.fnr,
                GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            )
        }

    fun opprettVergemaalEllerFremtidsfullmakt(
        vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt,
    ): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(
                vergeMaalEllerFremtidsfullmakt.fnr,
                GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
            )
        }

    fun opprettSivilstandHendelse(sivilstandHendelse: SivilstandHendelse): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(
                sivilstandHendelse.fnr,
                GrunnlagsendringsType.SIVILSTAND,
            )
        }

    fun opprettFolkeregisteridentifikatorhendelse(hendelse: Folkeregisteridentifikatorhendelse): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForPerson(
                hendelse.fnr,
                GrunnlagsendringsType.FOLKEREGISTERIDENTIFIKATOR,
            )
        }

    fun opprettInstitusjonsOppholdhendelse(oppholdsHendelse: InstitusjonsoppholdHendelseBeriket): List<Grunnlagsendringshendelse> =
        opprettHendelseAvTypeForPersonMedSamsvar(
            fnr = oppholdsHendelse.norskident,
            type = GrunnlagsendringsType.INSTITUSJONSOPPHOLD,
            samsvar =
                SamsvarMellomKildeOgGrunnlag.INSTITUSJONSOPPHOLD(
                    samsvar = false,
                    oppholdstype = oppholdsHendelse.institusjonsoppholdsType,
                    oppholdBeriket = oppholdsHendelse,
                ),
        )

    fun opprettUfoerehendelse(hendelse: UfoereHendelse): List<Grunnlagsendringshendelse> =
        opprettHendelseAvTypeForPersonMedSamsvar(
            fnr = hendelse.personIdent,
            type = GrunnlagsendringsType.UFOERETRYGD,
            samsvar =
                SamsvarMellomKildeOgGrunnlag.Ufoeretrygd(
                    samsvar = false,
                    hendelse = hendelse,
                ),
        )

    fun opprettEndretGrunnbeloepHendelse(sakId: SakId): List<Grunnlagsendringshendelse> =
        inTransaction {
            opprettHendelseAvTypeForSak(
                sakId,
                GrunnlagsendringsType.GRUNNBELOEP,
            )
        }

    suspend fun oppdaterAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse) {
        val gradering = adressebeskyttelse.adressebeskyttelseGradering
        val sakIder = grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr)
        inTransaction {
            val (eksisterendeSaker, manglendeSaker) =
                sakIder
                    .map { it to sakService.finnSak(it) }
                    .partition { it.second != null }

            val faktiskeSaker = eksisterendeSaker.map { it.second!! }

            if (manglendeSaker.isNotEmpty()) {
                logger.error("Sakider som ikke finnes lenger ${manglendeSaker.map { it.first }.joinToString { "," }}")
            }

            oppdaterEnheterForsaker(fnr = adressebeskyttelse.fnr, gradering = gradering, saker = faktiskeSaker)

            faktiskeSaker.forEach { sak ->
                sakService.oppdaterAdressebeskyttelse(
                    sak.id,
                    gradering,
                )
                sikkerLogg.info("Oppdaterte adressebeskyttelse for sakId=$sak med gradering=$gradering")
            }
        }

        if (sakIder.isNotEmpty() &&
            gradering in listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)
        ) {
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
        saker: List<Sak>,
    ) {
        val sakerMedNyEnhet =
            saker.map {
                SakMedEnhet(it.id, finnEnhetFraGradering(fnr, gradering, it.sakType))
            }
        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet)
    }

    private fun finnEnhetFraGradering(
        fnr: String,
        gradering: AdressebeskyttelseGradering,
        sakType: SakType,
    ): Enhetsnummer =
        when (gradering) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Enheter.STRENGT_FORTROLIG.enhetNr
            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr
            AdressebeskyttelseGradering.FORTROLIG -> {
                brukerService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }

            AdressebeskyttelseGradering.UGRADERT -> {
                brukerService.finnEnhetForPersonOgTema(fnr, sakType.tema, sakType).enhetNr
            }
        }

    fun opprettDoedshendelseForPerson(grunnlagsendringshendelse: Grunnlagsendringshendelse): OppgaveIntern {
        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(grunnlagsendringshendelse)
        return opprettOppgave(grunnlagsendringshendelse)
    }

    private fun opprettHendelseAvTypeForPersonMedSamsvar(
        fnr: String,
        type: GrunnlagsendringsType,
        samsvar: SamsvarMellomKildeOgGrunnlag,
    ): List<Grunnlagsendringshendelse> =
        inTransaction {
            runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakiderOgRoller }
                .asSequence()
                .distinct()
                .filter(::harRolleSoeker)
                .filter(::harSakIGjenny)
                .filter { grunnlagsendringsHendelseFilter.hendelseErRelevantForSak(it.sakId, type) }
                .map { sakIdOgRolle ->
                    val sakId = sakIdOgRolle.sakId
                    val hendelseId = UUID.randomUUID()

                    logger.info("Oppretter grunnlagsendringshendelse med id=$hendelseId for hendelse av type=$type på sak=$sakId")

                    grunnlagsendringshendelseDao
                        .opprettGrunnlagsendringshendelse(
                            Grunnlagsendringshendelse(
                                id = hendelseId,
                                sakId = sakId,
                                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                                type = type,
                                opprettet = now().toLocalDatetimeUTC(),
                                hendelseGjelderRolle = sakIdOgRolle.rolle,
                                gjelderPerson = fnr,
                                samsvarMellomKildeOgGrunnlag = samsvar,
                            ),
                        ).let { hendelse ->
                            logger.info("Oppretter oppgave for grunnlagsendringshendelse med id=$hendelseId")

                            val oppgave =
                                oppgaveService.opprettOppgave(
                                    referanse = hendelseId.toString(),
                                    sakId = sakId,
                                    kilde = OppgaveKilde.HENDELSE,
                                    type = OppgaveType.VURDER_KONSEKVENS,
                                    merknad = hendelse.beskrivelse(),
                                )

                            logger.info("Oppgave med id=${oppgave.id} opprettet for grunnlagsendringshendelse med id=$hendelseId")

                            hendelse
                        }
                }.toList()
        }

    private fun harSakIGjenny(rolleOgSak: SakidOgRolle) = sakService.finnSak(rolleOgSak.sakId) != null

    private fun harRolleSoeker(it: SakidOgRolle) = Saksrolle.SOEKER == it.rolle

    data class SakOgRolle(
        val sak: Sak,
        val sakiderOgRolle: SakidOgRolle,
    )

    fun opprettHendelseAvTypeForPerson(
        fnr: String,
        grunnlagendringType: GrunnlagsendringsType,
    ): List<Grunnlagsendringshendelse> {
        val tidspunktForMottakAvHendelse = now().toLocalDatetimeUTC()
        val sakerOgRoller = runBlocking { grunnlagKlient.hentPersonSakOgRolle(fnr).sakiderOgRoller }

        val sakerOgRollerGruppert = sakerOgRoller.distinct()

        return sakerOgRollerGruppert
            .map { sakiderOgRoller -> Pair(sakService.finnSak(sakiderOgRoller.sakId), sakiderOgRoller) }
            .filter { rollerogSak -> rollerogSak.first != null }
            .map { SakOgRolle(it.first!!, it.second) }
            .filter { rollerogSak ->
                if (grunnlagendringType == GrunnlagsendringsType.SIVILSTAND) {
                    rollerogSak.sak.sakType != SakType.BARNEPENSJON
                } else {
                    true
                }
            }.filter { grunnlagsendringsHendelseFilter.hendelseErRelevantForSak(it.sak.id, grunnlagendringType) }
            .map { rolleOgSak ->
                val hendelse =
                    Grunnlagsendringshendelse(
                        id = UUID.randomUUID(),
                        sakId = rolleOgSak.sak.id,
                        type = grunnlagendringType,
                        opprettet = tidspunktForMottakAvHendelse,
                        hendelseGjelderRolle = rolleOgSak.sakiderOgRolle.rolle,
                        gjelderPerson = fnr,
                    )
                logger.info(
                    "Oppretter grunnlagsendringshendelse med id=${hendelse.id} for hendelse av " +
                        "type $grunnlagendringType på sak med id=${rolleOgSak.sak.id}",
                )
                hendelse to rolleOgSak
            }.onEach {
                verifiserOgHaandterHendelse(it.first, it.second.sak)
            }.map { it.first }
    }

    private fun opprettHendelseAvTypeForSak(
        sakId: SakId,
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
                type = grunnlagendringType,
                opprettet = now().toLocalDatetimeUTC(),
                hendelseGjelderRolle = Saksrolle.SOEKER,
                gjelderPerson = sak?.ident!!,
            )
        return listOf(hendelse to sak)
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
        val grunnlag = runBlocking { grunnlagKlient.hentGrunnlag(sak.id) }
        try {
            val samsvarMellomPdlOgGrunnlag =
                finnSamsvarForHendelse(grunnlagsendringshendelse, pdlData, grunnlag, personRolle, sak.sakType)
            val erDuplikat =
                erDuplikatHendelse(
                    sak.id,
                    grunnlagsendringshendelse,
                    samsvarMellomPdlOgGrunnlag,
                )

            if (!samsvarMellomPdlOgGrunnlag.samsvar) {
                if (erDuplikat) {
                    forkastHendelse(grunnlagsendringshendelse, samsvarMellomPdlOgGrunnlag)
                } else {
                    opprettRelevantHendelse(grunnlagsendringshendelse, samsvarMellomPdlOgGrunnlag)
                }
            } else {
                forkastHendelse(grunnlagsendringshendelse, samsvarMellomPdlOgGrunnlag)
            }
        } catch (e: GrunnlagRolleException) {
            forkastHendelse(
                grunnlagsendringshendelse,
                SamsvarMellomKildeOgGrunnlag.FeilRolle(pdlData, grunnlag, false),
            )
        }
    }

    private fun opprettRelevantHendelse(
        hendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ) {
        val harKunAvbrutteBehandlinger =
            hendelseHarKunAvbrytteBehandlinger(hendelse)
        if (harKunAvbrutteBehandlinger) {
            forkastHendelse(hendelse, samsvarMellomKildeOgGrunnlag)
        } else {
            logger.info(
                "Grunnlagsendringshendelse for ${hendelse.type} med id ${hendelse.id} er naa sjekket " +
                    "og informasjonen i pdl og grunnlag samsvarer ikke. " +
                    "Hendelsen vises derfor til saksbehandler.",
            )
            grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                hendelse.copy(samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag, status = GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
            opprettOppgave(hendelse)
        }
    }

    private fun opprettOppgave(hendelse: Grunnlagsendringshendelse): OppgaveIntern =
        oppgaveService
            .opprettOppgave(
                referanse = hendelse.id.toString(),
                sakId = hendelse.sakId,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.VURDER_KONSEKVENS,
                merknad = hendelse.beskrivelse(),
            ).also {
                logger.info("Oppgave for hendelsen med id=${hendelse.id} er opprettet med id=${it.id}")
            }

    internal fun forkastHendelse(
        hendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ) {
        logger.info("Forkaster grunnlagsendringshendelse med id ${hendelse.id}.")
        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
            hendelse.copy(samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag, status = GrunnlagsendringStatus.FORKASTET),
        )
    }

    private fun hendelseHarKunAvbrytteBehandlinger(hendelse: Grunnlagsendringshendelse): Boolean {
        val behandlingerISak = behandlingService.hentBehandlingerForSak(hendelse.sakId)

        return behandlingerISak.all { it.status == BehandlingStatus.AVBRUTT }
    }

    internal fun erDuplikatHendelse(
        sakId: SakId,
        grunnlagsendringshendelse: Grunnlagsendringshendelse,
        samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag,
    ): Boolean {
        val relevanteHendelser =
            grunnlagsendringshendelseDao
                .hentGrunnlagsendringshendelserMedStatuserISak(
                    sakId,
                    listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
                ).filter {
                    it.gjelderPerson == grunnlagsendringshendelse.gjelderPerson && it.type == grunnlagsendringshendelse.type
                }
        logger.info(
            "Hendelser på samme sakid $sakId antall ${relevanteHendelser.size} " +
                "fnr: ${grunnlagsendringshendelse.gjelderPerson.maskerFnr()} grlid: ${grunnlagsendringshendelse.id}",
        )
        return relevanteHendelser.any { it.samsvarMellomKildeOgGrunnlag == samsvarMellomKildeOgGrunnlag }
    }
}
