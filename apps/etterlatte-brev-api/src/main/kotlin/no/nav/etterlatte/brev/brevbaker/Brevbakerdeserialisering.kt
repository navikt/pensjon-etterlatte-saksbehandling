package no.nav.etterlatte.brev.brevbaker

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.pensjon.brevbaker.api.model.RenderedJsonLetter

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RenderedJsonLetter.Block.Title1::class, name = "TITLE1"),
    JsonSubTypes.Type(value = RenderedJsonLetter.Block.Title2::class, name = "TITLE2"),
    JsonSubTypes.Type(value = RenderedJsonLetter.Block.Paragraph::class, name = "PARAGRAPH"),
)
interface BrevbakerJSONBlockMixIn

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RenderedJsonLetter.ParagraphContent.ItemList::class, name = "ITEM_LIST"),
    JsonSubTypes.Type(value = RenderedJsonLetter.ParagraphContent.Text.Literal::class, name = "LITERAL"),
    JsonSubTypes.Type(value = RenderedJsonLetter.ParagraphContent.Text.Variable::class, name = "VARIABLE"),
)
interface BrevbakerJSONParagraphMixIn
