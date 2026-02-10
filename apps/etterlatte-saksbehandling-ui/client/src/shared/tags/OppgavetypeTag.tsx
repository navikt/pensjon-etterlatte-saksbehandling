import React from 'react'
import { Oppgavetype } from '~shared/types/oppgave'
import { Tag } from '@navikt/ds-react'

export const OppgavetypeTag = ({ oppgavetype }: { oppgavetype: Oppgavetype }) => {
  switch (oppgavetype) {
    case Oppgavetype.FOERSTEGANGSBEHANDLING:
      return (
        <Tag data-color="success" variant="outline">
          Førstegangsbehandling
        </Tag>
      )

    case Oppgavetype.REVURDERING:
      return (
        <Tag data-color="warning" variant="outline">
          Revurdering
        </Tag>
      )

    case Oppgavetype.VURDER_KONSEKVENS:
      return (
        <Tag data-color="meta-purple" variant="outline">
          Hendelse
        </Tag>
      )

    case Oppgavetype.MANGLER_SOEKNAD:
      return (
        <Tag data-color="neutral" variant="moderate">
          Mangler søknad
        </Tag>
      )

    case Oppgavetype.KRAVPAKKE_UTLAND:
      return (
        <Tag data-color="warning" variant="moderate">
          Kravpakke utland
        </Tag>
      )

    case Oppgavetype.KLAGE:
      return (
        <Tag data-color="danger" variant="outline">
          Klage
        </Tag>
      )

    case Oppgavetype.KLAGE_SVAR_KABAL:
      return (
        <Tag data-color="danger" variant="outline">
          Klage svar KA
        </Tag>
      )

    case Oppgavetype.OMGJOERING:
      return (
        <Tag data-color="meta-purple" variant="moderate">
          Omgjøring
        </Tag>
      )

    case Oppgavetype.TILBAKEKREVING:
      return (
        <Tag data-color="info" variant="outline">
          Tilbakekreving
        </Tag>
      )

    case Oppgavetype.JOURNALFOERING:
      return (
        <Tag data-color="meta-lime" variant="outline">
          Journalføring
        </Tag>
      )

    case Oppgavetype.TILLEGGSINFORMASJON:
      return (
        <Tag data-color="meta-lime" variant="outline">
          Tilleggsinformasjon
        </Tag>
      )

    case Oppgavetype.OPPFOELGING:
      return (
        <Tag data-color="info" variant="strong">
          Oppfølging
        </Tag>
      )

    case Oppgavetype.ETTEROPPGJOER:
    case Oppgavetype.ETTEROPPGJOER_OPPRETT_REVURDERING:
      return (
        <Tag data-color="meta-purple" variant="strong">
          Etteroppgjør
        </Tag>
      )

    case Oppgavetype.GJENOPPRETTING_ALDERSOVERGANG:
      return (
        <Tag data-color="info" variant="moderate">
          Gjenoppretting
        </Tag>
      )
    case Oppgavetype.AKTIVITETSPLIKT:
    case Oppgavetype.AKTIVITETSPLIKT_12MND:
    case Oppgavetype.AKTIVITETSPLIKT_REVURDERING:
    case Oppgavetype.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK:
      return (
        <Tag data-color="info" variant="strong">
          Aktivitetsplikt
        </Tag>
      )
    case Oppgavetype.GENERELL_OPPGAVE:
      return (
        <Tag data-color="info" variant="outline">
          Generell oppgave
        </Tag>
      )
    case Oppgavetype.AARLIG_INNTEKTSJUSTERING:
      return (
        <Tag data-color="info" variant="outline">
          Årlig inntektsjustering
        </Tag>
      )
    case Oppgavetype.INNTEKTSOPPLYSNING:
      return (
        <Tag data-color="info" variant="outline">
          Inntektsopplysning
        </Tag>
      )
    case Oppgavetype.MANUELL_UTSENDING_BREV:
      return (
        <Tag data-color="info" variant="outline">
          Manuell brevutsending
        </Tag>
      )
    case Oppgavetype.MELDT_INN_ENDRING:
      return (
        <Tag data-color="meta-purple" variant="moderate">
          Meldt inn endring
        </Tag>
      )
    default:
      return (
        <Tag data-color="danger" variant="strong">
          Ukjent oppgave
        </Tag>
      )
  }
}
