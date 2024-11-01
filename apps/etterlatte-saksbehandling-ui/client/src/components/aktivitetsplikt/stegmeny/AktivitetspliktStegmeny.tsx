import { StegMenyBox } from '~components/behandling/StegMeny/stegmeny'
import { HStack } from '@navikt/ds-react'
import React from 'react'
import { AktivitetNavLenke } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktNavLenke'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export enum AktivitetspliktSteg {
  VURDERING = 'vurdering',
  BREV = 'brev',
}

export function AktivitetspliktStegmeny() {
  const { aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()

  const skalBrevVises =
    (!!aktivtetspliktbrevdata && !!aktivtetspliktbrevdata.brevId) || !!aktivtetspliktbrevdata?.skalSendeBrev //Implisitt at utbetaling og redusertEtterInntekt er satt

  return (
    <StegMenyBox>
      <HStack gap="6" align="center">
        <AktivitetNavLenke path={AktivitetspliktSteg.VURDERING} description="Oppfølging av aktivitet" enabled={true} />
        <AktivitetNavLenke path={AktivitetspliktSteg.BREV} description="Brev" enabled={skalBrevVises} erSisteRoute />
      </HStack>
    </StegMenyBox>
  )
}
