import { Alert, Box, Button, Heading, VStack } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import { isFailure, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Column, GridContainer } from '~shared/styled'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import styled from 'styled-components'
import { isPending } from '@reduxjs/toolkit'
import { ferdigstillJournalfoerOgDistribuerbrev, opprettAktivitetspliktsbrev } from '~shared/api/aktivitetsplikt'

export function VurderingInfoBrev() {
  useSidetittel('Aktivitetsplikt brev')

  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettAktivitetspliktsbrev)

  //const redigerbar = erOppgaveRedigerbar(oppgave.status)
  const brevdataFinnes = !!aktivtetspliktbrevdata

  const [brevId, setBrevid] = useState<number | undefined>(aktivtetspliktbrevdata?.brevId)
  const [brevErKlart, setBrevErKlart] = useState<boolean>(false)
  /*TODO: kalle backend her og sjekke om
    1. brevid finnes
    finnes ikke -> sjekk om oppgave kan redigeres og om det var valgt at brev skulle sendes JA/NEI
    finnes -> vise brev .. fikset brevkomponenten det automatisk?
     Bad state altså at brevid ikke finnes og sb ikke har tatt stilling til brevvalg må reroutes tilkbake til vurderingssiden
    hvis bad state her ha med tilbakeknapp

    TODO: håndtere sette oppgave til ferdigstilt på onclick lagre brev
     */

  useEffect(() => {
    if (brevdataFinnes) {
      if (aktivtetspliktbrevdata?.skalSendeBrev) {
        if (aktivtetspliktbrevdata.brevId) {
          setBrevid(aktivtetspliktbrevdata.brevId)
          setBrevErKlart(true)
        } else {
          opprettBrevApiCall({ oppgaveId: oppgave.id }, (brevIdDto) => {
            setBrevid(brevIdDto.brevId)
            setBrevErKlart(true)
          })
        }
      } else {
        //Skal ikke sende brev for denne oppgave, brevløs oppgave, bare å vise skal ikke ha brev blabla
        setBrevErKlart(false)
      }
    } else {
      //Håndtere manglende brevdata.... vise generell mangler utfylling av brevdata feil
    }
  }, [])

  return (
    <Box paddingInline="16" paddingBlock="16">
      <Heading size="large">Vurdering infobrev her</Heading>
      {brevdataFinnes ? (
        <>
          {aktivtetspliktbrevdata?.skalSendeBrev ? (
            <>
              {isPending(opprettBrevStatus) && <Spinner label="Oppretter brev" />}
              {isFailure(opprettBrevStatus) && (
                <ApiErrorAlert>Kunne ikke opprette brev {opprettBrevStatus.error.detail}</ApiErrorAlert>
              )}
              {brevErKlart && brevId && (
                <Aktivitetspliktbrev brevId={brevId} sakId={oppgave.sakId} oppgaveid={oppgave.id} />
              )}
            </>
          ) : (
            <>
              <div>Brev skal ikke sendes for denne oppgaven</div>
            </>
          )}
        </>
      ) : (
        <>
          <Alert variant="error">
            Brevdata finnes ikke for denne oppgaven, gå tilbake til vurderingssiden for å endre dette
          </Alert>
        </>
      )}
    </Box>
  )
}

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`

function Aktivitetspliktbrev({
  brevId,
  sakId,
  oppgaveid,
}: {
  brevId: number
  sakId: number
  oppgaveid: string
}): JSX.Element {
  const [kanRedigeres, setKanRedigeres] = useState(false)
  const [tilbakestilt, setTilbakestilt] = useState(false)

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
                  <RedigerbartBrev
                    brev={brev}
                    kanRedigeres={kanRedigeres}
                    tilbakestillingsaction={() => setTilbakestilt(true)}
                  />
                  {isFailure(status) && <ApiErrorAlert>Kunne ikke ferdigstille {status.error.detail}</ApiErrorAlert>}
                  {isPending(status) && <Spinner label="Ferdigstiller brev" />}
                  <Button onClick={ferdigstillBrev}>Ferdigstill brev</Button>
                </>
              )}
            </Column>
          </GridContainer>
        )
      )}
    </>
  )
}
