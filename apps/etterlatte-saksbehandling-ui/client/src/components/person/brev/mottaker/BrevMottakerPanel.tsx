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
import { hentDistKanal, oppdaterMottaker, settHovedmottaker } from '~shared/api/brev'
import { SlettMottakerModal } from '~components/person/brev/mottaker/SlettMottakerModal'
import { PersonCheckmarkIcon } from '@navikt/aksel-icons'
import { Distribusjonskanal, DokDistKanalResponse } from '~shared/types/dokdist'

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

const formaterDistribusjonskanal = (kanal: Distribusjonskanal) => {
  switch (kanal) {
    case Distribusjonskanal.LOKAL_PRINT:
      return 'Lokal print'
    case Distribusjonskanal.PRINT:
      return 'Print'
    case Distribusjonskanal.DITT_NAV:
      return 'Ditt Nav'
    case Distribusjonskanal.SDP:
      return 'SDP'
    case Distribusjonskanal.INGEN_DISTRIBUSJON:
      return 'Ingen distribusjon'
    case Distribusjonskanal.TRYGDERETTEN:
      return 'Trygderetten'
    case Distribusjonskanal.DPVT:
      return 'DPVT'
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
  const [distKanalResult, apiHentDistKanal] = useApiCall(hentDistKanal)

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
    if (mottaker.foedselsnummer?.value && !mottaker.tvingSentralPrint) {
      apiHentDistKanal({ brevId, sakId, mottakerId: mottaker.id })
    }
  }, [mottaker.foedselsnummer?.value, mottaker.tvingSentralPrint])

  const oppdater = (brevId: number, sakId: number, mottaker: Mottaker, onSuccess: () => void) => {
    apiOppdaterMottaker({ brevId, sakId, mottaker }, () => {
      setMottaker(mottaker)
      onSuccess()
    })
  }

  return (
    <Box padding="4" borderWidth="1" borderRadius="small">
      {mapResult(soeker, {
        initial: kanRedigeres && mottaker.type === MottakerType.HOVED && (
          <Box marginBlock="0 2">
            <Alert variant="info" size="small">
              Sjekk om bruker har verge
            </Alert>
          </Box>
        ),
        pending: <Spinner label="Henter eventuelle verger" margin="0" />,
        error: () =>
          kanRedigeres && (
            <Box marginBlock="0 2">
              <Alert variant="info" size="small">
                Sjekk om brevet skal sendes til verge. Registrer eventuelt riktig adresse.
              </Alert>
            </Box>
          ),
        success: (soekeren) =>
          ((kanRedigeres && soekeren?.opplysning?.vergemaalEllerFremtidsfullmakt) || []).length > 0 && (
            <Box marginBlock="0 2">
              <Alert variant="info" size="small">
                Brevet skal sendes til verge. Registrer riktig adresse.
              </Alert>
            </Box>
          ),
      })}

      <HStack gap="2" justify="space-between">
        <Heading spacing level="2" size="medium">
          <HStack gap="2">
            {flereMottakere ? formaterMottakerType(mottaker.type) : 'Mottaker'}

            {mottaker.adresse.adresseType === AdresseType.UTENLANDSKPOSTADRESSE && (
              <Tag variant="alt1" size="small">
                Utenlandsk adresse
              </Tag>
            )}
          </HStack>
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

      {!!mottaker.bestillingId && (
        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
          <Info label="JournalpostID" tekst={mottaker.journalpostId} wide />
          <Info label="DistribusjonID" tekst={mottaker.bestillingId} wide />
        </Box>
      )}

      <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="4 0" marginBlock="4 0">
        <Heading size="xsmall" spacing>
          Distribusjonsmetode
        </Heading>

        {mottaker.tvingSentralPrint
          ? 'Tvinger sentral print'
          : mapResult(distKanalResult, {
              pending: <Loader />,
              success: (res: DokDistKanalResponse) => (
                <VStack gap="4">
                  <Info label="Kanal" tekst={formaterDistribusjonskanal(res.distribusjonskanal)} />
                  <Info label="Begrunnelse" tekst={res.regelBegrunnelse} />
                </VStack>
              ),
            })}
      </Box>

      {mapFailure(settHovedmottakerResult, (error) => (
        <ApiErrorAlert>{error.detail}</ApiErrorAlert>
      ))}

      {mottaker.type === MottakerType.KOPI && (
        <HStack justify="center" marginBlock="4 0">
          <Button
            variant="secondary"
            icon={<PersonCheckmarkIcon aria-hidden />}
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
        {!!orgnummer && orgnummer}
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
