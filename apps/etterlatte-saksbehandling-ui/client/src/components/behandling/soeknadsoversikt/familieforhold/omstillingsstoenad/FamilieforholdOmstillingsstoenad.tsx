import styled from 'styled-components'
import { Border } from '../../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Person'
import { BarneListe } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/BarneListe'
import { Sivilstand } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Sivilstand'
import { ErrorMessage } from '@navikt/ds-react'
import { Personopplysninger } from '~shared/types/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { SamsvarPersongalleri } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger | null
}

export const FamilieforholdOmstillingsstoenad = ({ behandling, personopplysninger }: PropsFamilieforhold) => {
  if (personopplysninger == null || personopplysninger.soeker == null) {
    return (
      <FamilieforholdWrapper>
        <ErrorMessage>Familieforhold kan ikke hentes ut</ErrorMessage>
      </FamilieforholdWrapper>
    )
  }

  const soeker = personopplysninger.soeker
  const gjenlevende = personopplysninger.gjenlevende
  const avdoede = personopplysninger.avdoede
  const familieforhold: Familieforhold = { avdoede: avdoede, gjenlevende: gjenlevende }

  return (
    <>
      <FamilieforholdWrapper>
        {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT ? (
          <>
            <SamsvarPersongalleri />
            <FamilieforholdVoksne>
              {avdoede.map((avdoed) => (
                <Person person={avdoed.opplysning} kilde={avdoed.kilde} avdoed key={avdoed.id} />
              ))}
              <Person person={soeker.opplysning} kilde={soeker.kilde} />
            </FamilieforholdVoksne>
            {avdoede.map((avd) => (
              <Sivilstand familieforhold={familieforhold} avdoed={avd.opplysning} key={avd.id} />
            ))}
            <BarneListe familieforhold={familieforhold} />
          </>
        ) : (
          gjenlevende.map((person) => <Person person={person.opplysning} kilde={person.kilde} key={person.id} />)
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
