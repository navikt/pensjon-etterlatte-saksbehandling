import React from 'react'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Heading, VStack } from '@navikt/ds-react'
import { Aktivitetsgrad } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Aktivitetsgrad'
import { Unntak } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Unntak'

export const VurderingAvAktivitetsplikt = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNyDto
}) => {
  return (
    <VStack gap="8">
      <VStack gap="2">
        <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
        <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
      </VStack>

      <Aktivitetsgrad aktiviteter={aktivitetspliktVurdering.aktivitet} />
      <Unntak unntaker={aktivitetspliktVurdering.unntak} />
    </VStack>
  )
}
