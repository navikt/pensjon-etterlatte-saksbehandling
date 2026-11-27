import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.validerInformasjonsbrev
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InformasjonsbrevValideringTest {
    @Test
    fun `bevarer tittel og tekst uten normalisering`() {
        val tekst = " Informasjon om omstillingsstønad.\nDu trenger ikke svare. "
        val slate = avsnitt(tekst)
        assertDoesNotThrow { validerInformasjonsbrev("  Informasjon  ", slate) }
        assertEquals(
            tekst,
            slate.elements
                .single()
                .children
                .single()
                .text,
        )
    }

    @Test
    fun `godtar overskrifter og punktliste`() {
        val slate =
            Slate(
                listOf(
                    Slate.Element(Slate.ElementType.HEADING_TWO, listOf(Slate.InnerElement(text = "Overskrift"))),
                    Slate.Element(
                        Slate.ElementType.BULLETED_LIST,
                        listOf(
                            Slate.InnerElement(
                                type = Slate.ElementType.LIST_ITEM,
                                children = listOf(Slate.InnerElement(type = Slate.ElementType.PARAGRAPH, text = "Punkt")),
                            ),
                        ),
                    ),
                ),
            )
        assertDoesNotThrow { validerInformasjonsbrev("Informasjon", slate) }
    }

    @Test
    fun `avviser tom tittel og manglende meningsfull tekst`() {
        ugyldig("", avsnitt("Tekst"))
        ugyldig(" ", avsnitt("Tekst"))
        ugyldig("Tittel", Slate())
        ugyldig("Tittel", avsnitt(" \n\t"))
        ugyldig("Tittel", Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH))))
    }

    @Test
    fun `avviser markup maluttrykk og kontrolltegn`() {
        listOf("<script>alert(1)</script>", "<b>tekst</b>", "{{navn}}", "\${navn}", "<%navn%>", "tekst\u0000")
            .forEach { ugyldig("Tittel", avsnitt(it)) }
        ugyldig("<b>Tittel</b>", avsnitt("Tekst"))
        ugyldig("Tittel\nmed linjeskift", avsnitt("Tekst"))
    }

    @Test
    fun `kontrollerer lengdegrenser`() {
        assertDoesNotThrow { validerInformasjonsbrev("T".repeat(200), avsnitt("a".repeat(50_000))) }
        assertDoesNotThrow {
            validerInformasjonsbrev("Tittel", Slate(List(1_000) { avsnitt("a").elements.single() }))
        }
        ugyldig("T".repeat(201), avsnitt("Tekst"))
        ugyldig("Tittel", avsnitt("a".repeat(50_001)))
        ugyldig("Tittel", Slate(List(1_001) { avsnitt("a").elements.single() }))
        ugyldig("Tittel", Slate(List(2_001) { avsnitt("a").elements.single() }))
        ugyldig("Tittel", Slate(avsnitt("a".repeat(25_000)).elements + avsnitt("b".repeat(25_001)).elements))
    }

    @Test
    fun `avviser placeholder og ugyldig struktur`() {
        ugyldig(
            "Tittel",
            Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = "Tekst", placeholder = true))))),
        )
        ugyldig(
            "Tittel",
            Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement())))),
        )
        ugyldig("Tittel", Slate(listOf(Slate.Element(Slate.ElementType.LIST_ITEM, listOf(Slate.InnerElement(text = "Tekst"))))))
        ugyldig("Tittel", Slate(listOf(Slate.Element(Slate.ElementType.BULLETED_LIST, listOf(Slate.InnerElement(text = "Tekst"))))))
        ugyldig(
            "Tittel",
            Slate(
                listOf(
                    Slate.Element(
                        Slate.ElementType.PARAGRAPH,
                        listOf(Slate.InnerElement(text = "Tekst", children = listOf(Slate.InnerElement(text = "Ekstra")))),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `godtar aatte nivaaer men avviser ni`() {
        var node = Slate.InnerElement(type = Slate.ElementType.LIST_ITEM, children = listOf(Slate.InnerElement(text = "Tekst")))
        repeat(3) {
            node =
                Slate.InnerElement(
                    type = Slate.ElementType.LIST_ITEM,
                    children = listOf(Slate.InnerElement(type = Slate.ElementType.BULLETED_LIST, children = listOf(node))),
                )
        }
        val gyldig = Slate(listOf(Slate.Element(Slate.ElementType.BULLETED_LIST, listOf(node))))
        assertDoesNotThrow { validerInformasjonsbrev("Tittel", gyldig) }
        node =
            Slate.InnerElement(
                type = Slate.ElementType.LIST_ITEM,
                children = listOf(Slate.InnerElement(type = Slate.ElementType.BULLETED_LIST, children = listOf(node))),
            )
        ugyldig("Tittel", Slate(listOf(Slate.Element(Slate.ElementType.BULLETED_LIST, listOf(node)))))
    }

    private fun avsnitt(tekst: String) = Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = tekst)))))

    private fun ugyldig(
        tittel: String,
        slate: Slate,
    ) {
        assertThrows(UgyldigForespoerselException::class.java) { validerInformasjonsbrev(tittel, slate) }
    }
}
