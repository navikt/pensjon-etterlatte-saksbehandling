import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import { HStack } from '@navikt/ds-react'
import { EtteroppgjoerStegLenke } from '~components/etteroppgjoer/stegmeny/EtteroppgjoerStegLenke'
import React from 'react'

export enum EtteroppjoerSteg {
  OVERSIKT = 'oversikt',
  OPPSUMMERING_OG_BREV = 'oppsummering',
}

export function EtteroppjoerForbehandlingSteg() {
  return (
    <StegMenyBox>
      <HStack gap="6" align="center">
        <EtteroppgjoerStegLenke path={EtteroppjoerSteg.OVERSIKT} description="Oversikt" enabled={true} />
        <EtteroppgjoerStegLenke
          path={EtteroppjoerSteg.OPPSUMMERING_OG_BREV}
          description="Brev"
          enabled={true}
          erSisteRoute
        />
      </HStack>
    </StegMenyBox>
  )
}
