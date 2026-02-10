import React from 'react'
import { ITrygdetidGrunnlag } from '~shared/api/trygdetid'
import { BodyShort, Box, HStack, Label } from '@navikt/ds-react'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

export const FaktiskTrygdetidExpandableRowContent = ({
  trygdetidPeriode,
}: {
  trygdetidPeriode: ITrygdetidGrunnlag
}) => {
  return (
    <HStack gap="space-8">
      <Box maxWidth="7rem">
        <Label>Begrunnelse</Label>
        <TekstMedMellomrom>{trygdetidPeriode.begrunnelse}</TekstMedMellomrom>
      </Box>
      <div>
        <Label>Poeng i inn år</Label>
        <BodyShort>{trygdetidPeriode.poengInnAar ? 'Ja' : 'Nei'}</BodyShort>
      </div>
      <div>
        <Label>Poeng i ut år</Label>
        <BodyShort>{trygdetidPeriode.poengUtAar ? 'Ja' : 'Nei'}</BodyShort>
      </div>
      <div>
        <Label>Ikke med i prorata</Label>
        <BodyShort>{trygdetidPeriode.prorata ? 'Nei' : 'Ja'}</BodyShort>
      </div>
    </HStack>
  )
}
