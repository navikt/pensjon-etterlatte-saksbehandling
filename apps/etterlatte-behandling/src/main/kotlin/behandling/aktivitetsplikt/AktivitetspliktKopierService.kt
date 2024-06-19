package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntak
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import java.util.UUID

class AktivitetspliktKopierService(
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao,
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao,
) {
    private fun hentVurderingForBehandling(behandlingId: UUID): AktivitetspliktVurdering? {
        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingId)
        val unntak = aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingId)

        if (aktivitetsgrad == null && unntak == null) {
            return null
        }

        return AktivitetspliktVurdering(aktivitetsgrad, unntak)
    }

    fun kopierVurdering(
        sakId: Long,
        behandlingId: UUID,
    ) {
        val vurdering = hentVurderingForBehandling(behandlingId)
        if (vurdering?.unntak != null || vurdering?.aktivitet != null) {
            return
        }

        val aktivitetsgrad = aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId)
        val unntak = aktivitetspliktUnntakDao.hentNyesteUnntak(sakId)
        val nyesteVurdering = listOfNotNull(aktivitetsgrad, unntak).sortedBy { it.opprettet.endretDatoOrNull() }.lastOrNull()

        if (nyesteVurdering != null) {
            when (nyesteVurdering) {
                is AktivitetspliktAktivitetsgrad ->
                    aktivitetspliktAktivitetsgradDao.kopierAktivitetsgrad(
                        nyesteVurdering.id,
                        behandlingId,
                    )
                is AktivitetspliktUnntak -> aktivitetspliktUnntakDao.kopierUnntak(nyesteVurdering.id, behandlingId)
            }
        }
    }
}
