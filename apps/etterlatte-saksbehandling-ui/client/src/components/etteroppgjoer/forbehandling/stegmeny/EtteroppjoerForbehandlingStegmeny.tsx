import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import { HStack } from '@navikt/ds-react'
import { EtteroppgjoerForbehandlingStegLenke } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppgjoerForbehandlingStegLenke'
import React from 'react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { EtteroppgjoerResultatType } from '~shared/types/EtteroppgjoerForbehandling'

export enum EtteroppjoerForbehandlingSteg {
  OVERSIKT = 'oversikt',
  BREV = 'oppsummering',
}

export function EtteroppjoerForbehandlingStegmeny() {
  const etteroppgjoerForbehandling = useEtteroppgjoerForbehandling()

  function kanGaaTilEtteroppgjoerForbehandlingBrev(): boolean {
    return (
      !!etteroppgjoerForbehandling?.beregnetEtteroppgjoerResultat &&
      etteroppgjoerForbehandling.beregnetEtteroppgjoerResultat.resultatType !==
        EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING
    )
  }

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
          enabled={kanGaaTilEtteroppgjoerForbehandlingBrev()}
          erSisteRoute
        />
      </HStack>
    </StegMenyBox>
  )
}
