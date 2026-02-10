import { formaterDato } from '~utils/formatering/dato'
import { useVedtaksResultat } from '../useVedtaksResultat'
import React from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Box, Heading } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { Vedtaksresultat } from '~components/behandling/felles/Vedtaksresultat'
import { BeregneBP } from '~components/behandling/beregne/BeregneBP'
import { BeregneOMS } from '~components/behandling/beregne/BeregneOMS'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { BeregneEtteroppgjoerOMS } from '~components/behandling/beregne/BeregneEtteroppgjoerOMS'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterDato(behandling.virkningstidspunkt.dato)
    : undefined

  const vedtaksresultat = useVedtaksResultat()

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading spacing size="large" level="1">
          Beregning og vedtak
        </Heading>
        <Vedtaksresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>
      {(() => {
        switch (behandling.sakType) {
          case SakType.BARNEPENSJON:
            return <BeregneBP behandling={behandling} />
          case SakType.OMSTILLINGSSTOENAD:
            if (behandling.revurderingsaarsak == Revurderingaarsak.ETTEROPPGJOER) return <BeregneEtteroppgjoerOMS />
            else return <BeregneOMS />
        }
      })()}
    </>
  )
}
