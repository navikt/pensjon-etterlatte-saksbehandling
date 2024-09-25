import { formaterVedtaksResultat, VedtakResultat } from '~components/behandling/useVedtaksResultat'
import { BodyShort } from '@navikt/ds-react'
import React from 'react'

export const Vilkaarsresultat = (props: {
  vedtaksresultat: VedtakResultat | null
  virkningstidspunktFormatert: string | undefined
}) => {
  return (
    <BodyShort spacing>
      Vilkårsresultat:{' '}
      <strong>{formaterVedtaksResultat(props.vedtaksresultat, props.virkningstidspunktFormatert)}</strong>
    </BodyShort>
  )
}
