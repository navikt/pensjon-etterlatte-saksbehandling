package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.behandling.Navn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object SlateFletter {
    fun erstatt(slate: Slate, brevData: BrevData) = Slate(
        slate.elements.map {
            it.copy(
                children = it.children.map { element ->
                    if (element.placeholder == true) {
                        element.copy(text = brevData.flett(element.text))
                    } else {
                        element
                    }
                }
            )
        }
    )

    fun formaterNavn(adoptertAv1: Navn, adoptertAv2: Navn?) =
        adoptertAv1.toString() + if (adoptertAv2 != null) {
            " og $adoptertAv2"
        } else {
            ""
        }

    fun formaterDato(dato: LocalDate, spraak: Spraak): String =
        dato.format(dateFormatter(spraak)).replace(' ', ' ') // space to non-breaking space
}

private fun dateFormatter(spraak: Spraak): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(spraak.locale())