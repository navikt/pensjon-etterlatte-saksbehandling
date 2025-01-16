import React from 'react'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Heading, VStack } from '@navikt/ds-react'
import { AktivitetsgradSakTabell } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/AktivitetsgradSakTabell'
import { UnntakPlainVisning } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/UnntakPlainVisning'

export const VurderingAvAktivitetspliktSak = ({
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

      <AktivitetsgradSakTabell aktiviteter={aktivitetspliktVurdering.aktivitet} />
      <UnntakPlainVisning unntaker={aktivitetspliktVurdering.unntak} />
    </VStack>
  )
}
