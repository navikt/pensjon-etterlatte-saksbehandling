import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Box, Heading } from '@navikt/ds-react'
import React from 'react'
import { Personopplysninger } from '~shared/types/grunnlag'
import styled from 'styled-components'
import { RedigerFamilieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/RedigerFamilieforhold'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger | null
  redigerbar: boolean
}

export const Familieforhold = ({ behandling, personopplysninger, redigerbar }: PropsFamilieforhold) => {
  return (
    <>
      <Box paddingInline="16" paddingBlock="4">
        <Heading spacing size="medium" level="2" as="div">
          Familieforhold
          {personopplysninger && redigerbar && (
            <RedigerFamilieforhold behandling={behandling} personopplysninger={personopplysninger} />
          )}
        </Heading>
      </Box>

      <FamilieforholdWrapper>
        {behandling.sakType === SakType.BARNEPENSJON ? (
          <FamilieforholdBarnepensjon personopplysninger={personopplysninger} />
        ) : (
          <FamilieforholdOmstillingsstoenad personopplysninger={personopplysninger} />
        )}
      </FamilieforholdWrapper>
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 1em 4em;
  display: grid;
  gap: 4rem;
  margin-bottom: 4rem;
`
