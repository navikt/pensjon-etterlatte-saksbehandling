import React from 'react'
import { Journalpost, Journalposttype } from '~shared/types/Journalpost'
import { formaterStringDato } from '~utils/formattering'
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
      {dokument.avsenderMottaker.navn || 'Ukjent'} ({formaterStringDato(dokument.datoOpprettet)})
    </Detail>
  )
}
