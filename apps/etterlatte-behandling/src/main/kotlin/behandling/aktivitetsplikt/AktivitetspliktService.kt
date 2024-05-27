package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_100
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.LagreAktivitetspliktUnntak
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktService(
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
    private val behandlingService: BehandlingService,
) {
    fun hentAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? {
        return inTransaction {
            aktivitetspliktDao.finnSenesteAktivitetspliktOppfolging(behandlingId)
        }
    }

    fun lagreAktivitetspliktOppfolging(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ): AktivitetspliktOppfolging {
        inTransaction {
            aktivitetspliktDao.lagre(behandlingId, nyOppfolging, navIdent)
        }

        return hentAktivitetspliktOppfolging(behandlingId)!!
    }

    fun oppfyllerAktivitetspliktVed6Mnd(
        sakId: Long,
        aktivitetspliktDato: LocalDate,
    ): Boolean {
        return inTransaction {
            val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId)
            if (aktivitetsgrad?.aktivitetsgrad in listOf(AKTIVITET_OVER_50, AKTIVITET_100)) {
                logger.info("Aktivitetsgrad er over 50% eller 100%, ingen revurdering opprettes for sak $sakId")
                return@inTransaction true
            }

            val unntak = aktivitetspliktUnntakDao.hentNyesteUnntak(sakId)
            if (unntak != null && (unntak.tom == null || unntak.tom.isAfter(aktivitetspliktDato))) {
                logger.info("Det er unntak for aktivitetsplikt, ingen revurdering opprettes for sak $sakId")
                return@inTransaction true
            }

            logger.info("Det er ikke gjort en vurdering av bruker på over 50% aktivitet, og finner ingen unntak for sak $sakId")
            false
        }
    }

    fun hentAktiviteter(behandlingId: UUID) =
        inTransaction {
            aktivitetspliktDao.hentAktiviteter(behandlingId)
        }

    fun upsertAktivitet(
        behandlingId: UUID,
        aktivitet: LagreAktivitetspliktAktivitet,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(inTransaction { behandlingService.hentBehandling(behandlingId) }) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        if (aktivitet.sakId != behandling.sak.id) {
            throw SakidTilhoererIkkeBehandlingException()
        }

        if (aktivitet.tom != null && aktivitet.tom < aktivitet.fom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        inTransaction {
            if (aktivitet.id != null) {
                aktivitetspliktDao.oppdaterAktivitet(behandlingId, aktivitet, kilde)
            } else {
                aktivitetspliktDao.opprettAktivitet(behandlingId, aktivitet, kilde)
            }
        }
    }

    fun slettAktivitet(
        behandlingId: UUID,
        aktivitetId: UUID,
    ) {
        val behandling =
            requireNotNull(inTransaction { behandlingService.hentBehandling(behandlingId) }) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        inTransaction {
            aktivitetspliktDao.slettAktivitet(aktivitetId, behandlingId)
        }
    }

    fun kopierAktiviteter(
        fraBehandlingId: UUID,
        tilBehandlingId: UUID,
    ) {
        requireNotNull(behandlingService.hentBehandling(tilBehandlingId)) { "Fant ikke behandling $tilBehandlingId" }

        aktivitetspliktDao.kopierAktiviteter(fraBehandlingId, tilBehandlingId)
    }

    fun opprettAktivitetsgrad(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        oppgaveId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        inTransaction {
            require(
                aktivitetspliktAktivitetsgradDao.hentAktivitetsgrad(oppgaveId) == null,
            ) { "Aktivitetsgrad finnes allerede for oppgave $oppgaveId" }
            aktivitetspliktAktivitetsgradDao.opprettAktivitetsgrad(aktivitetsgrad, sakId, kilde, oppgaveId)
        }
    }

    fun opprettUnntak(
        unntak: LagreAktivitetspliktUnntak,
        oppgaveId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (unntak.fom != null && unntak.tom != null && unntak.fom > unntak.tom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())
        inTransaction {
            require(
                aktivitetspliktUnntakDao.hentUnntak(oppgaveId) == null,
            ) { "Unntak finnes allerede for oppgave $oppgaveId" }
            aktivitetspliktUnntakDao.opprettUnntak(unntak, sakId, kilde, oppgaveId)
        }
    }

    fun hentVurdering(oppgaveId: UUID): AktivitetspliktVurdering? =
        inTransaction {
            val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgrad(oppgaveId)
            val unntak = aktivitetspliktUnntakDao.hentUnntak(oppgaveId)

            if (aktivitetsgrad == null && unntak == null) {
                return@inTransaction null
            }

            AktivitetspliktVurdering(aktivitetsgrad, unntak)
        }
}

class SakidTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SAK_ID_TILHOERER_IKKE_BEHANDLING",
        detail = "Sak id stemmer ikke over ens med behandling",
    )

class TomErFoerFomException :
    UgyldigForespoerselException(
        code = "TOM_ER_FOER_FOM",
        detail = "Til og med dato er kan ikke være før fra og med dato",
    )

data class AktivitetspliktVurdering(val aktivitet: AktivitetspliktAktivitetsgrad?, val unntak: AktivitetspliktUnntak?)
