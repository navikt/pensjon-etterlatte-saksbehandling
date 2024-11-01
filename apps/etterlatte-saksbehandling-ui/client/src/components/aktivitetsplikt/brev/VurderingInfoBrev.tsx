import { Alert, Box, Heading, VStack } from '@navikt/ds-react'
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
import BrevStatusPanel from '~components/person/brev/BrevStatusPanel'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import styled from 'styled-components'
import { isPending } from '@reduxjs/toolkit'
import { opprettAktivitetspliktsbrev } from '~shared/api/aktivitetsplikt'

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
  const skalBrevVises =
    (!!aktivtetspliktbrevdata && !!aktivtetspliktbrevdata.brevId) || !!aktivtetspliktbrevdata?.skalSendeBrev //Implisitt at utbetaling og redusertEtterInntekt er satt

  useEffect(() => {
    if (brevdataFinnes) {
      if (aktivtetspliktbrevdata?.skalSendeBrev) {
        if (!aktivtetspliktbrevdata.brevId) {
          //TODO: sløye denne ifen eller ikke?
          opprettBrevApiCall({ oppgaveId: oppgave.id }, (brevIdDto) => {
            setBrevid(brevIdDto.brevId)
            setBrevErKlart(true)
          })
        } else {
          setBrevid(aktivtetspliktbrevdata.brevId)
          setBrevErKlart(true)
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
              {skalBrevVises && brevErKlart ? (
                <Aktivitetspliktbrev brevId={brevId!} sakId={oppgave.sakId} />
              ) : (
                <>Hvorfor skal brev ikke vises? blablabla</>
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

function Aktivitetspliktbrev({ brevId, sakId }: { brevId: number; sakId: number }) {
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
                <NyttBrevHandlingerPanel
                  brev={brev}
                  setKanRedigeres={setKanRedigeres}
                  callback={() => {
                    // TODO sett oppgave til ferdigstilt her, hele denne skal flyttes til backend som kan håndtere det.
                  }}
                />
              </Box>
            </Column>
          </GridContainer>
        )
      )}
    </>
  )
}
