import React, { useState } from 'react'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { HospitalIcon, PlusIcon } from '@navikt/aksel-icons'
import { HvaSkalRegistreresReadMore } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/HvaSkalRegistreresReadMore'
import { InstitusjonsoppholdBeregningsgrunnlagTable } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagTable'
import { BeregningsGrunnlagDto, InstitusjonsoppholdGrunnlagDTO } from '~shared/types/Beregning'
import { InstitusjonsoppholdBeregningsgrunnlagSkjema } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema'
import { SakType } from '~shared/types/sak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { InstitusjonsoppholdBeregningsgrunnlagReadMoreBP } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagReadMoreBP'
import { InstitusjonsoppholdBeregningsgrunnlagReadMoreOMS } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagReadMoreOMS'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  sakType: SakType
  beregningsgrunnlag?: BeregningsGrunnlagDto
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
    <VStack gap="space-4">
      <HStack gap="space-2" align="center">
        <HospitalIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Beregningsgrunnlag for institusjonsopphold
        </Heading>
      </HStack>
      <VStack maxWidth="42.5rem">
        {sakType === SakType.BARNEPENSJON && <InstitusjonsoppholdBeregningsgrunnlagReadMoreBP />}
        {sakType === SakType.OMSTILLINGSSTOENAD && <InstitusjonsoppholdBeregningsgrunnlagReadMoreOMS />}
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

      {redigerbar &&
        (visInstitusjonsoppholdBeregningPeriodeSkjema ? (
          <InstitusjonsoppholdBeregningsgrunnlagSkjema
            sakType={sakType}
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
        ))}
    </VStack>
  )
}
