import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { ferdigstillJournalfoerOgDistribuerbrev } from '~shared/api/aktivitetsplikt'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import { isFailure, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Column, GridContainer } from '~shared/styled'
import { Box, Button, HStack, VStack } from '@navikt/ds-react'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { isPending } from '@reduxjs/toolkit'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { handlinger } from '~components/behandling/handlinger/typer'
import styled from 'styled-components'

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`

export function Aktivitetspliktbrev({
  brevId,
  sakId,
  oppgaveid,
}: {
  brevId: number
  sakId: number
  oppgaveid: string
}) {
  const [kanRedigeres, setKanRedigeres] = useState(false)
  const [tilbakestilt, setTilbakestilt] = useState(false)
  const navigate = useNavigate()

  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)
  const [status, ferdigstillbrevApi] = useApiCall(ferdigstillJournalfoerOgDistribuerbrev)

  const ferdigstillBrev = () => {
    ferdigstillbrevApi(
      { oppgaveId: oppgaveid },
      () => hentBrevOgSetStatus(),
      () => hentBrevOgSetStatus()
    )
  }

  const hentBrevOgSetStatus = () => {
    apiHentBrev({ brevId: Number(brevId), sakId: Number(sakId) }, (brev) => {
      if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
        setKanRedigeres(true)
      } else {
        setKanRedigeres(false)
      }
    })
  }

  useEffect(() => {
    hentBrevOgSetStatus()
  }, [brevId, sakId, tilbakestilt])

  return (
    <>
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

                <BrevSpraak brev={brev} kanRedigeres={kanRedigeres} />

                <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
              </VStack>
            </Column>
            <Column>
              {brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT ? (
                <PanelWrapper>
                  <ForhaandsvisningBrev brev={brev} />
                </PanelWrapper>
              ) : (
                <>
                  <HStack wrap={false}>
                    <RedigerbartBrev
                      brev={brev}
                      kanRedigeres={kanRedigeres}
                      tilbakestillingsaction={() => setTilbakestilt(true)}
                    />
                  </HStack>
                </>
              )}
              <VStack gap="4">
                <HStack gap="4" justify="center">
                  <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
                    {isFailure(status) && <ApiErrorAlert>Kunne ikke ferdigstille {status.error.detail}</ApiErrorAlert>}
                    {isPending(status) && <Spinner label="Ferdigstiller brev og oppgave" />}
                    <HStack gap="4" justify="center">
                      <Button
                        variant="secondary"
                        onClick={() => {
                          navigate(`../${AktivitetspliktSteg.VURDERING}`)
                        }}
                      >
                        {handlinger.TILBAKE.navn}
                      </Button>
                      {!(
                        brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT
                      ) && <Button onClick={ferdigstillBrev}>Ferdigstill brev</Button>}
                    </HStack>
                  </Box>
                </HStack>
              </VStack>
            </Column>
          </GridContainer>
        )
      )}
    </>
  )
}
