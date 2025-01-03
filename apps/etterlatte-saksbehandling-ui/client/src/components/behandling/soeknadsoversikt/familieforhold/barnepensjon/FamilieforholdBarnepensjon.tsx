import styled from 'styled-components'
import { ErrorMessage } from '@navikt/ds-react'
import { Person } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Person'
import { Soeskenliste } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/Soeskenliste'
import { Personopplysninger } from '~shared/types/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { SamsvarPersongalleri } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'
import { Result } from '~shared/api/apiUtils'

import { ILand } from '~utils/kodeverk'
import { AnnenForelderSkjema } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/AnnenForelderSkjema'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

export interface PropsFamilieforhold {
  personopplysninger: Personopplysninger | null
  landListeResult: Result<ILand[]>
  behandlingId: string
}

export const FamilieforholdBarnepensjon = ({
  personopplysninger,
  landListeResult,
  behandlingId,
}: PropsFamilieforhold) => {
  const enJuridiskForelderEnabled = useFeaturetoggle(FeatureToggle.kun_en_registrert_juridisk_forelder)
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
        {enJuridiskForelderEnabled && alleAvdoede.length == 1 && alleGjenlevende.length == 0 && (
          <AnnenForelderSkjema behandlingId={behandlingId} personopplysninger={personopplysninger} />
        )}
      </FamilieforholdVoksne>
      <Soeskenliste familieforhold={familieforhold} soekerFnr={soeker.opplysning.foedselsnummer} />
    </>
  )
}

const FamilieforholdVoksne = styled.div`
  display: flex;
`
