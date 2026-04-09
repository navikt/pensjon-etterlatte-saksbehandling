import React from 'react'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Heading, VStack } from '@navikt/ds-react'
import { AktivitetsgradOgUnntakSakTabell } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/AktivitetsgradOgUnntakSakTabell'

export const VurderingAvAktivitetspliktSak = ({
  aktivitetspliktVurdering,
}: {
  aktivitetspliktVurdering: IAktivitetspliktVurderingNyDto
}) => {
  return (
    <VStack gap="space-32">
      <VStack gap="space-8">
        <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
        <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
      </VStack>
      <AktivitetsgradOgUnntakSakTabell
        aktiviteter={aktivitetspliktVurdering.aktivitet}
        unntak={aktivitetspliktVurdering.unntak}
      />
    </VStack>
  )
}
