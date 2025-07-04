package no.nav.pensjon.brevbaker.api.model

import no.nav.pensjon.brevbaker.api.model.LetterMarkup.Block
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.Block.Paragraph
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.Block.Title1
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.Block.Title2
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.Block.Type
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.ParagraphContent
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.ParagraphContent.Form.MultipleChoice
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.ParagraphContent.ItemList
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.ParagraphContent.Table
import no.nav.pensjon.brevbaker.api.model.LetterMarkup.ParagraphContent.Text.FontType

@Suppress("unused")
data class LetterMarkupGjenny(
    override val title: String,
    override val sakspart: SakspartImpl,
    override val blocks: List<Block>,
    override val signatur: SignaturImpl,
) : LetterMarkup {
    data class AttachmentImpl(
        override val title: List<ParagraphContent.Text>,
        override val blocks: List<Block>,
        override val includeSakspart: Boolean,
    ) : LetterMarkup.Attachment

    data class SakspartImpl(
        override val gjelderNavn: String,
        override val gjelderFoedselsnummer: String,
        override val saksnummer: String,
        override val dokumentDato: String,
    ) : LetterMarkup.Sakspart

    data class SignaturImpl(
        override val hilsenTekst: String,
        override val saksbehandlerRolleTekst: String,
        override val saksbehandlerNavn: String,
        override val attesterendeSaksbehandlerNavn: String?,
        override val navAvsenderEnhet: String,
    ) : LetterMarkup.Signatur

    object BlockImpl {
        data class Title1Impl(
            override val id: Int,
            override val editable: Boolean = true,
            override val content: List<ParagraphContent.Text>,
        ) : Title1 {
            override val type = Type.TITLE1
        }

        data class Title2Impl(
            override val id: Int,
            override val editable: Boolean = true,
            override val content: List<ParagraphContent.Text>,
        ) : Title2 {
            override val type = Type.TITLE2
        }

        data class ParagraphImpl(
            override val id: Int,
            override val editable: Boolean = true,
            override val content: List<ParagraphContent>,
        ) : Paragraph {
            override val type = Type.PARAGRAPH
        }
    }

    object ParagraphContentImpl {
        data class ItemListImpl(
            override val id: Int,
            override val items: List<ItemList.Item>,
        ) : ItemList {
            override val type = ParagraphContent.Type.ITEM_LIST

            data class ItemImpl(
                override val id: Int,
                override val content: List<ParagraphContent.Text>,
            ) : ItemList.Item
        }

        object TextImpl {
            data class LiteralImpl(
                override val id: Int,
                override val text: String,
                override val fontType: FontType = FontType.PLAIN,
                override val tags: Set<ElementTags> = emptySet(),
            ) : ParagraphContent.Text.Literal {
                override val type: ParagraphContent.Type = ParagraphContent.Type.LITERAL
            }

            data class VariableImpl(
                override val id: Int,
                override val text: String,
                override val fontType: FontType = FontType.PLAIN,
            ) : ParagraphContent.Text.Variable {
                override val type = ParagraphContent.Type.VARIABLE
            }

            data class NewLineImpl(
                override val id: Int,
            ) : ParagraphContent.Text.NewLine {
                override val fontType = FontType.PLAIN
                override val text: String = ""
                override val type = ParagraphContent.Type.NEW_LINE
            }
        }

        data class TableImpl(
            override val id: Int,
            override val rows: List<Table.Row>,
            override val header: Table.Header,
        ) : Table {
            override val type = ParagraphContent.Type.TABLE

            data class RowImpl(
                override val id: Int,
                override val cells: List<Table.Cell>,
            ) : Table.Row

            data class CellImpl(
                override val id: Int,
                override val text: List<ParagraphContent.Text>,
            ) : Table.Cell

            data class HeaderImpl(
                override val id: Int,
                override val colSpec: List<Table.ColumnSpec>,
            ) : Table.Header

            data class ColumnSpecImpl(
                override val id: Int,
                override val headerContent: Table.Cell,
                override val alignment: Table.ColumnAlignment,
                override val span: Int,
            ) : Table.ColumnSpec
        }

        object Form {
            data class TextImpl(
                override val id: Int,
                override val prompt: List<ParagraphContent.Text>,
                override val size: ParagraphContent.Form.Text.Size,
                override val vspace: Boolean,
            ) : ParagraphContent.Form.Text {
                override val type = ParagraphContent.Type.FORM_TEXT
            }

            data class MultipleChoiceImpl(
                override val id: Int,
                override val prompt: List<ParagraphContent.Text>,
                override val choices: List<MultipleChoice.Choice>,
                override val vspace: Boolean,
            ) : MultipleChoice {
                override val type = ParagraphContent.Type.FORM_CHOICE

                data class ChoiceImpl(
                    override val id: Int,
                    override val text: List<ParagraphContent.Text>,
                ) : MultipleChoice.Choice
            }
        }
    }
}
