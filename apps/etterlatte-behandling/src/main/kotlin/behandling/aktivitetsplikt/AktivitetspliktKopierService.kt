package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
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

    fun kopierVurdering(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        behandlingId: UUID,
    ) {
        val vurdering = hentVurderingForBehandling(behandlingId)
        if (vurdering != null) {
            return
        }
        val nyesteVurdering =
            hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId)

        nyesteVurdering.aktivitet.forEach {
            aktivitetspliktAktivitetsgradDao.kopierAktivitetsgrad(
                it.id,
                behandlingId,
            )
        }
        nyesteVurdering.unntak.forEach { aktivitetspliktUnntakDao.kopierUnntak(it.id, behandlingId) }
    }
}
