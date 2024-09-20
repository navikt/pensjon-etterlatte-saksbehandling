import React from 'react'
import { IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
import { VStack } from '@navikt/ds-react'
import { Aktivitetsgrad } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Aktivitetsgrad'
import { Unntak } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Unntak'

export const VurderingAvAktivitetsplikt = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNy
}) => {
  return (
    <VStack gap="8">
      <Aktivitetsgrad aktiviteter={aktivitetspliktVurdering.aktivitet} />
      <Unntak unntaker={aktivitetspliktVurdering.unntak} />
    </VStack>
  )
}
