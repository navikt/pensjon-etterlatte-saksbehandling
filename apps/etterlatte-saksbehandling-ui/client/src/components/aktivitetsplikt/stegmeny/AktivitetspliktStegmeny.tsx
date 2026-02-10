import { StegMenyBox } from '~components/behandling/stegmeny/StegMeny'
import { HStack } from '@navikt/ds-react'
import React from 'react'
import { AktivitetNavLenke } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktNavLenke'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'

export enum AktivitetspliktSteg {
  VURDERING = 'vurdering',
  BREVVALG = 'brevvalg',
  OPPSUMMERING_OG_BREV = 'oppsummering',
}

export function AktivitetspliktStegmeny() {
  const { aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()

  const skalOppsummeringVises =
    (!!aktivtetspliktbrevdata && !!aktivtetspliktbrevdata.brevId) || aktivtetspliktbrevdata?.skalSendeBrev !== undefined

  return (
    <StegMenyBox>
      <HStack gap="space-6" align="center">
        <AktivitetNavLenke path={AktivitetspliktSteg.VURDERING} description="Oppfølging av aktivitet" enabled={true} />
        <AktivitetNavLenke path={AktivitetspliktSteg.BREVVALG} description="Brevvalg" enabled={true} />
        <AktivitetNavLenke
          path={AktivitetspliktSteg.OPPSUMMERING_OG_BREV}
          description="Brev og oppsummering"
          enabled={skalOppsummeringVises}
          erSisteRoute
        />
      </HStack>
    </StegMenyBox>
  )
}
