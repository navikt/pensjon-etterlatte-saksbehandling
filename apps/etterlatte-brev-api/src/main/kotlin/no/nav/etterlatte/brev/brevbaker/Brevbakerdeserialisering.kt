package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.pensjon.brevbaker.api.model.RenderedLetterMarkdown

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RenderedLetterMarkdown.Block.Title1::class, name = "TITLE1"),
    JsonSubTypes.Type(value = RenderedLetterMarkdown.Block.Title2::class, name = "TITLE2"),
    JsonSubTypes.Type(value = RenderedLetterMarkdown.Block.Paragraph::class, name = "PARAGRAPH"),
)
interface BrevbakerJSONBlockMixIn

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RenderedLetterMarkdown.ParagraphContent.ItemList::class, name = "ITEM_LIST"),
    JsonSubTypes.Type(value = RenderedLetterMarkdown.ParagraphContent.Text.Literal::class, name = "LITERAL"),
    JsonSubTypes.Type(value = RenderedLetterMarkdown.ParagraphContent.Text.Variable::class, name = "VARIABLE"),
)
interface BrevbakerJSONParagraphMixIn
