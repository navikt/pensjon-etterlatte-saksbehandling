import { Heading } from '@navikt/ds-react'
import { Border } from '../styled'
import { PersonStatus, RelatertPersonsRolle } from '../../types'
import { PersonInfo } from './personinfo/PersonInfo'
import { hentAlderVedDoedsdato } from './utils'
import { usePersonInfoFromBehandling } from '../usePersonInfoFromBehandling'

export const Familieforhold = () => {
  const { soekerPdl, avdoedPersonPdl, gjenlevendePdl, soekerSoknad, avdodPersonSoknad, gjenlevendeSoknad } =
    usePersonInfoFromBehandling()

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
          adresser: soekerPdl?.bostedsadresse,
          fnr: soekerPdl?.foedselsnummer,
          fnrFraSoeknad: soekerSoknad.foedselsnummer,
          datoForDoedsfall: avdoedPersonPdl.doedsdato,
          statsborgerskap: soekerPdl.statsborgerskap,
          alderEtterlatt: hentAlderVedDoedsdato(soekerPdl.foedselsdato, avdoedPersonPdl?.doedsdato),
        }}
      />
      <PersonInfo
        person={{
          navn: `${gjenlevendePdl?.fornavn} ${gjenlevendePdl?.etternavn}`,
          personStatus: PersonStatus.GJENLEVENDE_FORELDER,
          rolle: RelatertPersonsRolle.FORELDER,
          adresser: gjenlevendePdl.bostedsadresse,
          fnr: gjenlevendePdl?.foedselsnummer,
          fnrFraSoeknad: gjenlevendeSoknad.foedselsnummer,
          adresseFraSoeknad: gjenlevendeSoknad.adresse,
          datoForDoedsfall: avdoedPersonPdl.doedsdato,
          statsborgerskap: gjenlevendePdl.statsborgerskap,
        }}
      />
      <PersonInfo
        person={{
          navn: `${avdoedPersonPdl?.fornavn} ${avdoedPersonPdl?.etternavn}`,
          personStatus: PersonStatus.AVDOED,
          rolle: RelatertPersonsRolle.FORELDER,
          adresser: avdoedPersonPdl.bostedsadresse,
          fnr: `${avdoedPersonPdl?.foedselsnummer}`,
          fnrFraSoeknad: avdodPersonSoknad.foedselsnummer,
          datoForDoedsfall: avdoedPersonPdl.doedsdato,
          statsborgerskap: avdoedPersonPdl?.statsborgerskap,
        }}
      />
    </>
  )
}
