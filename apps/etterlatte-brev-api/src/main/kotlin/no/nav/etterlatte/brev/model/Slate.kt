package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakType

data class Slate(
    @JsonValue val elements: List<Element> = emptyList()
) {

    data class Element(
        val type: ElementType,
        val children: List<InnerElement> = emptyList()
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class InnerElement(
        val type: ElementType? = null,
        val text: String? = null,
        val children: List<InnerElement>? = null,
        // TODO: Sjekk på at denne ikke finnes ved ferdigstilling av PDF
        val placeholder: Boolean? = null
    )

    enum class ElementType(@JsonValue val value: String) {
        HEADING_TWO("heading-two"),
        HEADING_THREE("heading-three"),
        PARAGRAPH("paragraph"),
        BULLETED_LIST("bulleted-list"),
        LIST_ITEM("list-item")
    }
}

object SlateHelper {
    fun hentInitiellPayload(sakType: SakType, vedtakType: VedtakType): Slate {
        return when (sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> getJsonFile("/maler/oms-nasjonal-innvilget.json")
                    else -> getJsonFile("/maler/tom-brevmal.json")
                }
            }

            else -> getJsonFile("/maler/tom-brevmal.json")
        }.let { deserialize(it) }
    }

    fun opprettTomBrevmal() = deserialize<Slate>(getJsonFile("/maler/tom-brevmal.json"))

    private fun getJsonFile(url: String) = javaClass.getResource(url)!!.readText()
}