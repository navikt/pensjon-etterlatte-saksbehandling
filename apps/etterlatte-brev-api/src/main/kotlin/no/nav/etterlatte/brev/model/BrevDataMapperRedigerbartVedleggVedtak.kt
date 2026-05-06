package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevDataRedigerbarRequest
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vedtak.VedtakType

class BrevDataMapperRedigerbartVedleggVedtak(
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
) {
    suspend fun brevData(brevDataRedigerbarRequest: BrevDataRedigerbarRequest) =
        with(brevDataRedigerbarRequest) {
            brevData(
                sakType,
                forenkletVedtak?.type,
            )
        }

    private suspend fun brevData(
        sakType: SakType,
        vedtakType: VedtakType?,
    ): BrevDataRedigerbar =
        when (sakType) {
            SakType.BARNEPENSJON -> {
                ManueltBrevData()
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE,
                    VedtakType.ENDRING,
                    -> {
                        OmstillingsstoenadBeregning(
                            innhold = TODO(),
                            virkningsdato = TODO(),
                            beregningsperioder = TODO(),
                            sisteBeregningsperiode = TODO(),
                            sisteBeregningsperiodeNesteAar = TODO(),
                            trygdetid = TODO(),
                            oppphoersdato = TODO(),
                            opphoerNesteAar = TODO(),
                            erYrkesskade = TODO(),
                        )
                    }

                    VedtakType.INGEN_ENDRING,
                    VedtakType.TILBAKEKREVING,
                    -> {
                        throw InternfeilException("Brevdata for $vedtakType skal ikke utledes her")
                    }

                    else -> {
                        ManueltBrevData()
                    }
                }
            }
        }
}
