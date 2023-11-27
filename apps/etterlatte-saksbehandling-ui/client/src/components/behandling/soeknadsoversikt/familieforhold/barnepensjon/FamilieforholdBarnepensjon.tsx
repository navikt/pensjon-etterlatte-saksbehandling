import styled from 'styled-components'
import { Border } from '../../styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { ErrorMessage } from '@navikt/ds-react'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Person'
import { Soeskenliste } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Soeskenliste'
import { Personopplysninger } from '~shared/types/grunnlag'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger | null
}

export const FamilieforholdBarnepensjon = ({ behandling, personopplysninger }: PropsFamilieforhold) => {
  if (behandling.familieforhold == null || personopplysninger == null) {
    return (
      <FamilieforholdWrapper>
        <ErrorMessage>Familieforhold kan ikke hentes ut</ErrorMessage>
      </FamilieforholdWrapper>
    )
  }
  const soeker = personopplysninger.soeker
  const alleGjenlevende = personopplysninger.gjenlevende
  const alleAvdoede = personopplysninger.avdoede

  return (
    <>
      <FamilieforholdWrapper>
        <FamilieforholdVoksne>
          <Person person={soeker.opplysning} kilde={alleGjenlevende[0].kilde} mottaker />
          {alleAvdoede.map((avdoede) => (
            <Person person={avdoede.opplysning} kilde={avdoede.kilde} avdoed key={avdoede.id} />
          ))}
          {alleGjenlevende.map((gjenlevende) => (
            <Person person={gjenlevende.opplysning} kilde={gjenlevende.kilde} gjenlevende key={gjenlevende.id} />
          ))}
        </FamilieforholdVoksne>
        <Soeskenliste familieforhold={behandling.familieforhold!!} soekerFnr={soeker.opplysning.foedselsnummer} />
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
