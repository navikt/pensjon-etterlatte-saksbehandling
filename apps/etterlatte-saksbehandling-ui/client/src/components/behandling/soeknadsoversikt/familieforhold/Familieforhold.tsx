import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Heading } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import React from 'react'
import { Personopplysninger } from '~shared/types/grunnlag'
import styled from 'styled-components'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { RedigerFamilieforhold } from '~components/behandling/soeknadsoversikt/familieforhold/RedigerFamilieforhold'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger | null
}

export const Familieforhold = ({ behandling, personopplysninger }: PropsFamilieforhold) => {
  return (
    <>
      <ContentHeader>
        <Heading spacing size="medium" level="2">
          Familieforhold
        </Heading>
      </ContentHeader>
      <FamilieforholdWrapper>
        {behandling.sakType === SakType.BARNEPENSJON ? (
          <FamilieforholdBarnepensjon personopplysninger={personopplysninger} />
        ) : (
          <FamilieforholdOmstillingsstoenad personopplysninger={personopplysninger} />
        )}
        {personopplysninger && (
          <RedigerFamilieforhold behandling={behandling} personopplysninger={personopplysninger} />
        )}
      </FamilieforholdWrapper>
      <Border />
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 1em 4em;
  display: grid;
  gap: 4rem;
  margin-bottom: 4rem;
`
