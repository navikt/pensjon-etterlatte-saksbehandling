import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import { HStack } from '@navikt/ds-react'
import { EtteroppgjoerForbehandlingStegLenke } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppgjoerForbehandlingStegLenke'
import React from 'react'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import {
  EtteroppgjoerDetaljertForbehandling,
  EtteroppgjoerResultatType,
} from '~shared/types/EtteroppgjoerForbehandling'

export enum EtteroppjoerForbehandlingSteg {
  OVERSIKT = 'oversikt',
  BREV = 'oppsummering',
}

function kanGaaTilEtteroppgjoerBrev(forbehandling?: EtteroppgjoerDetaljertForbehandling): boolean {
  return (
    !!forbehandling?.beregnetEtteroppgjoerResultat &&
    forbehandling.beregnetEtteroppgjoerResultat.resultatType !== EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING
  )
}

export function EtteroppjoerForbehandlingStegmeny() {
  const etteroppgjoer = useEtteroppgjoer()
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
          enabled={kanGaaTilEtteroppgjoerBrev(etteroppgjoer)}
          erSisteRoute
        />
      </HStack>
    </StegMenyBox>
  )
}
