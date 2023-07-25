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

  const getVariantAndTExt = (): [Variants, string] => {
    switch (oppgavetype) {
      case 'FOERSTEGANGSBEHANDLING':
        return [Variants.INFO, 'Førstegangsbehandling']
      case 'REVURDERING':
        return [Variants.NEUTRAL, 'Revurdering']
      case 'HENDELSE':
        return [Variants.ALT1, 'Hendelse']
      case 'MANUELT_OPPHOER':
        return [Variants.ALT2, 'Manuelt opphør']
      case 'EKSTERN':
        return [Variants.ALT3, 'Ekstern']
      case 'ATTESTERING':
        return [Variants.ALT3_FILLED, 'Attestering']
    }
  }

  const variant = getVariantAndTExt()
  return (
    <>
      <Tag variant={variant[0]}>{variant[1]}</Tag>
    </>
  )
}
