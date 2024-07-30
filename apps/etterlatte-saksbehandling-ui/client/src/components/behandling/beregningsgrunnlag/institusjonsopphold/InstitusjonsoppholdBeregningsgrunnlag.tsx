import React, { useState } from 'react'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { HospitalIcon, PlusIcon } from '@navikt/aksel-icons'
import { HvaSkalRegistreresReadMore } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/HvaSkalRegistreresReadMore'
import { InstitusjonsoppholdBeregningsgrunnlagTable } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagTable'
import { BeregningsGrunnlagOMSPostDto, InstitusjonsoppholdGrunnlagDTO } from '~shared/types/Beregning'
import { InstitusjonsoppholdBeregningsgrunnlagSkjema } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema'
import { SakType } from '~shared/types/sak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  sakType: SakType
  beregningsgrunnlag?: BeregningsGrunnlagOMSPostDto | undefined
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
}

export const InstitusjonsoppholdBeregningsgrunnlag = ({
  redigerbar,
  behandling,
  sakType,
  beregningsgrunnlag,
  institusjonsopphold,
}: Props) => {
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
        <InstitusjonsoppholdBeregningsgrunnlagTable
          behandling={behandling}
          sakType={sakType}
          institusjonsopphold={institusjonsopphold}
          beregningsgrunnlag={beregningsgrunnlag}
        />
      </Box>

      {redigerbar && visInstitusjonsoppholdBeregningPeriodeSkjema ? (
        <InstitusjonsoppholdBeregningsgrunnlagSkjema
          behandling={behandling}
          sakType={sakType}
          beregningsgrunnlag={beregningsgrunnlag}
          institusjonsopphold={institusjonsopphold}
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
