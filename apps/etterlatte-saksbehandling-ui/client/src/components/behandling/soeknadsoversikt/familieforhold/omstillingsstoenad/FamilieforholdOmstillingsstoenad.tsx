import styled from 'styled-components'
import { Border } from '../../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Person'
import { BarneListe } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/BarneListe'
import { Sivilstatus } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Sivilstatus'
import { ErrorMessage } from '@navikt/ds-react'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const FamilieforholdOmstillingsstoenad: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  if (behandling.familieforhold == null || behandling.søker == null) {
    return (
      <FamilieforholdWrapper>
        <ErrorMessage>Familieforhold kan ikke hentes ut</ErrorMessage>
      </FamilieforholdWrapper>
    )
  }

  const gjenlevende = behandling.familieforhold.gjenlevende
  const avdoede = behandling.familieforhold.avdoede

  return (
    <>
      <FamilieforholdWrapper>
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT ? (
          <>
            <FamilieforholdVoksne>
              <Person person={avdoede.opplysning} kilde={avdoede.kilde} avdoed />

              <Person person={gjenlevende.opplysning} kilde={gjenlevende.kilde} />
            </FamilieforholdVoksne>
            <Sivilstatus familieforhold={behandling.familieforhold!!} avdoed={avdoede.opplysning} />
            <BarneListe familieforhold={behandling.familieforhold!!} />
          </>
        ) : (
          <Person person={gjenlevende.opplysning} kilde={gjenlevende.kilde} />
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

const FamilieforholdVoksne = styled.div`
  display: flex;
`
