/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
  - borderColor=border-neutral-subtle
*/

import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useParams } from 'react-router-dom'
import { hentBrev } from '~shared/api/brev'
import React, { useEffect, useState } from 'react'
import { StatusBar } from '~shared/statusbar/Statusbar'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BrevProsessType, BrevStatus, Spraak } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import BrevStatusPanel from '~components/person/brev/BrevStatusPanel'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import { mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import { BodyShort, Box, Detail, Heading, HStack, VStack } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import { formaterSpraak } from '~utils/formatering/formatering'

export default function NyttBrev() {
  useSidetittel('Nytt brev')

  const { brevId, sakId } = useParams()
  const [kanRedigeres, setKanRedigeres] = useState(false)
  const [tilbakestilt, setTilbakestilt] = useState(false)

  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)

  useEffect(() => {
    apiHentBrev({ brevId: Number(brevId), sakId: Number(sakId) }, (brev) => {
      if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
        setKanRedigeres(true)
      } else {
        setKanRedigeres(false)
      }
    })
  }, [brevId, sakId, tilbakestilt])

  return (
    <>
      {mapSuccess(brevStatus, (brev) => (
        <>
          <StatusBar ident={brev.soekerFnr} />
          <NavigerTilbakeMeny to="/person?fane=BREV" state={{ fnr: brev.soekerFnr }}>
            Tilbake til brevoversikt
          </NavigerTilbakeMeny>
        </>
      ))}

      {mapApiResult(
        brevStatus,
        <Spinner label="Henter brev ..." />,
        () => (
          <ApiErrorAlert>Feil oppsto ved henting av brev</ApiErrorAlert>
        ),
        (brev) => (
          <HStack height="100%" minHeight="100vh" wrap={false}>
            <VStack gap="space-4" margin="space-4" minWidth="30rem">
              <BrevTittel
                brevId={brev.id}
                sakId={brev.sakId}
                tittel={brev.tittel}
                kanRedigeres={kanRedigeres}
                manueltBrev={brev.spraak !== Spraak.NB}
              />

              <Box padding="space-4" borderWidth="1" borderColor="border-neutral-subtle">
                <VStack gap="space-2" justify="space-between">
                  <Heading level="2" size="medium">
                    Språk / målform
                  </Heading>
                  <BodyShort spacing>{formaterSpraak(brev.spraak)}</BodyShort>
                  <Detail>For å endre språk må du opprette et nytt manuelt brev.</Detail>
                </VStack>
              </Box>

              <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
            </VStack>
            <Box minWidth="50rem" width="100%" borderWidth="0 1" borderColor="border-neutral-subtle">
              {brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT ? (
                <ForhaandsvisningBrev brev={brev} />
              ) : (
                <RedigerbartBrev
                  brev={brev}
                  kanRedigeres={kanRedigeres}
                  tilbakestillingsaction={() => setTilbakestilt(true)}
                />
              )}
            </Box>
            <Box minWidth="30rem">
              <BrevStatusPanel brev={brev} />
              <Box padding="space-4">
                <Heading spacing level="2" size="medium">
                  Handlinger
                </Heading>
                <NyttBrevHandlingerPanel brev={brev} setKanRedigeres={setKanRedigeres} />
              </Box>
            </Box>
          </HStack>
        )
      )}
    </>
  )
}
