package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class BrevbakerJSONResponse(
    val title: String,
    val sakspart: Sakspart,
    val blocks: List<Block>,
    val signatur: Signatur
) {

    data class Sakspart(
        val gjelderNavn: String,
        val gjelderFoedselsnummer: String,
        val saksnummer: String,
        val dokumentDato: String
    )

    data class Signatur(
        val hilsenTekst: String,
        val saksbehandlerRolleTekst: String,
        val saksbehandlerNavn: String,
        val attesterendeSaksbehandlerNavn: String?,
        val navAvsenderEnhet: String
    )

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = Block.Title1::class, name = "TITLE1"),
        JsonSubTypes.Type(value = Block.Title2::class, name = "TITLE2"),
        JsonSubTypes.Type(value = Block.Paragraph::class, name = "PARAGRAPH")
    )
    sealed class Block(open val id: Int, open val type: Type, open val editable: Boolean = true) {
        enum class Type {
            TITLE1, TITLE2, PARAGRAPH
        }

        data class Title1(
            override val id: Int,
            override val editable: Boolean,
            val content: List<ParagraphContent.Text>
        ) : Block(
            id,
            Type.TITLE1,
            editable
        )

        data class Title2(
            override val id: Int,
            override val editable: Boolean,
            val content: List<ParagraphContent.Text>
        ) : Block(id, Type.TITLE2, editable)

        data class Paragraph(
            override val id: Int,
            override val editable: Boolean,
            val content: List<ParagraphContent>
        ) : Block(id, Type.PARAGRAPH, editable)
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = ParagraphContent.ItemList::class, name = "ITEM_LIST"),
        JsonSubTypes.Type(value = ParagraphContent.Text.Literal::class, name = "LITERAL"),
        JsonSubTypes.Type(value = ParagraphContent.Text.Variable::class, name = "VARIABLE")
    )
    sealed class ParagraphContent(open val id: Int, open val type: Type) {
        enum class Type {
            ITEM_LIST, LITERAL, VARIABLE
        }

        data class ItemList(override val id: Int, val items: List<Item>) : ParagraphContent(id, Type.ITEM_LIST) {
            data class Item(val content: List<Text>)
        }

        sealed class Text(id: Int, type: Type, open val text: String) : ParagraphContent(id, type) {
            data class Literal(override val id: Int, override val text: String) : Text(id, Type.LITERAL, text)
            data class Variable(override val id: Int, override val text: String) : Text(id, Type.VARIABLE, text)
        }
    }
}