package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class GrunnlagsendringshendelseService(
    val grunnlagsendringshendelseDao: GrunnlagsendringshendelseDao,
    val generellBehandlingService: GenerellBehandlingService,
    val revurderingService: RevurderingService,
    val pdlService: PdlService
) {

    val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrunnlagsendringshendelse(id: UUID) =
        inTransaction {
            grunnlagsendringshendelseDao.hentGrunnlagsendringshendelse(id)
        }

    fun opprettSoekerDoedHendelse(doedshendelse: Doedshendelse): List<Grunnlagsendringshendelse> =
        // finner saker med loepende utbetalinger
        generellBehandlingService.alleBehandlingerForSoekerMedFnr(doedshendelse.avdoedFnr).run {
            inTransaction {
                filter { it.status in BehandlingStatus.ikkeAvbrutt() }
                    .map { it.sak }
                    .distinct()
                    .also {
                        // Forkast Ikke-vurderte doedshendelser i samme sak - ny hendelse erstatter tidligere ikke-vurderte
                        grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                            saker = it,
                            foerStatus = GrunnlagsendringStatus.IKKE_VURDERT,
                            etterStatus = GrunnlagsendringStatus.FORKASTET,
                            type = GrunnlagsendringsType.SOEKER_DOED
                        )
                    }
                    .map { sakId ->
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
        logger.info("Sjekker for klare Doedshendelser")
        inTransaction {
            grunnlagsendringshendelseDao.hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                minutterGamle,
                GrunnlagsendringsType.SOEKER_DOED
            )
        }.forEach { endringsHendelse ->
            when (endringsHendelse.data) {
                is Grunnlagsinformasjon.SoekerDoed -> {
                    pdlService.hentPdlModell(
                        endringsHendelse.data.hendelse.avdoedFnr,
                        PersonRolle.BARN
                    ).doedsdato?.let { doedsdato ->
                        logger.info(
                            "Person med fnr ${endringsHendelse.data.hendelse.avdoedFnr} er doed i pdl " +
                                    "med doedsdato: $doedsdato"
                        )
                        generellBehandlingService.hentBehandlingerISak(endringsHendelse.sakId)
                            .sortedByDescending { it.behandlingOpprettet }
                            // TODO: naar vi faar med komplekse saker i saksbehandlingssystemet: vurder hvilken behandling som skal brukes videre
                            .first { it.status in BehandlingStatus.ikkeAvbrutt() }
                            .also { behandling ->
                                when (behandling.status) {
                                    in BehandlingStatus.underBehandling() -> {
                                        logger.info(
                                            "Behandling ${behandling.id} med status ${behandling.status} er under " +
                                                    "behandling -> setter status til GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING."
                                        )
                                        inTransaction {
                                            grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                                listOf(endringsHendelse.sakId),
                                                GrunnlagsendringStatus.IKKE_VURDERT,
                                                GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                                                GrunnlagsendringsType.SOEKER_DOED
                                            )
                                        }
                                    }
                                    in BehandlingStatus.iverksattEllerAttestert() -> {
                                        logger.info(
                                            "Behandling ${behandling.id} med status ${behandling.status} er " +
                                                    "iverksatt eller attestert -> Starter revurdering."
                                        )
                                        revurderingService.startRevurdering(
                                            behandling,
                                            endringsHendelse.data.hendelse,
                                            RevurderingAarsak.SOEKER_DOD
                                        ).also {
                                            inTransaction {
                                                grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                                    listOf(endringsHendelse.sakId),
                                                    GrunnlagsendringStatus.IKKE_VURDERT,
                                                    GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                                                    GrunnlagsendringsType.SOEKER_DOED
                                                )
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                    }
                    /*
                    TODO: naa forkastes alle grunnlagsendringer hvor bruker ikke er doed. Vurder etterhvert om det er aktuelt
                    aa ta hensyn til grunnlagsendringer hvor endringstype er annullert, som eventuelt kan trigge en revurdering
                     */
                        ?: logger.info(
                            "Person med fnr ${endringsHendelse.data.hendelse.avdoedFnr} er ikke doed i Pdl. " +
                                    "Forkaster hendelse"
                        )
                            .also {
                                grunnlagsendringshendelseDao.oppdaterGrunnlagsendringStatusForType(
                                    listOf(endringsHendelse.sakId),
                                    GrunnlagsendringStatus.IKKE_VURDERT,
                                    GrunnlagsendringStatus.FORKASTET,
                                    GrunnlagsendringsType.SOEKER_DOED
                                )
                            }
                }
                else -> {} // ikke haandtert
            }
        }
    }
}