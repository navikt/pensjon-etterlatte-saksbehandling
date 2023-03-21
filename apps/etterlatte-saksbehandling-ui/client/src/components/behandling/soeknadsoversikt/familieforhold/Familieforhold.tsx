import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { FamilieforholdBarnepensjon } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import { FamilieforholdOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/FamilieforholdOmstillingsstoenad'
import { Heading } from '@navikt/ds-react'
import { ContentHeader } from '~shared/styled'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  return (
    <>
      <ContentHeader>
        <Heading spacing size="medium" level="2">
          Familieforhold
        </Heading>
      </ContentHeader>
      {behandling.sakType === ISaksType.BARNEPENSJON ? (
        <FamilieforholdBarnepensjon behandling={behandling} />
      ) : (
        <FamilieforholdOmstillingsstoenad behandling={behandling} />
      )}
    </>
  )
}
