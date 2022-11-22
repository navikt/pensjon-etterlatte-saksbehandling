package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
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
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class GrunnlagsendringshendelseService(
    private val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    private val generellBehandlingService: GenerellBehandlingService,
    private val revurderingService: RevurderingService,
    private val pdlService: PdlService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /* Henter grunnlagsendringshendelser med status SJEKKET_AV_JOBB */
    /* TODO: gjør slik at dersom det ligger en ikke-klar hendelse på saken så hentes ingen hendelser*/
    /* fiks også test: hentGyldigeGrunnlagsendringshendelserISak skal hente alle grunnlagsendringshendelser med...*/
    fun hentGyldigeHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGyldigeGrunnlagsendringshendelserISak(sakId)
    }

    fun hentAlleHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.values().toList()
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

    fun ikkeVurderteHendelser(minutterGamle: Long): List<Grunnlagsendringshendelse> = inTransaction {
        grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
            minutter = minutterGamle
        )
    }

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) {
        ikkeVurderteHendelser(minutterGamle)
            .forEach { hendelse ->
                when (val data = hendelse.data) {
                    is Doedsfall -> verifiserOgHaandterDoedsfall(data, hendelse.sakId, hendelse.id)
                    is Utflytting -> verifiserOgHaandterSoekerErUtflyttet(data, hendelse.sakId, hendelse.id)
                    is ForelderBarnRelasjon -> verifiserOgHaandterForelderBarnRelasjon(
                        data,
                        hendelse.sakId,
                        hendelse.id
                    )
                    null -> Unit
                }
            }
    }

    private fun verifiserOgHaandterDoedsfall(data: Doedsfall, sakId: Long, hendelseId: UUID) {
        val fnr = data.hendelse.avdoedFnr
        val soekerErDoed = soekerErDoed(fnr)
        haandterDoedsfall(sakId, data, soekerErDoed, hendelseId)
    }

    private fun verifiserOgHaandterSoekerErUtflyttet(data: Utflytting, sakId: Long, hendelseId: UUID) {
        val fnr = data.hendelse.fnr
        val soekerHarUtflytting = soekerHarUtflytting(fnr)
        haandterSoekerErUtflyttet(soekerHarUtflytting, hendelseId)
    }

    private fun verifiserOgHaandterForelderBarnRelasjon(data: ForelderBarnRelasjon, sakId: Long, hendelseId: UUID) {
        val fnr = data.hendelse.fnr
        val forelderBarnRelasjonErGyldig = forelderBarnRelasjonErGyldig(fnr)
        haandterForelderBarnRelasjon(forelderBarnRelasjonErGyldig, hendelseId)
    }

    /*
    TODO: her sjekkes det kun for at doedsdato eksisterer. Det er mulig at en person kan vaere doed
    uten at doedsdato eksisterer. Finn i saa fall ut hvordan doed kan bekreftes i pdl.
    https://pdldocs-navno.msappproxy.net/ekstern/index.html#_dødsfall
     */
    private fun soekerErDoed(fnr: String): KorrektIPDL {
        return pdlService.hentPdlModell(
            foedselsnummer = fnr,
            rolle = PersonRolle.BARN
        ).doedsdato?.let { doedsdato ->
            logger.info(
                "Person med fnr $fnr er doed i pdl " +
                    "med doedsdato: $doedsdato"
            )
            KorrektIPDL.JA
        } ?: KorrektIPDL.NEI
    }

    private fun soekerHarUtflytting(fnr: String): KorrektIPDL {
        return if (!pdlService.hentPdlModell(
                foedselsnummer = fnr,
                rolle = PersonRolle.BARN
            ).utland?.utflyttingFraNorge.isNullOrEmpty()
        ) {
            KorrektIPDL.JA
        } else {
            KorrektIPDL.NEI
        }
    }

    /*
    TODO: sjekk om forelder-barn-relasjon er gyldig
     */
    private fun forelderBarnRelasjonErGyldig(fnr: String): KorrektIPDL {
        return KorrektIPDL.IKKE_SJEKKET
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

    /*
    TODO: Vurder om ulike endringstyper boer haandteres forskjellig.
        Eksempel:
        - En tidligere hendelse sier at en person er doed.
        - Det kommer en ny hendelse fra pdl med korreksjon: bruker er ikke doed
        - Dette boer kanskje fanges opp?
        Se EY-976
     */
    private fun haandterDoedsfall(sakId: Long, data: Doedsfall, korrektIPDL: KorrektIPDL, hendelseId: UUID) {
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
}