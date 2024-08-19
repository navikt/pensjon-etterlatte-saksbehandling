import styled from 'styled-components'
import { ErrorMessage } from '@navikt/ds-react'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Person'
import { Soeskenliste } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Soeskenliste'
import { Personopplysninger } from '~shared/types/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { SamsvarPersongalleri } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'
import { Result } from '~shared/api/apiUtils'

import { ILand } from '~utils/kodeverk'

export interface PropsFamilieforhold {
  personopplysninger: Personopplysninger | null
  landListeResult: Result<ILand[]>
}

export const FamilieforholdBarnepensjon = ({ personopplysninger, landListeResult }: PropsFamilieforhold) => {
  if (personopplysninger == null || personopplysninger.soeker == null) {
    return <ErrorMessage>Familieforhold kan ikke hentes ut</ErrorMessage>
  }
  const soeker = personopplysninger.soeker
  const alleGjenlevende = personopplysninger.gjenlevende
  const alleAvdoede = personopplysninger.avdoede
  const familieforhold: Familieforhold = { avdoede: alleAvdoede, gjenlevende: alleGjenlevende, soeker: soeker }

  return (
    <>
      <SamsvarPersongalleri landListeResult={landListeResult} />
      <FamilieforholdVoksne>
        <Person person={soeker.opplysning} kilde={soeker.kilde} landListeResult={landListeResult} mottaker />
        {alleAvdoede.map((avdoede) => (
          <Person
            person={avdoede.opplysning}
            kilde={avdoede.kilde}
            avdoed
            key={avdoede.id}
            landListeResult={landListeResult}
          />
        ))}
        {alleGjenlevende.map((gjenlevende) => (
          <Person
            person={gjenlevende.opplysning}
            kilde={gjenlevende.kilde}
            gjenlevende
            key={gjenlevende.id}
            landListeResult={landListeResult}
          />
        ))}
      </FamilieforholdVoksne>
      <Soeskenliste familieforhold={familieforhold} soekerFnr={soeker.opplysning.foedselsnummer} />
    </>
  )
}

const FamilieforholdVoksne = styled.div`
  display: flex;
`
