package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

class AktivitetspliktKopierService(
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
) {
    private fun hentVurderingForBehandling(behandlingId: UUID): AktivitetspliktVurdering? {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)

        if (aktivitetsgrad.isEmpty() && unntak.isEmpty()) {
            return null
        }

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    private fun hentVurderingForOppgave(oppgaveId: UUID): AktivitetspliktVurdering? {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForOppgave(oppgaveId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForOppgave(oppgaveId)

        if (aktivitetsgrad.isEmpty() && unntak.isEmpty()) {
            return null
        }

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun kopierVurderingTilBehandling(
        sakId: SakId,
        behandlingId: UUID,
    ) {
        val vurdering = hentVurderingForBehandling(behandlingId)
        if (vurdering != null) {
            return
        }
        val nyesteVurdering =
            hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId)

        nyesteVurdering.aktivitet.forEach {
            aktivitetspliktAktivitetsgradDao.kopierAktivitetsgradTilBehandling(
                it.id,
                behandlingId,
            )
        }
        nyesteVurdering.unntak.forEach { aktivitetspliktUnntakDao.kopierUnntakTilBehandling(it.id, behandlingId) }
    }

    fun kopierVurderingTilOppgave(
        sakId: SakId,
        oppgaveId: UUID,
    ) {
        val vurdering = hentVurderingForOppgave(oppgaveId)
        if (vurdering != null) {
            return
        }
        val nyesteVurdering =
            hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId)

        nyesteVurdering.aktivitet.forEach {
            aktivitetspliktAktivitetsgradDao.kopierAktivitetsgradTilOppgave(
                it.id,
                oppgaveId,
            )
        }
        nyesteVurdering.unntak.forEach { aktivitetspliktUnntakDao.kopierUnntakTilOppgave(it.id, oppgaveId) }
    }

    fun kopierVurderingForOppgave() {
    }
}
