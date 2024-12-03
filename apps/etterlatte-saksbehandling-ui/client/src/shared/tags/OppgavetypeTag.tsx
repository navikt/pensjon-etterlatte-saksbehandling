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
    case Oppgavetype.MANGLER_SOEKNAD:
      return <Tag variant="neutral-moderate">Mangler søknad</Tag>
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
    case Oppgavetype.TILLEGGSINFORMASJON:
      return <Tag variant="alt2">Tilleggsinformasjon</Tag>
    case Oppgavetype.GJENOPPRETTING_ALDERSOVERGANG:
      return <Tag variant="alt3-moderate">Gjenoppretting</Tag>
    case Oppgavetype.AKTIVITETSPLIKT:
      return <Tag variant="alt3-filled">Aktivitetsplikt</Tag>
    case Oppgavetype.AKTIVITETSPLIKT_12MND:
      return <Tag variant="alt3-filled">Aktivitetsplikt 12 Måneder</Tag>
    case Oppgavetype.AKTIVITETSPLIKT_REVURDERING:
      return <Tag variant="alt3-filled">Aktivitetsplikt</Tag>
    case Oppgavetype.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK:
      return <Tag variant="alt3-filled">Aktivitetsplikt</Tag>
    case Oppgavetype.GENERELL_OPPGAVE:
      return <Tag variant="info">Generell oppgave</Tag>
    case Oppgavetype.AARLIG_INNTEKTSJUSTERING:
      return <Tag variant="info">Årlig inntektsjustering</Tag>
    case Oppgavetype.INNTEKTSOPPLYSNING:
      return <Tag variant="alt3">Inntektsopplysning</Tag>
    case Oppgavetype.MANUELL_UTSENDING_BREV:
      return <Tag variant="info">Manuell brevutsending</Tag>
    default:
      return <Tag variant="error-filled">Ukjent oppgave</Tag>
  }
}
