import { AdresseType, Mottaker, MottakerType } from '~shared/types/Brev'
import { Alert, BodyShort, Box, Button, Heading, HStack, Loader, Tag, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { RedigerMottakerModal } from '~components/person/brev/mottaker/RedigerMottakerModal'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, mapFailure, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { DigitalKontaktinformasjon, hentKontaktinformasjonKRR } from '~shared/api/krr'
import { oppdaterMottaker, settHovedmottaker } from '~shared/api/brev'
import { SlettMottakerModal } from '~components/person/brev/mottaker/SlettMottakerModal'
import { PersonCheckmarkIcon } from '@navikt/aksel-icons'

interface MottakerProps {
  brevId: number
  behandlingId?: string
  sakId: number
  mottaker: Mottaker
  kanRedigeres: boolean
  flereMottakere?: boolean
  fjernMottaker?: (mottakerId: string) => void
}

const formaterMottakerType = (type: MottakerType) => {
  switch (type) {
    case MottakerType.HOVED:
      return 'Hovedmottaker'
    case MottakerType.KOPI:
      return 'Kopimottaker'
  }
}

export function BrevMottakerPanel({
  brevId,
  behandlingId,
  sakId,
  mottaker: initialMottaker,
  kanRedigeres,
  flereMottakere,
  fjernMottaker,
}: MottakerProps) {
  const [mottaker, setMottaker] = useState(initialMottaker)

  const [oppdaterMottakerResult, apiOppdaterMottaker] = useApiCall(oppdaterMottaker)
  const [settHovedmottakerResult, apiSettHovedmottaker] = useApiCall(settHovedmottaker)
  const [soeker, getSoekerFraGrunnlag] = useApiCall(getGrunnlagsAvOpplysningstype)
  const [kontaktinfoResult, hentKontaktinfo] = useApiCall(hentKontaktinformasjonKRR)

  useEffect(() => {
    if (!behandlingId) {
      return
    }

    getSoekerFraGrunnlag({
      sakId: sakId,
      behandlingId: behandlingId,
      opplysningstype: 'SOEKER_PDL_V1',
    })
  }, [])

  useEffect(() => {
    if (mottaker.foedselsnummer?.value && !mottaker.bestillingId) {
      hentKontaktinfo(mottaker.foedselsnummer.value)
    }
  }, [mottaker.foedselsnummer?.value])

  const oppdater = (brevId: number, sakId: number, mottaker: Mottaker, onSuccess: Function) => {
    apiOppdaterMottaker({ brevId, sakId, mottaker }, () => {
      setMottaker(mottaker)
      onSuccess()
    })
  }

  return (
    <Box padding="4" borderWidth="1" borderRadius="small">
      {mapResult(soeker, {
        initial: kanRedigeres && mottaker.type === MottakerType.HOVED && (
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
              Brevet skal sendes til verge. Registrer riktig adresse.
            </Alert>
          ),
      })}

      <HStack gap="2" justify="space-between">
        <Heading spacing level="2" size="medium">
          {flereMottakere ? formaterMottakerType(mottaker.type) : 'Mottaker'}

          {mottaker.adresse.adresseType === AdresseType.UTENLANDSKPOSTADRESSE && (
            <Tag variant="alt1" size="small">
              Utenlandsk adresse
            </Tag>
          )}
        </Heading>

        {kanRedigeres && (
          <HStack gap="4" align="start">
            {mottaker.type === MottakerType.KOPI && !!fjernMottaker && (
              <SlettMottakerModal
                brevId={brevId}
                sakId={sakId}
                mottakerId={mottaker.id}
                fjernMottaker={fjernMottaker}
              />
            )}

            <RedigerMottakerModal
              brevId={brevId}
              sakId={sakId}
              mottaker={mottaker}
              lagre={oppdater}
              lagreResult={oppdaterMottakerResult}
            />
          </HStack>
        )}
      </HStack>

      {flereMottakere ? <MottakerInnholdKompakt mottaker={mottaker} /> : <MottakerInnhold mottaker={mottaker} />}

      {mapResult(kontaktinfoResult, {
        pending: <Loader />,
        success: (res?: DigitalKontaktinformasjon) => (
          <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
            <Heading size="xsmall" spacing>
              Kontakt- og reservasjonsregisteret
            </Heading>

            {res ? (
              <VStack gap="2">
                <Info label="Kan varsles" tekst={res.kanVarsles ? 'Ja' : 'Nei'} />
                <Info label="Reservert mot digital kommunikasjon" tekst={res.reservert ? 'Ja' : 'Nei'} />
              </VStack>
            ) : (
              'Ikke registrert i KRR'
            )}
          </Box>
        ),
      })}

      {!!mottaker.bestillingId && (
        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
          <Info label="JournalpostID" tekst={mottaker.journalpostId} wide />
          <Info label="DistribusjonID" tekst={mottaker.bestillingId} wide />
        </Box>
      )}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
        <Heading size="xsmall">Distribusjonsmetode</Heading>
        <BodyShort>{mottaker.tvingSentralPrint ? 'Tving sentral print' : 'Automatisk'}</BodyShort>
      </Box>

      {mapFailure(settHovedmottakerResult, (error) => (
        <ApiErrorAlert>{error.detail}</ApiErrorAlert>
      ))}

      {mottaker.type === MottakerType.KOPI && (
        <HStack justify="center" marginBlock="4 0">
          <Button
            variant="secondary"
            icon={<PersonCheckmarkIcon />}
            size="small"
            onClick={() =>
              apiSettHovedmottaker({ brevId, sakId, mottakerId: mottaker.id }, () => window.location.reload())
            }
            loading={isPending(settHovedmottakerResult)}
          >
            Sett som hovedmottaker
          </Button>
        </HStack>
      )}
    </Box>
  )
}

const MottakerInnholdKompakt = ({ mottaker }: { mottaker: Mottaker }) => {
  const { navn, adresse, foedselsnummer, orgnummer } = mottaker

  return (
    <VStack gap="2">
      <BodyShort as="div">
        {/[a-zA-Z\s]/.test(navn) ? (
          navn
        ) : (
          <Alert variant="error" size="small" inline>
            Navn mangler
          </Alert>
        )}
        <br />
        {foedselsnummer && foedselsnummer.value}
        {orgnummer && orgnummer}
      </BodyShort>

      <BodyShort as="div">
        {!adresse?.adresselinje1 && !adresse?.adresselinje2 && !adresse?.adresselinje3 ? (
          <Alert variant="warning" size="small" inline>
            Adresselinjer mangler
          </Alert>
        ) : (
          [adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3]
            .filter((linje) => !!linje)
            .map((linje, i) => <div key={`adresselinje-${i}`}>{linje}</div>)
        )}
        {mottaker.adresse.adresseType !== AdresseType.UTENLANDSKPOSTADRESSE &&
        !adresse?.postnummer &&
        !adresse?.poststed ? (
          <Alert variant="warning" size="small" inline>
            Postnummer og -sted mangler
          </Alert>
        ) : (
          <>
            {adresse?.postnummer} {adresse?.poststed}
          </>
        )}
        <br />
        {adresse?.land || (
          <Alert variant="error" size="small" inline>
            Land mangler
          </Alert>
        )}{' '}
        {!!adresse?.landkode ? (
          `(${adresse.landkode})`
        ) : (
          <Alert variant="error" size="small" inline>
            Landkode mangler
          </Alert>
        )}
      </BodyShort>
    </VStack>
  )
}

const MottakerInnhold = ({ mottaker }: { mottaker: Mottaker }) => {
  const { navn, adresse, foedselsnummer, orgnummer } = mottaker

  return (
    <VStack gap="2">
      <Info
        wide
        label="Navn"
        tekst={
          /[a-zA-Z\s]/.test(navn) ? (
            navn
          ) : (
            <Alert variant="error" size="small" inline>
              Navn mangler
            </Alert>
          )
        }
      />
      {foedselsnummer && <Info label="Fødselsnummer" tekst={foedselsnummer.value} wide />}
      {orgnummer && <Info label="Org.nr." tekst={orgnummer} wide />}

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
  )
}
