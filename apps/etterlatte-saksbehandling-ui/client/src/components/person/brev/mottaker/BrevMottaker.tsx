import { AdresseType, IBrev } from '~shared/types/Brev'
import { Alert, Box, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { BrevMottakerModal } from '~components/person/brev/mottaker/BrevMottakerModal'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

export function BrevMottaker({ brev, kanRedigeres }: { brev: IBrev; kanRedigeres: boolean }) {
  const [brevState, setBrevState] = useState<IBrev>(brev)

  const mottaker = brevState!.mottaker
  const adresse = mottaker?.adresse

  const [soeker, getSoekerFraGrunnlag] = useApiCall(getGrunnlagsAvOpplysningstype)
  useEffect(() => {
    getSoekerFraGrunnlag({
      sakId: brev.sakId,
      behandlingId: brev.behandlingId,
      opplysningstype: 'SOEKER_PDL_V1',
    })
  }, [brev])

  return (
    <Box padding="4" borderWidth="1" borderRadius="small">
      {mapApiResult(
        soeker,
        <Spinner visible label="Henter eventuelle verger" margin="0" />,
        (error) => (
          <ApiErrorAlert>
            {error.detail || 'Feil oppsto ved henting av eventuelle verger. Prøv igjen senere'}
          </ApiErrorAlert>
        ),
        (soekeren) =>
          (soekeren?.opplysning?.vergemaalEllerFremtidsfullmakt || []) && (
            <Alert variant="info" size="small" inline>
              Søker har verge
            </Alert>
          )
      )}

      <HStack gap="4" justify="space-between">
        <Heading spacing level="2" size="medium">
          Mottaker{' '}
          {mottaker.adresse.adresseType === AdresseType.UTENLANDSKPOSTADRESSE && (
            <Tag variant="alt1" size="small">
              Utenlandsk adresse
            </Tag>
          )}
        </Heading>
        <div>{kanRedigeres && <BrevMottakerModal brev={brevState} setBrev={setBrevState} />}</div>
      </HStack>

      <VStack gap="4">
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
      </VStack>
    </Box>
  )
}
