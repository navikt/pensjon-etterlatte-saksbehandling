import React from 'react'
import { Oppgavestatus } from '~shared/types/oppgave'
import { Tag } from '@navikt/ds-react'

export const OppgavestatusTag = ({ oppgavestatus }: { oppgavestatus: Oppgavestatus }) => {
  switch (oppgavestatus) {
    case Oppgavestatus.NY:
      return (
        <Tag data-color="meta-purple" variant="outline">
          Ny
        </Tag>
      )
    case Oppgavestatus.UNDER_BEHANDLING:
      return (
        <Tag data-color="info" variant="outline">
          Under behandling
        </Tag>
      )
    case Oppgavestatus.ATTESTERING:
      return (
        <Tag data-color="info" variant="outline">
          Attestering
        </Tag>
      )
    case Oppgavestatus.UNDERKJENT:
      return (
        <Tag data-color="warning" variant="outline">
          Underkjent
        </Tag>
      )
    case Oppgavestatus.PAA_VENT:
      return (
        <Tag data-color="warning" variant="outline">
          På vent
        </Tag>
      )
    case Oppgavestatus.FERDIGSTILT:
      return (
        <Tag data-color="success" variant="outline">
          Ferdigstilt
        </Tag>
      )
    case Oppgavestatus.FEILREGISTRERT:
      return (
        <Tag data-color="danger" variant="outline">
          Feilregistrert
        </Tag>
      )
    case Oppgavestatus.AVBRUTT:
      return (
        <Tag data-color="danger" variant="outline">
          Avbrutt
        </Tag>
      )
  }
}
