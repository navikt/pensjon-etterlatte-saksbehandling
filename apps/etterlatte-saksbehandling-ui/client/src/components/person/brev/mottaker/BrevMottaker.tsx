import { AdresseType, IBrev } from '~shared/types/Brev'
import { Alert, BodyShort, Box, Heading, HStack, Loader, Tag, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { BrevMottakerModal } from '~components/person/brev/mottaker/BrevMottakerModal'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { DigitalKontaktinformasjon, hentKontaktinformasjonKRR } from '~shared/api/krr'

export function BrevMottaker({ brev, kanRedigeres }: { brev: IBrev; kanRedigeres: boolean }) {
  const [brevState, setBrevState] = useState<IBrev>(brev)

  const mottaker = brevState!.mottaker
  const adresse = mottaker?.adresse

  const [soeker, getSoekerFraGrunnlag] = useApiCall(getGrunnlagsAvOpplysningstype)
  const [kontaktinfoResult, hentKontaktinfo] = useApiCall(hentKontaktinformasjonKRR)

  useEffect(() => {
    if (!brev.behandlingId) {
      return
    }

    getSoekerFraGrunnlag({
      sakId: brev.sakId,
      behandlingId: brev.behandlingId,
      opplysningstype: 'SOEKER_PDL_V1',
    })
  }, [brev])

  useEffect(() => {
    if (brev.mottaker.foedselsnummer?.value) {
      hentKontaktinfo(brev.mottaker.foedselsnummer.value)
    }
  }, [brev.mottaker.foedselsnummer?.value])

  return (
    <Box padding="4" borderWidth="1" borderRadius="small">
      {mapResult(soeker, {
        initial: kanRedigeres && (
          <Alert variant="info" size="small" inline>
            Sjekk om bruker har verge
          </Alert>
        ),
        pending: <Spinner label="Henter eventuelle verger" margin="0" />,
        error: (error) => (
          <ApiErrorAlert>
            {error.detail || 'Feil oppsto ved henting av eventuelle verger. Prøv igjen senere'}
          </ApiErrorAlert>
        ),
        success: (soekeren) =>
          (soekeren?.opplysning?.vergemaalEllerFremtidsfullmakt || []).length > 0 && (
            <Alert variant="info" size="small" inline>
              Søker har verge
            </Alert>
          ),
      })}

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

      {mapResult(kontaktinfoResult, {
        pending: <Loader />,
        success: (res?: DigitalKontaktinformasjon) => (
          <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
            <Heading size="xsmall" spacing>
              Kontakt- og reservasjonsregisteret
            </Heading>

            {res ? (
              <>
                <Info label="Kan varsles" tekst={res.kanVarsles ? 'Ja' : 'Nei'} />
                <Info label="Reservert mot digital kommunikasjon" tekst={res.reservert ? 'Ja' : 'Nei'} />
              </>
            ) : (
              'Ikke registrert i KRR'
            )}
          </Box>
        ),
      })}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
        <Heading size="xsmall">Distribusjonsmetode</Heading>
        <BodyShort>{mottaker.tvingSentralPrint ? 'Tving sentral print' : 'Automatisk'}</BodyShort>
      </Box>
    </Box>
  )
}
