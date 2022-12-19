package no.nav.etterlatte.grunnlagsendring

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
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.Doedsfall
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.ForelderBarnRelasjon
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.Utflytting
import no.nav.etterlatte.libs.common.behandling.KorrektIPDL
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.erLik
import no.nav.etterlatte.pdl.PdlService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class GrunnlagsendringshendelseService(
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val generellBehandlingService: GenerellBehandlingService,
    private val pdlService: PdlService
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

    fun ikkeVurderteHendelser(minutterGamle: Long): List<Grunnlagsendringshendelse> = inTransaction {
        grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            minutter = minutterGamle
        )
    }

    fun opprettDoedshendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> =
        generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(doedshendelse.avdoedFnr)
            .let {
                inTransaction {
                    it.filter { rolleOgSak ->
                        val statuser =
                            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
                        val eksisterendeHendelserISak =
                            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                                rolleOgSak.second,
                                statuser
                            )
                        val hendelseEksistererFraFoer = eksisterendeHendelserISak.any {
                            when (val doedsfall = it.data) {
                                is Doedsfall -> doedsfall.hendelse.erLik(doedshendelse)
                                else -> false
                            }
                        }
                        return@filter !hendelseEksistererFraFoer
                    }
                        .map { rolleOgSak ->
                            logger.info("Oppretter grunnlagsendringshendelse for doedshendelse: $doedshendelse")
                            grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                                Grunnlagsendringshendelse(
                                    id = UUID.randomUUID(),
                                    sakId = rolleOgSak.second,
                                    type = GrunnlagsendringsType.DOEDSFALL,
                                    opprettet = LocalDateTime.now(),
                                    data = Doedsfall(hendelse = doedshendelse),
                                    hendelseGjelderRolle = rolleOgSak.first,
                                    korrektIPDL = KorrektIPDL.IKKE_SJEKKET
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
                    val statuser =
                        listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
                    val eksisterendeHendelserISak =
                        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                            rolleOgSak.second,
                            statuser
                        )
                    val hendelseEksistererFraFoer = eksisterendeHendelserISak.any {
                        when (val utflytting = it.data) {
                            is Utflytting -> utflytting.hendelse.erLik(utflyttingsHendelse)
                            else -> false
                        }
                    }
                    return@filter !hendelseEksistererFraFoer
                }
                    .map { rolleOgSak ->
                        inTransaction {
                            logger.info(
                                "Oppretter grunnlagsendringshendelse for utflyttingshendelse: $utflyttingsHendelse"
                            )
                            grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                                Grunnlagsendringshendelse(
                                    id = UUID.randomUUID(),
                                    sakId = rolleOgSak.second,
                                    type = GrunnlagsendringsType.UTFLYTTING,
                                    opprettet = tidspunktForMottakAvHendelse,
                                    data = Utflytting(hendelse = utflyttingsHendelse),
                                    hendelseGjelderRolle = rolleOgSak.first,
                                    korrektIPDL = KorrektIPDL.IKKE_SJEKKET
                                )
                            )
                        }
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
                        val statuser =
                            listOf(GrunnlagsendringStatus.VENTER_PAA_JOBB, GrunnlagsendringStatus.SJEKKET_AV_JOBB)
                        val eksisterendeHendelserISak =
                            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
                                rolleOgSak.second,
                                statuser
                            )
                        val hendelseEksistererFraFoer = eksisterendeHendelserISak.any {
                            when (val forelderbarnrelasjon = it.data) {
                                is ForelderBarnRelasjon -> forelderbarnrelasjon.hendelse.erLik(
                                    forelderBarnRelasjonHendelse
                                )
                                else -> false
                            }
                        }
                        return@filter !hendelseEksistererFraFoer
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
                                        data = ForelderBarnRelasjon(hendelse = forelderBarnRelasjonHendelse),
                                        hendelseGjelderRolle = rolleOgSak.first,
                                        korrektIPDL = KorrektIPDL.IKKE_SJEKKET
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
                    when (val data = hendelse.data) {
                        is Doedsfall -> verifiserOgHaandterDoedsfall(data, hendelse.sakId, hendelse.id)
                        is Utflytting -> verifiserOgHaandterSoekerErUtflyttet(data, hendelse.id)
                        is ForelderBarnRelasjon -> verifiserOgHaandterForelderBarnRelasjon(
                            data,
                            hendelse.id
                        )
                        null -> Unit
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

    private fun verifiserOgHaandterDoedsfall(data: Doedsfall, sakId: Long, hendelseId: UUID) {
        val fnr = data.hendelse.avdoedFnr
        val soekerErDoed = pdlService.personErDoed(fnr)
        haandterDoedsfall(sakId, soekerErDoed, hendelseId)
    }

    private fun verifiserOgHaandterSoekerErUtflyttet(data: Utflytting, hendelseId: UUID) {
        val fnr = data.hendelse.fnr
        val soekerHarUtflytting = pdlService.personHarUtflytting(fnr)
        haandterSoekerErUtflyttet(soekerHarUtflytting, hendelseId)
    }

    private fun verifiserOgHaandterForelderBarnRelasjon(data: ForelderBarnRelasjon, hendelseId: UUID) {
        val fnr = data.hendelse.fnr
        val forelderBarnRelasjonErGyldig = pdlService.forelderBarnRelasjonErGyldig(fnr)
        haandterForelderBarnRelasjon(forelderBarnRelasjonErGyldig, hendelseId)
    }

    private fun haandterSoekerErUtflyttet(korrektIPDL: KorrektIPDL, hendelseId: UUID) {
        inTransaction {
            logger.info(
                "Grunnlagsendringshendelse for utflytting med id $hendelseId er naa sjekket av jobb " +
                    "og har korrektIPDL-verdi: ${korrektIPDL.name}"
            )
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                korrektIPDL = korrektIPDL
            )
        }
    }

    private fun haandterForelderBarnRelasjon(korrektIPDL: KorrektIPDL, hendelseId: UUID) {
        inTransaction {
            logger.info(
                "Grunnlagsendringshendelse for forelder-barn-relasjon med id $hendelseId er naa sjekket av jobb " +
                    "og har korrektIPDL-verdi: ${korrektIPDL.name}"
            )
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                korrektIPDL = korrektIPDL
            )
        }
    }

    private fun haandterDoedsfall(sakId: Long, korrektIPDL: KorrektIPDL, hendelseId: UUID) {
        val behandlingerISak = generellBehandlingService.hentBehandlingerISak(sakId)

        // Har vi en eksisterende behandling som ikke er avbrutt?
        val sisteBehandling = behandlingerISak
            .`siste ikke-avbrutte behandling`()
            ?: run {
                forkastHendelse(hendelseId, korrektIPDL)
                return
            }

        val harAlleredeEtManueltOpphoer = behandlingerISak.any { it.type == BehandlingType.MANUELT_OPPHOER }
        if (harAlleredeEtManueltOpphoer) {
            forkastHendelse(hendelseId, korrektIPDL)
            return
        }

        when (sisteBehandling.status) {
            in underBehandling() + iverksattEllerAttestert() -> {
                inTransaction {
                    logger.info(
                        "Grunnlagsendringshendelse for doedsfall med id $hendelseId er naa sjekket av jobb " +
                            "og har korrektIPDL-verdi: ${korrektIPDL.name}"
                    )
                    grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                        hendelseId = hendelseId,
                        foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                        etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                        korrektIPDL = korrektIPDL
                    )
                }
            }
            else -> forkastHendelse(hendelseId, korrektIPDL)
        }
    }

    private fun forkastHendelse(hendelseId: UUID, korrektIPDL: KorrektIPDL) =
        inTransaction {
            logger.info("Forkaster grunnlagsendringshendelse med id $hendelseId.")
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatus(
                hendelseId = hendelseId,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.FORKASTET,
                korrektIPDL = korrektIPDL
            )
        }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }
}