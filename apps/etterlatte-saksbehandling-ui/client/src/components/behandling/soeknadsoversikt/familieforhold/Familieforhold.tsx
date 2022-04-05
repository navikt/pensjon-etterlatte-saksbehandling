import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { hentAlderVedDoedsdato } from '../utils'
import { PropsFamilieforhold } from '../props'
import { AvdoedForelder } from './personer/AvdoedForelder'
import { GjenlevendeForelder } from './personer/GjenlevendeForelder'
import { Barn } from './personer/Barn'

export const Familieforhold: React.FC<PropsFamilieforhold> = ({
  soekerPdl,
  soekerSoknad,
  avdoedPersonPdl,
  avdodPersonSoknad,
  gjenlevendePdl,
  gjenlevendeSoknad,
}) => {
  return (
    <>
      <Heading spacing size="small" level="5">
        Familieforhold
      </Heading>
      <Border />
      <Barn
        person={{
          navn: `${soekerPdl?.fornavn} ${soekerPdl?.etternavn}`,
          fnr: soekerPdl?.foedselsnummer,
          statsborgerskap: soekerPdl?.statsborgerskap,
          adresser: soekerPdl?.bostedsadresse,
          alderEtterlatt: hentAlderVedDoedsdato(soekerPdl?.foedselsdato, avdoedPersonPdl?.doedsdato),
          fnrFraSoeknad: soekerSoknad?.foedselsnummer,
        }}
      />
      <GjenlevendeForelder
        person={{
          navn: `${gjenlevendePdl?.fornavn} ${gjenlevendePdl?.etternavn}`,
          fnr: gjenlevendePdl?.foedselsnummer,
          statsborgerskap: gjenlevendePdl?.statsborgerskap,
          adresser: gjenlevendePdl?.bostedsadresse,
          fnrFraSoeknad: gjenlevendeSoknad?.foedselsnummer,
          adresseFraSoeknad: gjenlevendeSoknad?.adresse,
        }}
      />
      <AvdoedForelder
        person={{
          navn: `${avdoedPersonPdl?.fornavn} ${avdoedPersonPdl?.etternavn}`,
          fnr: avdoedPersonPdl?.foedselsnummer,
          statsborgerskap: avdoedPersonPdl?.statsborgerskap,
          adresser: avdoedPersonPdl?.bostedsadresse,
          fnrFraSoeknad: avdodPersonSoknad?.foedselsnummer,
          datoForDoedsfall: avdoedPersonPdl?.doedsdato,
        }}
      />
    </>
  )
}

export const Border = styled.div`
  border-top: 1px solid #b0b0b0;
`
