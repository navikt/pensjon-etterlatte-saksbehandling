import React, { useState } from 'react'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { HospitalIcon, PlusIcon } from '@navikt/aksel-icons'
import { HvaSkalRegistreresReadMore } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/HvaSkalRegistreresReadMore'
import { InstitusjonsoppholdBeregningsgrunnlagTable } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagTable'
import { InstitusjonsoppholdGrunnlagDTO } from '~shared/types/Beregning'
import { mapListeFraDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { InstitusjonsoppholdBeregningsgrunnlagSkjema } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema'

interface Props {
  redigerbar: boolean
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
}

export const InstitusjonsoppholdBeregningsgrunnlag = ({ redigerbar, institusjonsopphold }: Props) => {
  const [visInstitusjonsoppholdBeregningPeriodeSkjema, setVisInstitusjonsoppholdBeregningPeriodeSkjema] =
    useState<boolean>(false)

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

      {redigerbar && visInstitusjonsoppholdBeregningPeriodeSkjema ? (
        <InstitusjonsoppholdBeregningsgrunnlagSkjema
          paaAvbryt={() => setVisInstitusjonsoppholdBeregningPeriodeSkjema(false)}
          paaLagre={() => setVisInstitusjonsoppholdBeregningPeriodeSkjema(false)}
        />
      ) : (
        <div>
          <Button
            size="small"
            variant="secondary"
            icon={<PlusIcon aria-hidden />}
            onClick={() => setVisInstitusjonsoppholdBeregningPeriodeSkjema(true)}
          >
            Ny periode
          </Button>
        </div>
      )}
    </VStack>
  )
}
