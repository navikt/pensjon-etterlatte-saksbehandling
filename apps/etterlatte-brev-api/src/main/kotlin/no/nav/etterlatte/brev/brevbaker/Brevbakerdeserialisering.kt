package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.pensjon.brevbaker.api.model.LetterMarkup

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = LetterMarkup.Block.Title1::class, name = "TITLE1"),
    JsonSubTypes.Type(value = LetterMarkup.Block.Title2::class, name = "TITLE2"),
    JsonSubTypes.Type(value = LetterMarkup.Block.Paragraph::class, name = "PARAGRAPH"),
)
interface BrevbakerJSONBlockMixIn

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = LetterMarkup.ParagraphContent.ItemList::class, name = "ITEM_LIST"),
    JsonSubTypes.Type(value = LetterMarkup.ParagraphContent.Text.Literal::class, name = "LITERAL"),
    JsonSubTypes.Type(value = LetterMarkup.ParagraphContent.Text.Variable::class, name = "VARIABLE"),
    JsonSubTypes.Type(value = LetterMarkup.ParagraphContent.Text.NewLine::class, name = "NEW_LINE"),
)
interface BrevbakerJSONParagraphMixIn
