package no.nav.etterlatte

import no.nav.etterlatte.database.Vedtak
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.*


class VedtaksvurderingService(private val repository: VedtaksvurderingRepository) {

    fun lagreAvkorting(sakId: String, behandlingId: UUID, avkorting: Any) {
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

    fun hentVedtak(sakId: String, behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(sakId, behandlingId)
    }

    fun fattVedtak() {
        //repository
    }

}