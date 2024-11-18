import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useParams } from 'react-router-dom'
import { hentBrev } from '~shared/api/brev'
import React, { useEffect, useState } from 'react'
import { Column, GridContainer } from '~shared/styled'
import { StatusBar } from '~shared/statusbar/Statusbar'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import BrevStatusPanel from '~components/person/brev/BrevStatusPanel'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'

import { mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import { BodyShort, Box, Detail, Heading, VStack } from '@navikt/ds-react'
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
          <GridContainer>
            <Column>
              <VStack gap="4" margin="4">
                <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={kanRedigeres} />

                <Box padding="4" borderWidth="1" borderRadius="small">
                  <VStack gap="2" justify="space-between">
                    <Heading level="2" size="medium">
                      Språk / målform
                    </Heading>
                    <BodyShort spacing>{formaterSpraak(brev.spraak)}</BodyShort>
                    <Detail>For å endre språk må du opprette et nytt manuelt brev.</Detail>
                  </VStack>
                </Box>

                <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
              </VStack>
            </Column>
            <Column>
              {brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT ? (
                <PanelWrapper>
                  <ForhaandsvisningBrev brev={brev} />
                </PanelWrapper>
              ) : (
                <RedigerbartBrev
                  brev={brev}
                  kanRedigeres={kanRedigeres}
                  tilbakestillingsaction={() => setTilbakestilt(true)}
                />
              )}
            </Column>
            <Column>
              <BrevStatusPanel brev={brev} />
              <Box padding="4" borderRadius="small">
                <Heading spacing level="2" size="medium">
                  Handlinger
                </Heading>
                <NyttBrevHandlingerPanel brev={brev} setKanRedigeres={setKanRedigeres} />
              </Box>
            </Column>
          </GridContainer>
        )
      )}
    </>
  )
}

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`
