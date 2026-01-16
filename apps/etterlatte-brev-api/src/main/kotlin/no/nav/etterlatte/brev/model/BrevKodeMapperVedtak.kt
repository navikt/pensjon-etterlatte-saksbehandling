package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakType

data class BrevkodeRequest(
    val erMigrering: Boolean,
    // TODO BP_INNVILGELSE og BP_INNVILGELSE_FORELDRELOES skal slåes sammen og da kan denne fjernes
    val erForeldreloes: Boolean,
    val sakType: SakType,
    val vedtakType: VedtakType?,
    val revurderingaarsak: Revurderingaarsak?,
)

class VedtakTypeFeilForBrevkode(
    val vedtakType: VedtakType?,
) : UgyldigForespoerselException(
        code = "FEIL_VEDTAKTYPE",
        detail = "Feil vedtakstype $vedtakType",
    )

class BrevKodeMapperVedtak {
    fun brevKode(request: BrevkodeRequest): Brevkoder {
        if (request.erMigrering && !request.erForeldreloes) {
            if (!listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING).contains(request.vedtakType)) {
                throw VedtakTypeFeilForBrevkode(request.vedtakType)
            }
            return Brevkoder.OMREGNING
        }

        return when (request.sakType) {
            SakType.BARNEPENSJON -> {
                when (request.vedtakType) {
                    VedtakType.INNVILGELSE -> {
                        if (request.erForeldreloes) Brevkoder.BP_INNVILGELSE_FORELDRELOES else Brevkoder.BP_INNVILGELSE
                    }

                    VedtakType.AVSLAG -> {
                        Brevkoder.BP_AVSLAG
                    }

                    VedtakType.ENDRING -> {
                        Brevkoder.BP_REVURDERING
                    }

                    VedtakType.OPPHOER -> {
                        Brevkoder.BP_OPPHOER
                    }

                    VedtakType.AVVIST_KLAGE -> {
                        Brevkoder.AVVIST_KLAGE
                    }

                    null -> {
                        throw UgyldigForespoerselException(
                            code = "MANGLENDE_VEDTAKSTYPE_VEDTAKSBREV",
                            detail = "Skal ikke kunne komme hit med manglande vedtakstype. Gå igjennom steg fra beregning på nytt.",
                            meta = mapOf("request" to request),
                        )
                    }

                    VedtakType.INGEN_ENDRING,
                    VedtakType.TILBAKEKREVING,
                    -> {
                        throw InternfeilException("Brevkode for ${request.vedtakType} skal ikke utledes her")
                    }
                }
            }

            SakType.OMSTILLINGSSTOENAD -> {
                when (request.vedtakType) {
                    VedtakType.INNVILGELSE -> {
                        Brevkoder.OMS_INNVILGELSE
                    }

                    VedtakType.AVSLAG -> {
                        Brevkoder.OMS_AVSLAG
                    }

                    VedtakType.ENDRING -> {
                        when (request.revurderingaarsak) {
                            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK
                            else -> Brevkoder.OMS_REVURDERING
                        }
                    }

                    VedtakType.OPPHOER -> {
                        Brevkoder.OMS_OPPHOER
                    }

                    VedtakType.AVVIST_KLAGE -> {
                        Brevkoder.AVVIST_KLAGE
                    }

                    null -> {
                        throw UgyldigForespoerselException(
                            code = "MANGLENDE_VEDTAKSTYPE_VEDTAKSBREV",
                            detail = "Skal ikke kunne komme hit med manglande vedtakstype. Gå igjennom steg fra beregning på nytt.",
                            meta = mapOf("request" to request),
                        )
                    }

                    VedtakType.INGEN_ENDRING,
                    VedtakType.TILBAKEKREVING,
                    -> {
                        throw InternfeilException("Brevkode for ${request.vedtakType} skal ikke utledes her")
                    }
                }
            }
        }
    }
}
