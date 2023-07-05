import styled from 'styled-components'
import { Border } from '../../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { ErrorMessage } from '@navikt/ds-react'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Person'
import { BarneListe } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/BarneListe'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const FamilieforholdBarnepensjon = ({ behandling }: PropsFamilieforhold) => {
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
              <Person person={behandling.søker} kilde={gjenlevende.kilde} mottaker />
              <Person person={avdoede.opplysning} kilde={avdoede.kilde} avdoed />
              <Person person={gjenlevende.opplysning} kilde={gjenlevende.kilde} gjenlevende />
            </FamilieforholdVoksne>
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
