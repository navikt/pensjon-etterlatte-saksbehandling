import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Heading } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import React, { useEffect } from 'react'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useAppDispatch } from '~store/Store'
import { setPersonopplysninger } from '~store/reducers/PersonopplysningerReducer'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold = ({ behandling }: PropsFamilieforhold) => {
  const [, fetchPersonopplysninger] = useApiCall(hentPersonopplysningerForBehandling)

  const personopplysninger = usePersonopplysninger()

  const dispatch = useAppDispatch()

  useEffect(() => {
    fetchPersonopplysninger({ behandlingId: behandling.id, sakType: behandling.sakType }, (result) =>
      dispatch(setPersonopplysninger(result))
    )
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
