package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakType

object BrevRequestMapper {
    fun fra(
        vedtakType: VedtakType,
        behandling: Behandling,
        avsender: Avsender,
        mottaker: Mottaker,
        attestant: Attestant?
    ) = when (vedtakType) {
        VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(
            behandling,
            avsender,
            BrevMottaker.fra(mottaker),
            attestant
        )
        VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(
            behandling,
            avsender,
            BrevMottaker.fra(mottaker),
            attestant
        )
        VedtakType.ENDRING -> TODO("Vedtakstype er ikke støttet: $vedtakType")
        VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
    }
}