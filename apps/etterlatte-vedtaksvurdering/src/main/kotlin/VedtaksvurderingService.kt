package no.nav.etterlatte

import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat


class VedtaksvurderingService(private val repository: VedtaksvurderingRepository) {

    fun lagreAvkorting(sakId: String, behandlingId: String, avkorting: Any) {
        repository.lagreAvkorting(sakId, behandlingId, avkorting)
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingId: String, vilkaarResultat: VilkaarResultat) {
        repository.lagreVilkaarsresultat(sakId, behandlingId, vilkaarResultat)
    }

    fun lagreBeregningsresultat(sakId: String, behandlingId: String, beregningsResultat: BeregningsResultat) {
        repository.lagreBeregningsresultat(sakId, behandlingId, beregningsResultat)
    }


    fun hentVilkaarsresultat(sakId: String, behandlingId: String): VilkaarResultat? {
        return repository.hentVilkaarsresultat(sakId, behandlingId)
    }

    fun hentAvkorting(sakId: String, behandlingId: String): String {
        return repository.hentAvkorting(sakId, behandlingId)
    }

    fun hentBeregningsresultat(sakId: String, behandlingId: String): BeregningsResultat? {
        return repository.hentBeregningsresultat(sakId, behandlingId)
    }

    fun fattVedtak() {
        repository
    }

}