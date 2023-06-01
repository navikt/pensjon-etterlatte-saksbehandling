package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.vedtak.VedtakType

object BrevDataMapper {
    fun fra(
        behandling: Behandling,
        avsender: Avsender,
        mottaker: Mottaker,
        attestant: Attestant?
    ) = when (val vedtakType = behandling.vedtak.type) {
        // TODO: Skille på forskjellige typer søknad (OMS, BP)
        VedtakType.INNVILGELSE -> InnvilgetBrevData.fraVedtak(
            behandling,
            avsender,
            BrevMottaker.fra(mottaker),
            attestant
        )
        VedtakType.AVSLAG -> AvslagBrevData.fraVedtak(
            behandling,
            avsender,
            BrevMottaker.fra(mottaker),
            attestant
        )
        VedtakType.ENDRING -> EndringBrevRequest.fraVedtak(
            behandling,
            avsender,
            BrevMottaker.fra(mottaker),
            attestant
        )
        VedtakType.OPPHOER -> TODO("Vedtakstype er ikke støttet: $vedtakType")
    }
}