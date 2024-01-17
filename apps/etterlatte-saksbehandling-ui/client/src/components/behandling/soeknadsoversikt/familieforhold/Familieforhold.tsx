import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Heading } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import React from 'react'
import { Personopplysninger } from '~shared/types/grunnlag'

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
      {behandling.sakType === SakType.BARNEPENSJON ? (
        <FamilieforholdBarnepensjon personopplysninger={personopplysninger} />
      ) : (
        <FamilieforholdOmstillingsstoenad personopplysninger={personopplysninger} />
      )}
    </>
  )
}
