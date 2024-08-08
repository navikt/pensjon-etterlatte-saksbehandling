import React from 'react'
import { Oppgavetype } from '~shared/types/oppgave'
import { Tag } from '@navikt/ds-react'

export const OppgavetypeTag = ({ oppgavetype }: { oppgavetype: Oppgavetype }) => {
  switch (oppgavetype) {
    case Oppgavetype.FOERSTEGANGSBEHANDLING:
      return <Tag variant="success">Førstegangsbehandling</Tag>
    case Oppgavetype.REVURDERING:
      return <Tag variant="warning">Revurdering</Tag>
    case Oppgavetype.VURDER_KONSEKVENS:
      return <Tag variant="alt1">Hendelse</Tag>
    case Oppgavetype.KRAVPAKKE_UTLAND:
      return <Tag variant="warning-moderate">Kravpakke utland</Tag>
    case Oppgavetype.KLAGE:
      return <Tag variant="error">Klage</Tag>
    case Oppgavetype.OMGJOERING:
      return <Tag variant="alt1-moderate">Omgjøring</Tag>
    case Oppgavetype.TILBAKEKREVING:
      return <Tag variant="info">Tilbakekreving</Tag>
    case Oppgavetype.JOURNALFOERING:
      return <Tag variant="alt2">Journalføring</Tag>
    case Oppgavetype.GJENOPPRETTING_ALDERSOVERGANG:
      return <Tag variant="alt3-moderate">Gjenoppretting</Tag>
    case Oppgavetype.AKTIVITETSPLIKT:
      return <Tag variant="alt3-filled">Aktivitetsplikt</Tag>
    case Oppgavetype.AKTIVITETSPLIKT_REVURDERING:
      return <Tag variant="alt3-filled">Aktivitetsplikt</Tag>
    case Oppgavetype.GENERELL_OPPGAVE:
      return <Tag variant="info">Generell oppgave</Tag>
    default:
      return <Tag variant="error-filled">Ukjent oppgave</Tag>
  }
}
