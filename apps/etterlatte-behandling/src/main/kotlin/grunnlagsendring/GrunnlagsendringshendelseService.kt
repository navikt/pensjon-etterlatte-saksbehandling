package no.nav.etterlatte.grunnlagsendring

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.Companion.iverksattEllerAttestert
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.Companion.underBehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.SamsvarMellomPdlOgGrunnlag
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class GrunnlagsendringshendelseService(
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val generellBehandlingService: GenerellBehandlingService,
    private val pdlService: PdlService,
    private val grunnlagClient: GrunnlagClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGyldigeHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserSomErSjekketAvJobb(sakId)
    }

    fun hentAlleHendelserForSak(sakId: Long) = inTransaction {
        logger.info("Henter alle hendelser for sak $sakId")
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.values().toList()
        )
    }

    private fun ikkeVurderteHendelser(minutterGamle: Long): List<Grunnlagsendringshendelse> = inTransaction {
        grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            minutter = minutterGamle
        )
    }

    fun opprettDoedshendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> =
        generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(doedshendelse.avdoedFnr)
            .let {
                inTransaction {
                    it.filter { rolleOgSak ->
                        !hendelseEksistererFraFoer(
                            rolleOgSak,
                            doedshendelse.avdoedFnr,
                            GrunnlagsendringsType.DOEDSFALL
                        )
                    }
                        .map { rolleOgSak ->
                            logger.info("Oppretter grunnlagsendringshendelse for doedshendelse: $doedshendelse")
                            grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                                Grunnlagsendringshendelse(
                                    id = UUID.randomUUID(),
                                    sakId = rolleOgSak.second,
                                    type = GrunnlagsendringsType.DOEDSFALL,
                                    opprettet = LocalDateTime.now(),
                                    hendelseGjelderRolle = rolleOgSak.first,
                                    gjelderPerson = doedshendelse.avdoedFnr
                                )
                            )
                        }
                }
            }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse): List<Grunnlagsendringshendelse> {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        return generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(utflyttingsHendelse.fnr).let {
            inTransaction {
                it.filter { rolleOgSak ->
                    !hendelseEksistererFraFoer(
                        rolleOgSak,
                        utflyttingsHendelse.fnr,
                        GrunnlagsendringsType.UTFLYTTING
                    )
                }
                    .map { rolleOgSak ->
                        logger.info(
                            "Oppretter grunnlagsendringshendelse for utflyttingshendelse: $utflyttingsHendelse"
                        )
                        grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                            Grunnlagsendringshendelse(
                                id = UUID.randomUUID(),
                                sakId = rolleOgSak.second,
                                type = GrunnlagsendringsType.UTFLYTTING,
                                opprettet = tidspunktForMottakAvHendelse,
                                hendelseGjelderRolle = rolleOgSak.first,
                                gjelderPerson = utflyttingsHendelse.fnr
                            )
                        )
                    }
            }
        }
    }

    fun opprettForelderBarnRelasjonHendelse(
        forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse
    ): List<Grunnlagsendringshendelse> {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        return generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(forelderBarnRelasjonHendelse.fnr)
            .let {
                inTransaction {
                    it.filter { rolleOgSak ->
                        !hendelseEksistererFraFoer(
                            rolleOgSak,
                            forelderBarnRelasjonHendelse.fnr,
                            GrunnlagsendringsType.FORELDER_BARN_RELASJON
                        )
                    }
                        .map { rolleOgSak ->
                            inTransaction {
                                logger.info(
                                    "Oppretter grunnlagsendringshendelse for forelder-barn-relasjon-hendelse: " +
                                        "$forelderBarnRelasjonHendelse"
                                )
                                grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                                    Grunnlagsendringshendelse(
                                        id = UUID.randomUUID(),
                                        sakId = rolleOgSak.second,
                                        type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
                                        opprettet = tidspunktForMottakAvHendelse,
                                        hendelseGjelderRolle = rolleOgSak.first,
                                        gjelderPerson = forelderBarnRelasjonHendelse.fnr
                                    )
                                )
                            }
                        }
                }
            }
    }

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) {
        ikkeVurderteHendelser(minutterGamle)
            .forEach { hendelse ->
                try {
                    when (hendelse.type) {
                        GrunnlagsendringsType.DOEDSFALL -> verifiserOgHaandterDoedsfall(
                            fnr = hendelse.gjelderPerson!!,
                            sakId = hendelse.sakId,
                            hendelseId = hendelse.id,
                            hendelseGjelderRolle = hendelse.hendelseGjelderRolle
                        )

                        GrunnlagsendringsType.UTFLYTTING -> verifiserOgHaandterSoekerErUtflyttet(
                            fnr = hendelse.gjelderPerson!!,
                            sakId = hendelse.sakId,
                            hendelseId = hendelse.id,
                            hendelseGjelderRolle = hendelse.hendelseGjelderRolle
                        )

                        GrunnlagsendringsType.FORELDER_BARN_RELASJON -> verifiserOgHaandterForelderBarnRelasjon(
                            fnr = hendelse.gjelderPerson!!,
                            sakId = hendelse.sakId,
                            hendelseId = hendelse.id,
                            hendelseGjelderRolle = hendelse.hendelseGjelderRolle
                        )
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Kunne ikke sjekke opp for hendelsen med id=${hendelse.id} på sak ${hendelse.sakId} " +
                            "på grunn av feil",
                        e
                    )
                }
            }
    }

    private fun verifiserOgHaandterDoedsfall(
        fnr: String,
        sakId: Long,
        hendelseId: UUID,
        hendelseGjelderRolle: Saksrolle
    ) {
        val personRolle = hendelseGjelderRolle.toPersonrolle()

        val doedsdatoPdl = pdlService.hentDoedsdato(fnr, personRolle)
        val doedsdatoGrunnlag = runBlocking {
            grunnlagClient.hentGrunnlag(sakId)?.doedsdato(hendelseGjelderRolle, fnr)?.verdi
        }
        val samsvarMellomPdlOgGrunnlag =
            samsvarDoedsdatoer(doedsdatoPdl = doedsdatoPdl, doedsdatoGrunnlag = doedsdatoGrunnlag)

        if (!samsvarMellomPdlOgGrunnlag.samsvar) {
            haandterDoedsfall(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
        } else {
            forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun verifiserOgHaandterSoekerErUtflyttet(
        fnr: String,
        sakId: Long,
        hendelseId: UUID,
        hendelseGjelderRolle: Saksrolle
    ) {
        val personRolle = hendelseGjelderRolle.toPersonrolle()
        val utlandPdl = pdlService.hentUtland(fnr, personRolle)
        val utlandGrunnlag =
            runBlocking { grunnlagClient.hentGrunnlag(sakId)?.utland(hendelseGjelderRolle, fnr) }

        val samsvarMellomPdlOgGrunnlag = samsvarUtflytting(utlandPdl, utlandGrunnlag)

        if (!samsvarMellomPdlOgGrunnlag.samsvar) {
            haandterSoekerErUtflyttet(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
        } else {
            forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun verifiserOgHaandterForelderBarnRelasjon(
        fnr: String,
        sakId: Long,
        hendelseId: UUID,
        hendelseGjelderRolle: Saksrolle
    ) {
        val samsvarMellomPdlOgGrunnlag = when (val personRolle = hendelseGjelderRolle.toPersonrolle()) {
            PersonRolle.BARN -> {
                val ansvarligeForeldrePDL = pdlService.hentAnsvarligeForeldre(fnr, personRolle)
                val ansvarligeForeldreGrunnlag =
                    runBlocking { grunnlagClient.hentGrunnlag(sakId)?.ansvarligeForeldre(hendelseGjelderRolle, fnr) }
                samsvarAnsvarligeForeldre(ansvarligeForeldrePDL, ansvarligeForeldreGrunnlag)
            }

            PersonRolle.GJENLEVENDE, PersonRolle.AVDOED -> {
                val barnPDL = pdlService.hentBarn(fnr, personRolle)
                val barnGrunnlag = runBlocking { grunnlagClient.hentGrunnlag(sakId)?.barn(hendelseGjelderRolle) }
                samsvarBarn(barnPDL, barnGrunnlag)
            }
        }

        if (!samsvarMellomPdlOgGrunnlag.samsvar) {
            haandterForelderBarnRelasjon(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
        } else {
            forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun haandterSoekerErUtflyttet(
        sakId: Long,
        samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag,
        hendelseId: UUID
    ) {
        val behandling =
            `siste ikke-avbrutte behandling uten manuelt opphoer`(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
                ?: return
        when (behandling.status) {
            in underBehandling() + iverksattEllerAttestert() -> {
                inTransaction {
                    logger.info(
                        "Grunnlagsendringshendelse for utflytting med id $hendelseId er naa sjekket av jobb " +
                            " naa sjekket av jobb, og informasjonen i pdl og grunnlag samsvarer ikke. " +
                            " Hendelsen forkastes derfor ikke."
                    )
                    grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                        hendelseId = hendelseId,
                        foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                        etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                        samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag
                    )
                }
            }

            else -> forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun haandterForelderBarnRelasjon(
        sakId: Long,
        samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag,
        hendelseId: UUID
    ) {
        val behandling =
            `siste ikke-avbrutte behandling uten manuelt opphoer`(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
                ?: return
        when (behandling.status) {
            in underBehandling() + iverksattEllerAttestert() -> {
                inTransaction {
                    logger.info(
                        "Grunnlagsendringshendelse for forelder-barn-relasjon med id $hendelseId er " +
                            " naa sjekket av jobb, og informasjonen i pdl og grunnlag samsvarer ikke. " +
                            " Hendelsen forkastes derfor ikke."
                    )
                    grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                        hendelseId = hendelseId,
                        foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                        etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                        samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag
                    )
                }
            }

            else -> {
                forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
            }
        }
    }

    private fun haandterDoedsfall(
        sakId: Long,
        samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag,
        hendelseId: UUID
    ) {
        val behandling =
            `siste ikke-avbrutte behandling uten manuelt opphoer`(sakId, samsvarMellomPdlOgGrunnlag, hendelseId)
                ?: return

        when (behandling.status) {
            in underBehandling() + iverksattEllerAttestert() -> {
                inTransaction {
                    logger.info(
                        "Grunnlagsendringshendelse for doedsfall med id $hendelseId er naa sjekket av jobb " +
                            "naa sjekket av jobb, og informasjonen i pdl og grunnlag samsvarer ikke. " +
                            "Hendelsen forkastes derfor ikke."
                    )
                    grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                        hendelseId = hendelseId,
                        foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                        etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                        samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag
                    )
                }
            }

            else -> forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
        }
    }

    private fun forkastHendelse(hendelseId: UUID, samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag) =
        inTransaction {
            logger.info("Forkaster grunnlagsendringshendelse med id $hendelseId.")
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.FORKASTET,
                samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag
            )
        }

    private fun `siste ikke-avbrutte behandling uten manuelt opphoer`(
        sakId: Long,
        samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag,
        hendelseId: UUID
    ): Behandling? {
        val behandlingerISak = generellBehandlingService.hentBehandlingerISak(sakId)
        // Har vi en eksisterende behandling som ikke er avbrutt?
        val sisteBehandling = behandlingerISak
            .`siste ikke-avbrutte behandling`()
            ?: run {
                forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
                return null
            }
        val harAlleredeEtManueltOpphoer = behandlingerISak.any { it.type == BehandlingType.MANUELT_OPPHOER }
        return if (harAlleredeEtManueltOpphoer) {
            forkastHendelse(hendelseId, samsvarMellomPdlOgGrunnlag)
            null
        } else {
            sisteBehandling
        }
    }

    private fun hendelseEksistererFraFoer(
        rolleOgSak: Pair<Saksrolle, Long>,
        fnr: String,
        hendelsesType: GrunnlagsendringsType
    ): Boolean {
        return grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            rolleOgSak.second,
            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
        ).any {
            it.gjelderPerson == fnr && it.type == hendelsesType
        }
    }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }
}