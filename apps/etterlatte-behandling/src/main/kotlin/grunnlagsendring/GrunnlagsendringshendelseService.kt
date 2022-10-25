package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.ForelderBarnRelasjon
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.SoekerDoed
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon.Utflytting
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
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

    /* Henter grunnlagsendringshendelser med status GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING */
    fun hentGyldigeHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGyldigeGrunnlagsendringshendelserISak(sakId)
    }

    fun hentAlleHendelserForSak(sakId: Long) = inTransaction {
        grunnlagsendringshendelseDao.hentGrunnlagsendringshendelserMedStatuserISak(
            sakId,
            GrunnlagsendringStatus.values().toList()
        )
    }

    fun opprettDoedshendelser(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> {
        return opprettSoekerDoedHendelse(doedshendelse)
    }

    fun opprettSoekerDoedHendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> =
        // finner saker med loepende utbetalinger
        generellBehandlingService.alleSakIderForSoekerMedFnr(doedshendelse.avdoedFnr).let { saker ->
            inTransaction {
                // Forkast Ikke-vurderte doedshendelser i samme sak - ny hendelse erstatter tidligere ikke-vurderte
                grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                    saker = saker,
                    foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                    etterStatus = GrunnlagsendringStatus.FORKASTET,
                    type = GrunnlagsendringsType.SOEKER_DOED
                )
                saker.map { sakId ->
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                        Grunnlagsendringshendelse(
                            id = UUID.randomUUID(),
                            sakId = sakId,
                            type = GrunnlagsendringsType.SOEKER_DOED,
                            opprettet = LocalDateTime.now(),
                            data = SoekerDoed(hendelse = doedshendelse)
                        )
                    )
                }
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
                    is SoekerDoed -> verifiserOgHaandterSoekerErDoed(data, hendelse.sakId)
                    is Utflytting -> verifiserOgHaandterSoekerErUtflyttet(data, hendelse.sakId)
                    is ForelderBarnRelasjon -> verifiserOgHaandterForelderBarnRelasjon(data, hendelse.sakId)
                    null -> Unit
                }
            }
    }

    private fun verifiserOgHaandterSoekerErDoed(data: SoekerDoed, sakId: Long) {
        val fnr = data.hendelse.avdoedFnr
        if (soekerErDoed(fnr)) {
            haandterSoekerDoed(sakId, data)
        } else {
            logger.info("Person med fnr $fnr er ikke doed i FDL. Forkaster hendelse.")
            forkastHendelse(sakId, GrunnlagsendringsType.SOEKER_DOED)
        }
    }

    private fun verifiserOgHaandterSoekerErUtflyttet(data: Utflytting, sakId: Long) {
        val fnr = data.hendelse.fnr
        if (soekerHarUtflytting(fnr)) {
            haandterSoekerErUtflyttet(sakId)
        } else {
            logger.info("Person med fnr $fnr er ikke utflyttet i PDL. Forkaster hendelse")
            forkastHendelse(sakId, GrunnlagsendringsType.UTFLYTTING)
        }
    }

    private fun verifiserOgHaandterForelderBarnRelasjon(data: ForelderBarnRelasjon, sakId: Long) {
        val fnr = data.hendelse.fnr
        if (forelderBarnRelasjonErGyldig(fnr)) {
            haandterForelderBarnRelasjon(sakId)
        } else {
            logger.info("Forelder-barn-relasjo-melding for $fnr stemte ikke e.l.......")
            forkastHendelse(sakId, GrunnlagsendringsType.FORELDER_BARN_RELASJON)
        }
    }

    /*
    TODO: her sjekkes det kun for at doedsdato eksisterer. Det er mulig at en person kan vaere doed
    uten at doedsdato eksisterer. Finn i saa fall ut hvordan doed kan bekreftes i pdl.
    https://pdldocs-navno.msappproxy.net/ekstern/index.html#_dødsfall
     */
    private fun soekerErDoed(fnr: String): Boolean {
        return pdlService.hentPdlModell(
            foedselsnummer = fnr,
            rolle = PersonRolle.BARN
        ).doedsdato?.let { doedsdato ->
            logger.info(
                "Person med fnr $fnr er doed i pdl " +
                    "med doedsdato: $doedsdato"
            )
            true
        } ?: false
    }

    private fun soekerHarUtflytting(fnr: String): Boolean {
        return !pdlService.hentPdlModell(
            foedselsnummer = fnr,
            rolle = PersonRolle.BARN
        ).utland?.utflyttingFraNorge.isNullOrEmpty()
    }

    private fun forelderBarnRelasjonErGyldig(fnr: String): Boolean {
        // I første omgang sier vi at alle hendelesene her er 'gyldige', dvs vi oppretter en hendelse
        //  for de og viser de fram i frontend
        return true
    }

    private fun haandterSoekerErUtflyttet(sakId: Long) {
        inTransaction {
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                saker = listOf(sakId),
                foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                etterStatus = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                type = GrunnlagsendringsType.UTFLYTTING
            )
        }
    }

    private fun haandterForelderBarnRelasjon(sakId: Long) {
        inTransaction {
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                saker = listOf(sakId),
                foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                etterStatus = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                type = GrunnlagsendringsType.FORELDER_BARN_RELASJON
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
    private fun haandterSoekerDoed(sakId: Long, data: SoekerDoed) {
        val behandlingerISak = generellBehandlingService.hentBehandlingerISak(sakId)

        // Har vi en eksisterende behandling som ikke er avbrutt?
        val sisteBehandling = behandlingerISak
            .`siste ikke-avbrutte behandling`() ?: return // TODO("Se på ekstra håndtering her, kanskje slette data") øh 19.10.2022

        val harAlleredeEtManueltOpphoer = behandlingerISak.any { it.type == BehandlingType.MANUELT_OPPHOER }
        val harAlleredeOpphoerDoedsfall =
            behandlingerISak.any {
                it.type == BehandlingType.REVURDERING &&
                    (it as? Revurdering)?.revurderingsaarsak == RevurderingAarsak.SOEKER_DOD
            }
        if (harAlleredeEtManueltOpphoer || harAlleredeOpphoerDoedsfall) {
            return // TODO("Oppdatere grunnlagsendringshendelsen til å være vurdert?") øh 19.10.2022 se EY-975
        }

        when (sisteBehandling.status) {
            in BehandlingStatus.underBehandling() -> {
                logger.info(
                    "Behandling ${sisteBehandling.id} med status ${sisteBehandling.status} er under " +
                        "behandling -> setter status til GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING."
                )
                inTransaction {
                    grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                        saker = listOf(sakId),
                        foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                        etterStatus = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                        type = GrunnlagsendringsType.SOEKER_DOED
                    )
                }
            }

            in BehandlingStatus.iverksattEllerAttestert() -> {
                revurderingService.startRevurdering(
                    forrigeBehandling = sisteBehandling,
                    pdlHendelse = data.hendelse,
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                ).also {
                    inTransaction {
                        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                            saker = listOf(sakId),
                            foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                            etterStatus = GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                            type = GrunnlagsendringsType.SOEKER_DOED
                        )
                        grunnlagsendringshendelseDao.settBehandlingIdForTattMedIBehandling(
                            sak = sakId,
                            behandlingId = it.id,
                            type = GrunnlagsendringsType.SOEKER_DOED
                        )
                    }
                }
            }

            else -> {}
        }
    }

    private fun forkastHendelse(sakId: Long, type: GrunnlagsendringsType) =
        inTransaction {
            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                saker = listOf(sakId),
                foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                etterStatus = GrunnlagsendringStatus.FORKASTET,
                type = type
            )
        }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }
            .firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }

    fun opprettUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse) {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        generellBehandlingService.alleSakIderForSoekerMedFnr(utflyttingsHendelse.fnr).also {
            inTransaction {
                it.forEach { sakId ->
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                        Grunnlagsendringshendelse(
                            id = UUID.randomUUID(),
                            sakId = sakId,
                            type = GrunnlagsendringsType.UTFLYTTING,
                            opprettet = tidspunktForMottakAvHendelse,
                            data = Utflytting(hendelse = utflyttingsHendelse)
                        )
                    )
                }
            }
        }
    }

    fun opprettForelderBarnRelasjonHendelse(forelderBarnRelasjonHendelse: ForelderBarnRelasjonHendelse) {
        val tidspunktForMottakAvHendelse = LocalDateTime.now()

        generellBehandlingService.alleSakIderForSoekerMedFnr(forelderBarnRelasjonHendelse.fnr).also {
            inTransaction {
                it.forEach { sakId ->
                    grunnlagsendringshendelseDao.opprettGrunnlagsendringshendelse(
                        Grunnlagsendringshendelse(
                            id = UUID.randomUUID(),
                            sakId = sakId,
                            type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
                            opprettet = tidspunktForMottakAvHendelse,
                            data = ForelderBarnRelasjon(hendelse = forelderBarnRelasjonHendelse)
                        )
                    )
                }
            }
        }
    }
}