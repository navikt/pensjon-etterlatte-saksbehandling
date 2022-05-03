package no.nav.etterlatte

import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat

enum class Vurdering {
    VEDTAK_OK,
    VEDTAK_UGYLDIG
}
data class VurdertVedtak(val vurdering: Vurdering)

class VedtaksvurderingService(private val vedtaksvurderingRepository: VedtaksvurderingRepository) {

    fun lagreAvkorting(sakId: String, behandlingId: String, avkorting: Any) {

    }

    fun lagreVilkaarsresultat(sakId: String, behandlingId: String, vilkaarResultat: VilkaarResultat) {

    }

    fun lagreBeregningsresultat(sakId: String, behandlingId: String, beregningsResultat: BeregningsResultat) {

    }

    fun hentVilkaarsresultat(sakId: String, behandlingId: String): String {
        return ""
    }

    fun hentAvkorting(sakId: String, behandlingId: String): String {
        return ""
    }

    fun hentBeregningsresultat(sakId: String, behandlingId: String): String {
        return ""
    }

}