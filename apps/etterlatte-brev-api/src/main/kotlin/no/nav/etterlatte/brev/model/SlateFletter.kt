package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.behandling.Navn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object SlateFletter {
    fun erstatt(slate: Slate, second: BrevData) = Slate(
        slate.elements.map {
            it.copy(children = it.children.map { element -> element.copy(text = erstattFlettefelt(element, second)) })
        }
    )

    private fun erstattFlettefelt(element: Slate.InnerElement, brevData: BrevData) = when (brevData) {
        is AdopsjonRevurderingBrevdata -> flettAdopsjonRevurderingBrevdata(element, brevData)
        else -> element.text
    }

    private fun flettAdopsjonRevurderingBrevdata(
        element: Slate.InnerElement,
        brevData: AdopsjonRevurderingBrevdata
    ) = element.text
        ?.replace("<dato>", formaterDato(brevData.virkningsdato, Spraak.NB))
        ?.replace("<adopsjonsdato>", "<her skal adopsjonsdato komme automatisk>")
        ?.replace("<navn>", formaterNavn(brevData.adoptertAv1, brevData.adoptertAv2))

    private fun formaterNavn(adoptertAv1: Navn, adoptertAv2: Navn?) =
        adoptertAv1.formaterNavn() + if (adoptertAv2 != null) {
            " og ${adoptertAv2.formaterNavn()}"
        } else {
            ""
        }

    private fun formaterDato(dato: LocalDate, spraak: Spraak): String =
        dato.format(dateFormatter(spraak)).replace(' ', ' ') // space to non-breaking space
}

private fun dateFormatter(spraak: Spraak): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(spraak.locale())