import { Oppgavetype, Saktype } from '~shared/api/oppgaverny'
import { Tag } from '@navikt/ds-react'
import { Variants } from '~shared/Tags'

export const SaktypeTag = (props: { sakType: Saktype }) => {
  const { sakType } = props
  const getVariantAndTExt = (): [Variants, string] => {
    switch (sakType) {
      case 'BARNEPENSJON':
        return [Variants.INFO, 'Barnepensjon']
      case 'OMSTILLINGSSTOENAD':
        return [Variants.NEUTRAL, 'Omstillingsstønad']
    }
  }
  const variant = getVariantAndTExt()
  return (
    <>
      <Tag variant={variant[0]}>{variant[1]}</Tag>
    </>
  )
}

export const OppgavetypeTag = (props: { oppgavetype: Oppgavetype }) => {
  const { oppgavetype } = props

    const OPPGAVETYPE_TIL_TAGDATA: Record<Oppgavetype, { variant: Variants; text: string }> = {
        FOERSTEGANGSBEHANDLING: { variant: Variants.INFO, text: 'Førstegangsbehandling' },
        REVURDERING: { variant: Variants.NEUTRAL, text: 'Revurdering' },
        HENDELSE: { variant: Variants.ALT1, text: 'Hendelse' },
        MANUELT_OPPHOER: { variant: Variants.ALT2, text: 'Manuelt opphør' },
        EKSTERN: { variant: Variants.ALT3, text: 'Ekstern' },
        ATTESTERING: { variant: Variants.ALT3_FILLED, text: 'Attestering' },
    } as const

  const tagdata = OPPGAVETYPE_TIL_TAGDATA[oppgavetype]
    return (
      <>
          <Tag variant={tagdata.variant}>{tagdata.text}</Tag>
      </>
    )
}
