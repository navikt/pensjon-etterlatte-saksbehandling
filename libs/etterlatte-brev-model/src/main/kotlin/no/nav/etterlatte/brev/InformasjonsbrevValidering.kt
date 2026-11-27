package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

fun validerInformasjonsbrev(
    tittel: String,
    innhold: Slate,
) {
    fun krevGyldig(gyldig: Boolean) {
        if (!gyldig) {
            throw UgyldigForespoerselException(
                code = "UGYLDIG_BREVINNHOLD",
                detail = "Brevet må ha en tittel og gyldig Slate-innhold uten HTML, maluttrykk eller uutfylte tekstfelt.",
            )
        }
    }

    val markup = Regex("<\\s*[/!a-zA-Z]|\\{\\{|\\}\\}|\\$\\{|<%|%>")

    fun validerTekst(tekst: String) {
        krevGyldig(!markup.containsMatchIn(tekst))
        krevGyldig(tekst.none { it.isISOControl() && it != '\n' && it != '\t' })
    }

    krevGyldig(tittel.isNotBlank() && tittel.length <= 200 && '\n' !in tittel && '\t' !in tittel)
    validerTekst(tittel)
    krevGyldig(innhold.elements.isNotEmpty() && innhold.elements.size <= 2_000)

    var antallNoder = innhold.elements.size
    var tekstlengde = 0
    var harTekst = false

    fun validerBarn(
        barn: List<Slate.InnerElement>,
        forelder: Slate.ElementType,
        dybde: Int,
    ) {
        krevGyldig(dybde <= 8 && barn.isNotEmpty())
        antallNoder += barn.size
        krevGyldig(antallNoder <= 2_000)
        barn.forEach { node ->
            krevGyldig(node.placeholder != true)
            when (forelder) {
                Slate.ElementType.BULLETED_LIST -> {
                    krevGyldig(node.type == Slate.ElementType.LIST_ITEM)
                }

                Slate.ElementType.LIST_ITEM -> {
                    krevGyldig(
                        node.type == null ||
                            node.type == Slate.ElementType.PARAGRAPH ||
                            node.type == Slate.ElementType.BULLETED_LIST,
                    )
                }

                else -> {
                    krevGyldig(node.type == null)
                }
            }
            val tekst = node.text
            if (tekst != null) {
                krevGyldig(node.children == null)
                krevGyldig(node.type == null || node.type == Slate.ElementType.PARAGRAPH)
                validerTekst(tekst)
                tekstlengde += tekst.length
                krevGyldig(tekstlengde <= 50_000)
                harTekst = harTekst || tekst.isNotBlank()
            } else {
                val type = node.type
                val children = node.children
                krevGyldig(type != null && children != null)
                if (type != null && children != null) {
                    validerBarn(children, type, dybde + 1)
                }
            }
        }
    }

    innhold.elements.forEach {
        krevGyldig(it.type != Slate.ElementType.LIST_ITEM)
        validerBarn(it.children, it.type, 1)
    }
    krevGyldig(harTekst)
}
