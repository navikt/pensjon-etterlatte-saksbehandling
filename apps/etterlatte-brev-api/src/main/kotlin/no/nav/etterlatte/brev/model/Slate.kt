package no.nav.etterlatte.brev.model

import com.fasterxml.jackson.annotation.JsonValue

class Slate(
    @JsonValue val value: List<Element> = emptyList()
) {

    data class Element(
        val type: ElementType,
        val children: List<Text> = emptyList()
    )

    data class Text(
        val text: String,
        // TODO: Sjekk på at denne ikke finnes ved ferdigstilling av PDF
        val placeholder: Boolean? = null
    )

    enum class ElementType(@JsonValue val value: String) {
        PARAGRAPH("paragraph"),
        HEADING_ONE("heading-one"),
        HEADING_TWO("heading-two"),
        HEADING_THREE("heading-three"),
        HEADING_FOUR("heading-four")
    }
}