import { Tag } from '@navikt/ds-react'
import { Variants } from '~shared/Tags'
import { SakType } from '~shared/types/sak'
import { Oppgavetype } from '~shared/types/oppgave'
import { GosysTema } from '~shared/types/Gosys'

const SAKTYPE_TIL_TAGDATA: Record<SakType, { variant: Variants; text: string }> = {
  BARNEPENSJON: { variant: Variants.INFO, text: 'Barnepensjon' },
  OMSTILLINGSSTOENAD: { variant: Variants.NEUTRAL, text: 'Omstillingsstønad' },
} as const

export const SaktypeTag = (props: { sakType: SakType }) => {
  const { sakType } = props
  const tagdata = SAKTYPE_TIL_TAGDATA[sakType]
  return <Tag variant={tagdata.variant}>{tagdata.text}</Tag>
}

const OPPGAVETYPE_TIL_TAGDATA: Record<Oppgavetype, { variant: Variants; text: string }> = {
  FOERSTEGANGSBEHANDLING: { variant: Variants.INFO, text: 'Førstegangsbehandling' },
  REVURDERING: { variant: Variants.NEUTRAL, text: 'Revurdering' },
  VURDER_KONSEKVENS: { variant: Variants.ALT1, text: 'Hendelse' },
  KRAVPAKKE_UTLAND: { variant: Variants.ALT2_FILLED, text: 'Kravpakke utland' },
  KLAGE: { variant: Variants.ALT2, text: 'Klage' },
  OMGJOERING: { variant: Variants.ALT2_MODERATE, text: 'Omgjøring' },
  TILBAKEKREVING: { variant: Variants.ALT2, text: 'Tilbakekreving' },
  JOURNALFOERING: { variant: Variants.ALT2, text: 'Journalføring' },
  GJENOPPRETTING_ALDERSOVERGANG: { variant: Variants.ALT3_FILLED, text: 'Gjenoppretting' },
  AKTIVITETSPLIKT: { variant: Variants.ALT3_FILLED, text: 'Aktivitetsplikt' },
  AKTIVITETSPLIKT_REVURDERING: { variant: Variants.ALT3_FILLED, text: 'Aktivitetsplikt' },
} as const

export const OppgavetypeTag = (props: { oppgavetype: Oppgavetype }) => {
  const { oppgavetype } = props

  const tagdata = OPPGAVETYPE_TIL_TAGDATA[oppgavetype]
  if (tagdata) {
    return <Tag variant={tagdata.variant}>{tagdata.text}</Tag>
  } else {
    return <Tag variant={Variants.ALT1_FILLED}>Ukjent oppgave</Tag>
  }
}

export const TemaTag = ({ tema }: { tema: GosysTema }) => {
  switch (tema) {
    case 'EYO':
      return <Tag variant={Variants.ALT2}>Omstillingsstønad</Tag>
    case 'EYB':
      return <Tag variant={Variants.INFO}>Barnepensjon</Tag>
    case 'PEN':
      return <Tag variant={Variants.ALT1}>Pensjon</Tag>
  }
}
