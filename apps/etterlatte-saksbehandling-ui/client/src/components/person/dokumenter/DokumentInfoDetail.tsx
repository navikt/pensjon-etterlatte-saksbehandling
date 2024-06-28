import React from 'react'
import { Journalpost, Journalposttype } from '~shared/types/Journalpost'
import { formaterDato } from '~utils/formatering/dato'
import { Detail } from '@navikt/ds-react'

export const DokumentInfoDetail = ({ dokument }: { dokument: Journalpost }) => {
  return (
    <Detail>
      {
        {
          [Journalposttype.I]: 'Avsender: ',
          [Journalposttype.U]: 'Mottaker: ',
          [Journalposttype.N]: 'Notat',
        }[dokument.journalposttype]
      }
      {dokument.avsenderMottaker.navn || 'Ukjent'} ({formaterDato(dokument.datoOpprettet)})
    </Detail>
  )
}
