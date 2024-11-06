import { Alert, AlertProps, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { ferdigstillJournalfoerOgDistribuerbrev } from '~shared/api/aktivitetsplikt'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import { isFailure, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Column, GridContainer } from '~shared/styled'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { isPending } from '@reduxjs/toolkit'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { handlinger } from '~components/behandling/handlinger/typer'
import { useNavigate } from 'react-router-dom'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'

function ViHarIkkeBrev(props: { variant: AlertProps['variant']; message: string }) {
  return (
    <VStack gap="4">
      <Box paddingInline="16" paddingBlock="16" maxWidth="120rem">
        <Heading level="1" size="large">
          Infobrev aktivitetsplikt
        </Heading>
      </Box>
      <Box paddingInline="16" maxWidth="70rem">
        <Alert variant={props.variant}>{props.message}</Alert>
      </Box>
      <InfobrevKnapperad />
    </VStack>
  )
}

export function VurderingInfoBrev() {
  useSidetittel('Aktivitetsplikt brev')

  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const brevdataFinnes = !!aktivtetspliktbrevdata

  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  if (!brevdataFinnes) {
    if (oppgaveErRedigerbar) {
      return (
        <ViHarIkkeBrev
          variant="error"
          message="Brevdata finnes ikke for denne oppgaven, gå tilbake til vurderingssiden for å endre dette"
        />
      )
    } else {
      return (
        <ViHarIkkeBrev
          variant="info"
          message="Oppgaven ble ferdigstilt før ny vurderingsside for aktivitetspliktoppgaver, så et eventuelt ligger i
          brevoversikten."
        />
      )
    }
  }

  if (aktivtetspliktbrevdata?.skalSendeBrev) {
    if (!aktivtetspliktbrevdata.brevId) {
      return (
        <ViHarIkkeBrev
          variant="error"
          message="Brev for denne oppgaven er ikke opprettet. Gå tilbake til forrige steg og trykk opprett brev."
        />
      )
    } else {
      return (
        <>
          <Aktivitetspliktbrev brevId={aktivtetspliktbrevdata.brevId} sakId={oppgave.sakId} oppgaveid={oppgave.id} />
        </>
      )
    }
  }

  return <ViHarIkkeBrev variant="warning" message="Brev skal ikke sendes for denne oppgaven" />
}

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
    apiHentBrev({ brevId: brevId, sakId: sakId }, (brev) => {
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

  return mapResult(brevStatus, {
    pending: <Spinner label="Henter brev ..." />,
    error: (error) => <ApiErrorAlert>En feil oppsto ved henting av brev: {error.detail}</ApiErrorAlert>,
    success: (brev) => (
      <GridContainer>
        <Column>
          <VStack gap="4" margin="4">
            <Box marginInline="0 8">
              <Heading size="large">Infobrev aktivitetsplikt</Heading>
            </Box>
            <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={kanRedigeres} />
            <BrevSpraak brev={brev} kanRedigeres={kanRedigeres} />

            <BrevMottakerWrapper brev={brev} kanRedigeres={kanRedigeres} />
          </VStack>
        </Column>
        <Column>
          {brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT ? (
            <Box maxHeight="955px" width="100%" height="100%">
              <ForhaandsvisningBrev brev={brev} />
            </Box>
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
          <InfobrevKnapperad
            ferdigstill={
              !(brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT)
                ? { ferdigstillBrev, status }
                : undefined
            }
          >
            <>
              {isFailure(status) && <ApiErrorAlert>Kunne ikke ferdigstille {status.error.detail}</ApiErrorAlert>}
              {isPending(status) && <Spinner label="Ferdigstiller brev og oppgave" />}
            </>
          </InfobrevKnapperad>
        </Column>
      </GridContainer>
    ),
  })
}

export function InfobrevKnapperad(props: {
  ferdigstill?: { ferdigstillBrev: () => void; status: Result<unknown> }
  children?: React.ReactElement
}) {
  const navigate = useNavigate()
  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      {props.children}
      <HStack gap="4" justify="center">
        <Button
          variant="secondary"
          onClick={() => {
            navigate(`../${AktivitetspliktSteg.VURDERING}`)
          }}
        >
          {handlinger.TILBAKE.navn}
        </Button>
        {props.ferdigstill && (
          <Button onClick={props.ferdigstill.ferdigstillBrev} loading={isPending(props.ferdigstill.status)}>
            Ferdigstill brev
          </Button>
        )}
      </HStack>
    </Box>
  )
}
