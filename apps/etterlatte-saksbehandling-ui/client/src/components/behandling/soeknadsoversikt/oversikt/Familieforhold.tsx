import { Heading } from '@navikt/ds-react'
import { Border } from '../styled'
import { PersonStatus, RelatertPersonsRolle } from '../types'
import { PersonInfo } from './personinfo/PersonInfo'
import { hentAlderVedDoedsdato } from './utils'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'

export const Familieforhold = () => {
  const {
    soekerPdl,
    soekerSoknad,
    dodsfall,
    avdodPersonPdl,
    avdodPersonSoknad,
    gjenlevendePdl,
    gjenlevendeSoknad,
    soekerFoedseldato,
    soekerBostedadresserPdl,
    avdoedBostedadresserPdl,
    gjenlevendeBostedadresserPdl,
    gjenlevendeForelderInfo,
  } = usePersonInfoFromBehandling()
  return (
    <>
      <Heading spacing size="small" level="5">
        Familieforhold
      </Heading>
      <Border />
      <PersonInfo
        person={{
          navn: `${soekerPdl?.fornavn} ${soekerPdl?.etternavn}`,
          personStatus: PersonStatus.BARN,
          rolle: RelatertPersonsRolle.BARN,
          adresser: soekerBostedadresserPdl,
          fnr: soekerPdl?.foedselsnummer,
          fnrFraSoeknad: soekerSoknad?.foedselsnummer,
          statsborgerskap: 'NO',
          alderEtterlatt: hentAlderVedDoedsdato(soekerFoedseldato, dodsfall?.doedsdato),
        }}
      />
      <PersonInfo
        person={{
          navn: `${gjenlevendePdl?.fornavn} ${gjenlevendePdl?.etternavn}`,
          personStatus: PersonStatus.GJENLEVENDE_FORELDER,
          rolle: RelatertPersonsRolle.FORELDER,
          adresser: gjenlevendeBostedadresserPdl,
          fnr: gjenlevendePdl?.foedselsnummer,
          fnrFraSoeknad: gjenlevendeSoknad?.foedselsnummer,
          adresseFraSoeknad: gjenlevendeForelderInfo?.adresse,
          statsborgerskap: 'NO',
        }}
      />
      <PersonInfo
        person={{
          navn: `${avdodPersonPdl?.fornavn} ${avdodPersonPdl?.etternavn}`,
          personStatus: PersonStatus.AVDOED,
          rolle: RelatertPersonsRolle.FORELDER,
          adresser: avdoedBostedadresserPdl,
          fnr: `${avdodPersonPdl?.foedselsnummer}`,
          fnrFraSoeknad: avdodPersonSoknad?.foedselsnummer,
          datoForDoedsfall: dodsfall?.doedsdato,
          statsborgerskap: 'NO',
        }}
      />
    </>
  )
}
