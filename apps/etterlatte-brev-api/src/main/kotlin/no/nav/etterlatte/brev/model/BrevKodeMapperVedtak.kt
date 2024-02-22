package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.brevbaker.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakType

data class BrevkodeRequest(
    val erMigrering: Boolean,
    // TODO BP_INNVILGELSE og BP_INNVILGELSE_FORELDRELOES skal slåes sammen og da kan denne fjernes
    val erForeldreloes: Boolean,
    val sakType: SakType,
    val vedtakType: VedtakType?,
)

class BrevKodeMapperVedtak {
    fun brevKode(request: BrevkodeRequest): Brevkoder {
        if (request.erMigrering && !request.erForeldreloes) {
            assert(listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(request.vedtakType))
            return Brevkoder.OMREGNING
        }

        return when (request.sakType) {
            SakType.BARNEPENSJON -> {
                when (request.vedtakType) {
                    VedtakType.INNVILGELSE ->
                        if (request.erForeldreloes) Brevkoder.BP_INNVILGELSE_FORELDRELOES else Brevkoder.BP_INNVILGELSE

                    VedtakType.AVSLAG -> Brevkoder.BP_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.BP_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.BP_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    VedtakType.AVVIST_KLAGE -> Brevkoder.AVVIST_KLAGE
                    null -> throw UgyldigForespoerselException(
                        "MANGLENDE_VEDTAKSTYPE_VEDTAKSBREV",
                        "Skal ikke kunne komme hit med manglande vedtakstype, for request $request",
                    )
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (request.vedtakType) {
                    VedtakType.INNVILGELSE -> Brevkoder.OMS_INNVILGELSE
                    VedtakType.AVSLAG -> Brevkoder.OMS_AVSLAG
                    VedtakType.ENDRING -> Brevkoder.OMS_REVURDERING
                    VedtakType.OPPHOER -> Brevkoder.OMS_OPPHOER
                    VedtakType.TILBAKEKREVING -> Brevkoder.TILBAKEKREVING
                    VedtakType.AVVIST_KLAGE -> Brevkoder.AVVIST_KLAGE
                    null -> throw UgyldigForespoerselException(
                        "MANGLENDE_VEDTAKSTYPE_VEDTAKSBREV",
                        "Skal ikke kunne komme hit med manglande vedtakstype, for request $request",
                    )
                }
            }
        }
    }
}
