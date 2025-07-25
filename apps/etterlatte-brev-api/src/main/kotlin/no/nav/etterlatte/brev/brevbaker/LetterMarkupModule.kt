package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import no.nav.pensjon.brevbaker.api.model.LetterMarkup
import no.nav.pensjon.brevbaker.api.model.LetterMarkupGjenny
import no.nav.pensjon.brevbaker.api.model.LetterMarkupGjenny.ParagraphContentImpl
import no.nav.pensjon.brevbaker.api.model.LetterMarkupGjenny.SakspartImpl
import no.nav.pensjon.brevbaker.api.model.LetterMarkupGjenny.SignaturImpl

internal object LetterMarkupModule : SimpleModule() {
    private fun readResolve(): Any = LetterMarkupModule

    class DeserializationException(
        message: String,
    ) : Exception(message)

    init {
        addDeserializer(LetterMarkup.Block::class.java, blockDeserializer())
        addDeserializer(LetterMarkup.ParagraphContent::class.java, paragraphContentDeserializer())
        addDeserializer(LetterMarkup.ParagraphContent.Text::class.java, textContentDeserializer())

        addInterfaceDeserializer<LetterMarkup.Sakspart, SakspartImpl>()
        addInterfaceDeserializer<LetterMarkup.Signatur, SignaturImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.ItemList, ParagraphContentImpl.ItemListImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.ItemList.Item, ParagraphContentImpl.ItemListImpl.ItemImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Text.Literal, ParagraphContentImpl.TextImpl.LiteralImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Text.Variable, ParagraphContentImpl.TextImpl.VariableImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Text.NewLine, ParagraphContentImpl.TextImpl.NewLineImpl>()
        addInterfaceDeserializer<LetterMarkup.Attachment, LetterMarkupGjenny.AttachmentImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Table, ParagraphContentImpl.TableImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Table.Row, ParagraphContentImpl.TableImpl.RowImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Table.Cell, ParagraphContentImpl.TableImpl.CellImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Table.Header, ParagraphContentImpl.TableImpl.HeaderImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Table.ColumnSpec, ParagraphContentImpl.TableImpl.ColumnSpecImpl>()
        addInterfaceDeserializer<
            LetterMarkup.ParagraphContent.Form.MultipleChoice.Choice,
            ParagraphContentImpl.Form.MultipleChoiceImpl.ChoiceImpl,
        >()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Form.MultipleChoice, ParagraphContentImpl.Form.MultipleChoiceImpl>()
        addInterfaceDeserializer<LetterMarkup.ParagraphContent.Form.Text, ParagraphContentImpl.Form.TextImpl>()
        addInterfaceDeserializer<LetterMarkup, LetterMarkupGjenny>()
    }

    private fun blockDeserializer() =
        object : StdDeserializer<LetterMarkup.Block>(LetterMarkup.Block::class.java) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext,
            ): LetterMarkup.Block {
                val node = p.codec.readTree<JsonNode>(p)
                val type =
                    when (LetterMarkup.Block.Type.valueOf(node.get("type").textValue())) {
                        LetterMarkup.Block.Type.TITLE1 -> LetterMarkupGjenny.BlockImpl.Title1Impl::class.java
                        LetterMarkup.Block.Type.TITLE2 -> LetterMarkupGjenny.BlockImpl.Title2Impl::class.java
                        LetterMarkup.Block.Type.PARAGRAPH -> LetterMarkupGjenny.BlockImpl.ParagraphImpl::class.java
                    }
                return p.codec.treeToValue(node, type)
            }
        }

    private fun paragraphContentDeserializer() =
        object : StdDeserializer<LetterMarkup.ParagraphContent>(LetterMarkup.ParagraphContent::class.java) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext,
            ): LetterMarkup.ParagraphContent {
                val node = p.codec.readTree<JsonNode>(p)
                val type =
                    when (LetterMarkup.ParagraphContent.Type.valueOf(node.get("type").textValue())) {
                        LetterMarkup.ParagraphContent.Type.ITEM_LIST -> LetterMarkup.ParagraphContent.ItemList::class.java
                        LetterMarkup.ParagraphContent.Type.LITERAL -> LetterMarkup.ParagraphContent.Text.Literal::class.java
                        LetterMarkup.ParagraphContent.Type.VARIABLE -> LetterMarkup.ParagraphContent.Text.Variable::class.java
                        LetterMarkup.ParagraphContent.Type.TABLE -> LetterMarkup.ParagraphContent.Table::class.java
                        LetterMarkup.ParagraphContent.Type.FORM_TEXT -> LetterMarkup.ParagraphContent.Form.Text::class.java
                        LetterMarkup.ParagraphContent.Type.FORM_CHOICE -> LetterMarkup.ParagraphContent.Form.MultipleChoice::class.java
                        LetterMarkup.ParagraphContent.Type.NEW_LINE -> LetterMarkup.ParagraphContent.Text.NewLine::class.java
                    }
                return p.codec.treeToValue(node, type)
            }
        }

    private fun textContentDeserializer() =
        object : StdDeserializer<LetterMarkup.ParagraphContent.Text>(LetterMarkup.ParagraphContent.Text::class.java) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext,
            ): LetterMarkup.ParagraphContent.Text {
                val node = p.codec.readTree<JsonNode>(p)
                val clazz =
                    when (val contentType = LetterMarkup.ParagraphContent.Type.valueOf(node.get("type").textValue())) {
                        LetterMarkup.ParagraphContent.Type.LITERAL -> LetterMarkup.ParagraphContent.Text.Literal::class.java
                        LetterMarkup.ParagraphContent.Type.VARIABLE -> LetterMarkup.ParagraphContent.Text.Variable::class.java
                        LetterMarkup.ParagraphContent.Type.NEW_LINE -> LetterMarkup.ParagraphContent.Text.NewLine::class.java
                        LetterMarkup.ParagraphContent.Type.TABLE,
                        LetterMarkup.ParagraphContent.Type.FORM_TEXT,
                        LetterMarkup.ParagraphContent.Type.FORM_CHOICE,
                        LetterMarkup.ParagraphContent.Type.ITEM_LIST,
                        -> throw DeserializationException(
                            "$contentType is not allowed in a text-only block.",
                        )
                    }
                return p.codec.treeToValue(node, clazz)
            }
        }
}

private inline fun <reified T, reified V : T> SimpleModule.addInterfaceDeserializer() =
    addDeserializer(T::class.java, object : FellesDeserializer<T, V>(V::class.java) {})

private abstract class FellesDeserializer<T, V : T>(
    private val v: Class<V>,
) : JsonDeserializer<T>() {
    override fun deserialize(
        parser: JsonParser,
        ctxt: DeserializationContext,
    ): T = parser.codec.treeToValue(parser.codec.readTree<JsonNode>(parser), v)
}
