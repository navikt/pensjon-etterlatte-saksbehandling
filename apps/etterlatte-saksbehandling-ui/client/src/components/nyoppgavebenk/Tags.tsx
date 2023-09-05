import { Oppgavetype } from '~shared/api/oppgaverny'
import { Tag } from '@navikt/ds-react'
import { Variants } from '~shared/Tags'
import { SakType } from '~shared/types/sak'

export const SaktypeTag = (props: { sakType: SakType }) => {
  const { sakType } = props
  const SAKTYPE_TIL_TAGDATA: Record<SakType, { variant: Variants; text: string }> = {
    BARNEPENSJON: { variant: Variants.INFO, text: 'Barnepensjon' },
    OMSTILLINGSSTOENAD: { variant: Variants.NEUTRAL, text: 'Omstillingsstønad' },
  }
  const tagdata = SAKTYPE_TIL_TAGDATA[sakType]
  return (
    <>
      <Tag variant={tagdata.variant}>{tagdata.text}</Tag>
    </>
  )
}

export const OppgavetypeTag = (props: { oppgavetype: Oppgavetype }) => {
  const { oppgavetype } = props

  const OPPGAVETYPE_TIL_TAGDATA: Record<Oppgavetype, { variant: Variants; text: string }> = {
    FOERSTEGANGSBEHANDLING: { variant: Variants.INFO, text: 'Førstegangsbehandling' },
    REVURDERING: { variant: Variants.NEUTRAL, text: 'Revurdering' },
    VURDER_KONSEKVENS: { variant: Variants.ALT1, text: 'Hendelse' },
    MANUELT_OPPHOER: { variant: Variants.ALT2, text: 'Manuelt opphør' },
    ATTESTERING: { variant: Variants.ALT3_FILLED, text: 'Attestering' },
    UNDERKJENT: { variant: Variants.ALT3, text: 'Underkjent' },
    GOSYS: { variant: Variants.INFO_FILLED, text: 'Gosys-oppgave' },
    UTLAND: { variant: Variants.ALT2_FILLED, text: 'Utlandsoppgave' },
  } as const

  const tagdata = OPPGAVETYPE_TIL_TAGDATA[oppgavetype]
  return (
    <>
      <Tag variant={tagdata.variant}>{tagdata.text}</Tag>
    </>
  )
}
