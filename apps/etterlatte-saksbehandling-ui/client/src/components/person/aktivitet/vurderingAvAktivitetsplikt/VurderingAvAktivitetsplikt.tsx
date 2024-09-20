import React from 'react'
import { IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
import { VStack } from '@navikt/ds-react'
import { Aktivitetsgrad } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Aktivitetsgrad'

export const VurderingAvAktivitetsplikt = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNy
}) => {
  return (
    <VStack gap="4">
      <Aktivitetsgrad aktiviteter={aktivitetspliktVurdering.aktivitet} />
    </VStack>
  )
}
