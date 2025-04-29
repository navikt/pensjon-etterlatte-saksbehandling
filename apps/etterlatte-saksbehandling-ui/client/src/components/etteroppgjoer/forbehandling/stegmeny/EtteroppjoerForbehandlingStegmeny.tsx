import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import { HStack } from '@navikt/ds-react'
import { EtteroppgjoerForbehandlingStegLenke } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppgjoerForbehandlingStegLenke'
import React from 'react'

export enum EtteroppjoerForbehandlingSteg {
  OVERSIKT = 'oversikt',
  BREV = 'oppsummering',
}

export function EtteroppjoerForbehandlingStegmeny() {
  return (
    <StegMenyBox>
      <HStack gap="6" align="center">
        <EtteroppgjoerForbehandlingStegLenke
          path={EtteroppjoerForbehandlingSteg.OVERSIKT}
          description="Oversikt"
          enabled={true}
        />
        <EtteroppgjoerForbehandlingStegLenke
          path={EtteroppjoerForbehandlingSteg.BREV}
          description="Brev"
          enabled={true}
          erSisteRoute
        />
      </HStack>
    </StegMenyBox>
  )
}
