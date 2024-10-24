import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Person'
import { BarneListe } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/BarneListe'
import { Sivilstand } from '~components/behandling/soeknadsoversikt/familieforhold/omstillingsstoenad/Sivilstand'
import { ErrorMessage, HStack } from '@navikt/ds-react'
import { Personopplysninger } from '~shared/types/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { SamsvarPersongalleri } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'
import { Result } from '~shared/api/apiUtils'

import { ILand } from '~utils/kodeverk'

export interface PropsFamilieforhold {
  personopplysninger: Personopplysninger | null
  landListeResult: Result<ILand[]>
}

export const FamilieforholdOmstillingsstoenad = ({ personopplysninger, landListeResult }: PropsFamilieforhold) => {
  if (personopplysninger == null || personopplysninger.soeker == null) {
    return <ErrorMessage>Familieforhold kan ikke hentes ut</ErrorMessage>
  }

  const soeker = personopplysninger.soeker
  const gjenlevende = personopplysninger.gjenlevende
  const avdoede = personopplysninger.avdoede
  const familieforhold: Familieforhold = { avdoede: avdoede, gjenlevende: gjenlevende, soeker: soeker }

  return (
    <>
      <SamsvarPersongalleri landListeResult={landListeResult} />
      <HStack gap="4">
        {avdoede.map((avdoed) => (
          <Person
            person={avdoed.opplysning}
            kilde={avdoed.kilde}
            avdoed
            key={avdoed.id}
            landListeResult={landListeResult}
          />
        ))}
        <Person person={soeker.opplysning} kilde={soeker.kilde} landListeResult={landListeResult} />
      </HStack>
      {avdoede.map((avd) => (
        <Sivilstand familieforhold={familieforhold} avdoed={avd.opplysning} key={avd.id} />
      ))}
      <BarneListe familieforhold={familieforhold} />
    </>
  )
}
