package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
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
                            data = Grunnlagsinformasjon.SoekerDoed(hendelse = doedshendelse)
                        )
                    )
                }
            }
        }

    fun sjekkKlareGrunnlagsendringshendelser(minutterGamle: Long) {
        sjekkKlareDoedshendelser(minutterGamle)
    }

    fun sjekkKlareDoedshendelser(minutterGamle: Long) {
        inTransaction {
            grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                minutter = minutterGamle,
                type = GrunnlagsendringsType.SOEKER_DOED
            )
        }.forEach { endringsHendelse ->
            when (val endringsdata = endringsHendelse.data) {
                is Grunnlagsinformasjon.SoekerDoed -> {
                    pdlService.hentPdlModell(
                        foedselsnummer = endringsdata.hendelse.avdoedFnr,
                        rolle = PersonRolle.BARN
                    ).doedsdato?.let { doedsdato ->
                        logger.info(
                            "Person med fnr ${endringsdata.hendelse.avdoedFnr} er doed i pdl " +
                                "med doedsdato: $doedsdato"
                        )
                        generellBehandlingService.hentBehandlingerISak(endringsHendelse.sakId)
                            .`siste ikke-avbrutte behandling`()?.let { behandling ->
                                when (behandling.status) {
                                    in BehandlingStatus.underBehandling() -> {
                                        logger.info(
                                            "Behandling ${behandling.id} med status ${behandling.status} er under " +
                                                "behandling -> setter status til GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING."
                                        )
                                        inTransaction {
                                            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                                saker = listOf(endringsHendelse.sakId),
                                                foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                                                etterStatus = GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                                                type = GrunnlagsendringsType.SOEKER_DOED
                                            )
                                        }
                                    }

                                    in BehandlingStatus.iverksattEllerAttestert() -> {
                                        revurderingService.startRevurdering(
                                            forrigeBehandling = behandling,
                                            pdlHendelse = endringsdata.hendelse,
                                            revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                                        ).also {
                                            inTransaction {
                                                grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                                    saker = listOf(endringsHendelse.sakId),
                                                    foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                                                    etterStatus = GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                                                    type = GrunnlagsendringsType.SOEKER_DOED
                                                )
                                                grunnlagsendringshendelseDao.settBehandlingIdForTattMedIBehandling(
                                                    sak = endringsHendelse.sakId,
                                                    behandlingId = it.id,
                                                    type = GrunnlagsendringsType.SOEKER_DOED
                                                )
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            }
                    }
                        // TODO: Vurder om det er riktig å forkaste hvis dødsfall er annulert f.eks.
                        ?: logger.info(
                            "Person med fnr ${endringsdata.hendelse.avdoedFnr} er ikke doed i Pdl. " +
                                "Forkaster hendelse"
                        ).also {
                            inTransaction {
                                grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                    saker = listOf(endringsHendelse.sakId),
                                    foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                                    etterStatus = GrunnlagsendringStatus.FORKASTET,
                                    type = GrunnlagsendringsType.SOEKER_DOED
                                )
                            }
                        }
                }

                else -> {} // ikke haandtert
            }
        }
    }

    private fun List<Behandling>.`siste ikke-avbrutte behandling`() =
        this.sortedByDescending { it.behandlingOpprettet }.firstOrNull { it.status in BehandlingStatus.ikkeAvbrutt() }

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
                            data = Grunnlagsinformasjon.Utflytting(hendelse = utflyttingsHendelse)
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
                            data = Grunnlagsinformasjon.ForelderBarnRelasjon(hendelse = forelderBarnRelasjonHendelse)
                        )
                    )
                }
            }
        }
    }
}