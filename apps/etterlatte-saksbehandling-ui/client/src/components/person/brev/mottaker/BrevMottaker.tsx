import { AdresseType, IBrev } from '~shared/types/Brev'
import { Alert, Box, Heading, Tag } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getVergeadresseForPerson } from '~shared/api/grunnlag'
import { VergeFeilhaandtering } from '~components/person/VergeFeilhaandtering'
import { isSuccess } from '~shared/api/apiUtils'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { FlexRow } from '~shared/styled'
import { BrevMottakerModal } from '~components/person/brev/mottaker/BrevMottakerModal'

export function BrevMottaker({ brev, kanRedigeres }: { brev: IBrev; kanRedigeres: boolean }) {
  const [brevState, setBrevState] = useState<IBrev>(brev)

  const mottaker = brevState!.mottaker
  const adresse = mottaker?.adresse

  const [vergeadresse, getVergeadresse] = useApiCall(getVergeadresseForPerson)

  useEffect(() => {
    getVergeadresse(brev.soekerFnr)
  }, [brev])

  return (
    <Box padding="4" borderWidth="1" borderRadius="small">
      {isSuccess(vergeadresse) && (
        <Alert variant="info" size="small" inline>
          Søker har verge
        </Alert>
      )}
      <FlexRow justify="space-between">
        <Heading spacing level="2" size="medium">
          Mottaker{' '}
          {mottaker.adresse.adresseType === AdresseType.UTENLANDSKPOSTADRESSE && (
            <Tag variant="alt1" size="small">
              Utenlandsk adresse
            </Tag>
          )}
        </Heading>
        <div>
          {kanRedigeres && <BrevMottakerModal brev={brevState} setBrev={setBrevState} vergeadresse={vergeadresse} />}
        </div>
      </FlexRow>

      <InfoWrapper>
        <Info
          wide
          label="Navn"
          tekst={
            /[a-zA-Z\s]/.test(mottaker.navn) ? (
              mottaker.navn
            ) : (
              <Alert variant="error" size="small" inline>
                Navn mangler
              </Alert>
            )
          }
        />
        {mottaker.foedselsnummer && <Info label="Fødselsnummer" tekst={mottaker.foedselsnummer.value} wide />}
        {mottaker.orgnummer && <Info label="Org.nr." tekst={mottaker.orgnummer} wide />}

        <Info
          wide
          label="Adresse"
          tekst={
            <>
              {!adresse?.adresselinje1 && !adresse?.adresselinje2 && !adresse?.adresselinje3 ? (
                <Alert variant="warning" size="small" inline>
                  Adresselinjer mangler
                </Alert>
              ) : (
                [adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3]
                  .filter((linje) => !!linje)
                  .map((linje, i) => <div key={`adresselinje-${i}`}>{linje}</div>)
              )}
            </>
          }
        />

        <Info
          wide
          label="Postnummer-/sted"
          tekst={
            mottaker.adresse.adresseType !== AdresseType.UTENLANDSKPOSTADRESSE &&
            !adresse?.postnummer &&
            !adresse?.poststed ? (
              <Alert variant="warning" size="small" inline>
                Postnummer og -sted mangler
              </Alert>
            ) : (
              <>
                {adresse?.postnummer} {adresse?.poststed}
              </>
            )
          }
        />

        <Info
          wide
          label="Land"
          tekst={
            <>
              {adresse?.land || (
                <Alert variant="error" size="small" inline>
                  Land mangler
                </Alert>
              )}
              {!!adresse?.landkode ? (
                `(${adresse.landkode})`
              ) : (
                <Alert variant="error" size="small" inline>
                  Landkode mangler
                </Alert>
              )}
            </>
          }
        />
      </InfoWrapper>

      {VergeFeilhaandtering(vergeadresse)}
    </Box>
  )
}
