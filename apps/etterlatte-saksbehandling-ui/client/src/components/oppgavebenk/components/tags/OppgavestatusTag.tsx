import React from 'react'
import { Oppgavestatus } from '~shared/types/oppgave'
import { Tag } from '@navikt/ds-react'

export const OppgavestatusTag = ({ oppgavestatus }: { oppgavestatus: Oppgavestatus }) => {
  switch (oppgavestatus) {
    case Oppgavestatus.NY:
      return <Tag variant="alt1">Ikke startet</Tag>
    case Oppgavestatus.UNDER_BEHANDLING:
      return <Tag variant="info">Under behandling</Tag>
    case Oppgavestatus.ATTESTERING:
      return <Tag variant="info">Attestering</Tag>
    case Oppgavestatus.UNDERKJENT:
      return <Tag variant="warning">Underkjent</Tag>
    case Oppgavestatus.PAA_VENT:
      return <Tag variant="warning">På vent</Tag>
    case Oppgavestatus.FERDIGSTILT:
      return <Tag variant="success">Ferdigstilt</Tag>
    case Oppgavestatus.FEILREGISTRERT:
      return <Tag variant="error">Feilregistrert</Tag>
    case Oppgavestatus.AVBRUTT:
      return <Tag variant="error">Avbrutt</Tag>
  }
}
