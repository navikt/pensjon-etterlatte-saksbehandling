import { StegMenyBox } from '~components/behandling/StegMeny/stegmeny'
import { HStack } from '@navikt/ds-react'
import React from 'react'
import { AktivitetNavLenke } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktNavLenke'

export enum AktivitetspliktSteg {
  VURDERING = 'vurdering',
  BREV = 'brev',
}

export function AktivitetspliktStegmeny() {
  const skalBrevVises = true // TODO: logikk for om man skal sende brev

  return (
    <StegMenyBox>
      <HStack gap="6" align="center">
        <AktivitetNavLenke path={AktivitetspliktSteg.VURDERING} description="Oppfølging av aktivitet" enabled={true} />
        <AktivitetNavLenke path={AktivitetspliktSteg.BREV} description="Brev" enabled={skalBrevVises} erSisteRoute />
      </HStack>
    </StegMenyBox>
  )
}
