import styled from 'styled-components'
import { Border } from '../../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Person'
import { BarneListe } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/BarneListe'
import { Sivilstatus } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Sivilstatus'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const FamilieforholdOmstillingsstoenad: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  if (behandling.familieforhold == null || behandling.søker == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  return (
    <>
      {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT ? (
        <>
          <FamilieforholdWrapper>
            <FamilieforholdVoksne>
              <Person
                person={behandling.familieforhold.avdoede.opplysning}
                kilde={behandling.familieforhold.avdoede.kilde}
                avdoed
              />

              <Person
                person={behandling.familieforhold.gjenlevende.opplysning}
                kilde={behandling.familieforhold.gjenlevende.kilde}
              />
            </FamilieforholdVoksne>
            <Sivilstatus familieforhold={behandling.familieforhold!!} />
            <BarneListe familieforhold={behandling.familieforhold!!} />
          </FamilieforholdWrapper>
        </>
      ) : (
        <>
          <FamilieforholdWrapper>
            <Person
              person={behandling.familieforhold.gjenlevende.opplysning}
              kilde={behandling.familieforhold.gjenlevende.kilde}
            />
          </FamilieforholdWrapper>
        </>
      )}
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
