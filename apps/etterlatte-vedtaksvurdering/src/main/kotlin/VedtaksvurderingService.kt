package no.nav.etterlatte

import no.nav.etterlatte.database.Vedtak
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*


class VedtaksvurderingService(private val repository: VedtaksvurderingRepository) {

    fun lagreAvkorting(sakId: String, behandlingId: UUID, avkorting: String) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreAvkorting(sakId, behandlingId, avkorting)
        } else {
            repository.oppdaterAvkorting(sakId, behandlingId, avkorting)
        }
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingId: UUID, vilkaarResultat: VilkaarResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreVilkaarsresultat(sakId, behandlingId, vilkaarResultat)
        } else {
            repository.oppdaterVilkaarsresultat(sakId, behandlingId, vilkaarResultat)
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingId: UUID, beregningsResultat: BeregningsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreBeregningsresultat(sakId, behandlingId, beregningsResultat)
        } else {
            repository.oppdaterBeregningsgrunnlag(sakId, behandlingId, beregningsResultat)
        }
    }

    fun lagreKommerSoekerTilgodeResultat(sakId: String, behandlingId: UUID, kommerSoekerTilgodeResultat: KommerSoekerTilgode) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)

        if(vedtak == null) {
            repository.lagreKommerSoekerTilgodeResultat(sakId, behandlingId, kommerSoekerTilgodeResultat)
        } else {
            repository.oppdaterKommerSoekerTilgodeResultat(sakId, behandlingId, kommerSoekerTilgodeResultat)
        }
    }

    fun hentVedtak(sakId: String, behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(sakId, behandlingId)
    }

    fun fattVedtak(sakId: String, behandlingId: UUID) {
        //repository
        return repository.fattVedtak("abc", sakId, behandlingId)
    }

}