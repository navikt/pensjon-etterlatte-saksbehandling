import { Mottaker } from '~shared/types/Brev'
import { formaterDato, formaterFnr } from '~utils/formattering'
import React from 'react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { mapSuccess, Result } from '~shared/api/apiUtils'

interface Props {
  vergeadresseResult: Result<Mottaker>
}

export const VergeadressePanel = ({ vergeadresseResult }: Props) =>
  mapSuccess(vergeadresseResult, (vergeadresse) => {
    if (!vergeadresse) {
      return null
    }

    return (
      <InfoWrapper>
        <Info
          wide
          label="Verges adresse"
          tekst={
            <>
              {!vergeadresse.foedselsnummer && !vergeadresse.orgnummer && `Fødselsnummer/orgnummer: Ikke registrert.`}
              {!vergeadresse.foedselsnummer && !vergeadresse.orgnummer && <br />}
              {vergeadresse.foedselsnummer && `Fødselsnummer: ${formaterFnr(vergeadresse.foedselsnummer.value)}`}
              {vergeadresse.foedselsnummer && <br />}
              {vergeadresse.navn}
              {vergeadresse.navn && <br />}
              {vergeadresse.adresse.adresselinje1}
              {vergeadresse.adresse.adresselinje1 && <br />}
              {vergeadresse.adresse.adresselinje2}
              {vergeadresse.adresse.adresselinje2 && <br />}
              {vergeadresse.adresse.adresselinje3}
              {vergeadresse.adresse.adresselinje3 && <br />}
              {vergeadresse.adresse.postnummer} {vergeadresse.adresse.poststed} {vergeadresse.adresse.land} (
              {vergeadresse.adresse.landkode})
            </>
          }
          undertekst={'PDL: ' + formaterDato(new Date())}
        />
        <br />
      </InfoWrapper>
    )
  })
