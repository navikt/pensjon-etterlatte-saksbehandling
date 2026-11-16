import React from 'react'
import { Datotype, Journalpost, Journalposttype } from '~shared/types/Journalpost'
import { formaterDato } from '~utils/formatering/dato'
import { Detail } from '@navikt/ds-react'

const detaljTekst = (dokument: Journalpost) => {
  const datoRegistrert = dokument.relevanteDatoer.find((dato) => dato.datotype === Datotype.DATO_REGISTRERT)?.dato
  const datoEkspedert = dokument.relevanteDatoer.find((dato) => dato.datotype === Datotype.DATO_EKSPEDERT)?.dato
  const navn = dokument.avsenderMottaker.navn || 'Ukjent'
  const opprettet = `Opprettet: ${formaterDato(dokument.datoOpprettet)}`

  if (dokument.journalposttype === Journalposttype.I) {
    const datoTekst = datoRegistrert ? `Registrert: ${formaterDato(datoRegistrert)}` : opprettet
    return `Avsender: ${navn} (${datoTekst})`
  }

  if (dokument.journalposttype === Journalposttype.U) {
    const datoTekst = datoEkspedert ? `Ekspedert: ${formaterDato(datoEkspedert)}` : opprettet
    return `Mottaker: ${navn} (${datoTekst})`
  }

  if (dokument.journalposttype === Journalposttype.N) {
    return `Notat: ${opprettet}`
  }

  return opprettet
}

export const DokumentInfoDetail = ({ dokument }: { dokument: Journalpost }) => {
  return <Detail>{detaljTekst(dokument)}</Detail>
}
