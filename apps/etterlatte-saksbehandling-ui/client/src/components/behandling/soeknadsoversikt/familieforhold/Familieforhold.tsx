import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { hentAlderVedDoedsdato } from '../utils'
import { PropsFamilieforhold } from '../props'
import { AvdoedForelder } from './personer/AvdoedForelder'
import { GjenlevendeForelder } from './personer/GjenlevendeForelder'
import { Barn } from './personer/Barn'
import { ContentHeader } from '../../../../shared/styled'
import { Border, DashedBorder } from '../styled'

export const Familieforhold: React.FC<PropsFamilieforhold> = ({
  soekerPdl,
  soekerSoknad,
  avdoedPersonPdl,
  avdodPersonSoknad,
  gjenlevendePdl,
  gjenlevendeSoknad,
  innsender,
}) => {
  return (
    <>
      <ContentHeader>
        <Heading spacing size="medium" level="5">
          Familieforhold
        </Heading>
      </ContentHeader>
      <FamilieforholdWrapper>
        <DashedBorder />
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
        <DashedBorder />
        <GjenlevendeForelder
          person={{
            navn: `${gjenlevendePdl?.fornavn} ${gjenlevendePdl?.etternavn}`,
            fnr: gjenlevendePdl?.foedselsnummer,
            statsborgerskap: gjenlevendePdl?.statsborgerskap,
            adresser: gjenlevendePdl?.bostedsadresse,
            fnrFraSoeknad: gjenlevendeSoknad?.foedselsnummer,
            adresseFraSoeknad: gjenlevendeSoknad?.adresse,
          }}
          innsenderErGjenlevende={innsender.foedselsnummer === gjenlevendePdl.foedselsnummer}
        />
        <DashedBorder />
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
      </FamilieforholdWrapper>
      <Border />
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 0em 5em;
`
