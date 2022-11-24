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
        generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(doedshendelse.avdoedFnr).map { rolleOgSak ->
            inTransaction {
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

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse) {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(utflyttingsHendelse.fnr).map { rolleOgSak ->
            inTransaction {
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

    fun opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse) {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        generellBehandlingService.hentSakerOgRollerMedFnrIPersongalleri(forelderBarnRelasjonHendelse.fnr)
            .map { rolleOgSak ->
                inTransaction {
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

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) {
        ikkeVurderteHendelser(minutterGamle)
            .forEach { hendelse ->
                when (val data = hendelse.data) {
                    is Doedsfall -> verifiserOgHaandterDoedsfall(data, hendelse.sakId, hendelse.id)
                    is Utflytting -> verifiserOgHaandterSoekerErUtflyttet(data, hendelse.id)
                    is ForelderBarnRelasjon -> verifiserOgHaandterForelderBarnRelasjon(data, hendelse.id)
                    null -> Unit
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