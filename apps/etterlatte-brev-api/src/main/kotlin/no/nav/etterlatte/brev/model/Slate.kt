package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.bp.VedleggBP
import no.nav.etterlatte.brev.model.oms.VedleggOMS
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakType

data class Slate(
    @JsonValue val elements: List<Element> = emptyList(),
) {
    data class Element(
        val type: ElementType,
        val children: List<InnerElement> = emptyList(),
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class InnerElement(
        val type: ElementType? = null,
        val text: String? = null,
        val children: List<InnerElement>? = null,
        // TODO: Sjekk på at denne ikke finnes ved ferdigstilling av PDF
        val placeholder: Boolean? = null,
    )

    enum class ElementType(
        @JsonValue val value: String,
    ) {
        HEADING_TWO("heading-two"),
        HEADING_THREE("heading-three"),
        PARAGRAPH("paragraph"),
        BULLETED_LIST("bulleted-list"),
        LIST_ITEM("list-item"),
    }
}

object SlateHelper {
    fun hentInitiellPayload(generellBrevData: GenerellBrevData): Slate =
        when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE -> getSlate("/maler/oms-nasjonal-innvilget.json")
                    else -> getSlate("/maler/tom-brevmal.json")
                }
            }

            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak.type) {
                    VedtakType.AVSLAG -> getSlate("/maler/bp-avslag.json")
                    else -> getSlate("/maler/tom-brevmal.json")
                }
            }
        }

    fun hentInitiellPayloadVedlegg(generellBrevData: GenerellBrevData): List<BrevInnholdVedlegg>? {
        return when (generellBrevData.sak.sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE -> VedleggOMS.innvilgelseOMS()
                    VedtakType.ENDRING -> {
                        when (generellBrevData.revurderingsaarsak) {
                            RevurderingAarsak.INNTEKTSENDRING,
                            RevurderingAarsak.ANNEN,
                            -> VedleggOMS.inntektsendringOMS()
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            SakType.BARNEPENSJON -> {
                when (generellBrevData.forenkletVedtak.type) {
                    VedtakType.INNVILGELSE -> VedleggBP.innvilgelse()
                    else -> null
                }
            }
        }
    }

    fun opprettTomBrevmal() = getSlate("/maler/tom-brevmal.json")

    private fun getJsonFile(url: String) = javaClass.getResource(url)!!.readText()

    fun getSlate(url: String) = getJsonFile(url).let { deserialize<Slate>(it) }
}
