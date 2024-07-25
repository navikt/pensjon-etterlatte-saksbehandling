import React from 'react'
import { Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { HospitalIcon } from '@navikt/aksel-icons'
import { HvaSkalRegistreresReadMore } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/HvaSkalRegistreresReadMore'
import { InstitusjonsoppholdBeregningsgrunnlagTable } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagTable'
import { InstitusjonsoppholdGrunnlagDTO } from '~shared/types/Beregning'
import { mapListeFraDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

interface Props {
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
}

export const InstitusjonsoppholdBeregningsgrunnlag = ({ institusjonsopphold }: Props) => {
  console.log(institusjonsopphold)

  return (
    <VStack gap="4">
      <HStack gap="2" align="center">
        <HospitalIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Beregningsgrunnlag for institusjonsopphold
        </Heading>
      </HStack>
      <VStack gap="2" maxWidth="42.5rem">
        <HvaSkalRegistreresReadMore />
      </VStack>

      <Box maxWidth="60rem">
        <InstitusjonsoppholdBeregningsgrunnlagTable institusjonsopphold={mapListeFraDto(institusjonsopphold ?? [])} />
      </Box>
    </VStack>
  )
}
