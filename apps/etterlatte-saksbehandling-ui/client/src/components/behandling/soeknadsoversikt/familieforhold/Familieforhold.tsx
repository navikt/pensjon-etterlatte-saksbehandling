import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Heading } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import React, { useEffect, useState } from 'react'
import { Personopplysninger } from '~shared/types/grunnlag'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold = ({ behandling }: PropsFamilieforhold) => {
  const [, fetchPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)
  const [personopplysninger, setPersonopplysninger] = useState<Personopplysninger | null>(null)

  useEffect(() => {
    fetchPersonopplysninger(behandling.id, (result) => setPersonopplysninger(result))
  }, [])

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
        <FamilieforholdOmstillingsstoenad behandling={behandling} personopplysninger={personopplysninger} />
      )}
    </>
  )
}
