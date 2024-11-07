import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { ferdigstillBrevOgOppgaveAktivitetsplikt } from '~shared/api/aktivitetsplikt'
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
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export function Aktivitetspliktbrev({ brevId }: { brevId: number }) {
  const { oppgave, oppdater } = useAktivitetspliktOppgaveVurdering()
  const [kanRedigeres, setKanRedigeres] = useState(false)

  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)
  const [status, ferdigstillOppgaveOgBrev] = useApiCall(ferdigstillBrevOgOppgaveAktivitetsplikt)

  const ferdigstill = () => {
    ferdigstillOppgaveOgBrev({ oppgaveId: oppgave.id }, () => oppdater()) //TODO: replace oppdater her
  }

  const hentBrevOgSetStatus = () => {
    apiHentBrev({ brevId: brevId, sakId: oppgave.sakId }, (brev) => {
      if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
        setKanRedigeres(true)
      } else {
        setKanRedigeres(false)
      }
    })
  }

  useEffect(() => {
    hentBrevOgSetStatus()
  }, [brevId, oppgave.status])

  return mapResult(brevStatus, {
    pending: <Spinner label="Henter brev ..." />,
    error: (error) => <ApiErrorAlert>En feil oppsto ved henting av brev: {error.detail}</ApiErrorAlert>,
    success: (brev) => {
      const brevErFerdigstilt =
        brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT
      return (
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
            {brevErFerdigstilt ? (
              <Box maxHeight="955px" width="100%" height="100%" marginBlock="0 16">
                <ForhaandsvisningBrev brev={brev} />
              </Box>
            ) : (
              <HStack wrap={false}>
                <RedigerbartBrev brev={brev} kanRedigeres={kanRedigeres} tilbakestillingsaction={() => undefined} />
              </HStack>
            )}
            <InfobrevKnapperad ferdigstill={!brevErFerdigstilt ? ferdigstill : undefined} status={status}>
              <>
                {isFailure(status) && <ApiErrorAlert>Kunne ikke ferdigstille {status.error.detail}</ApiErrorAlert>}
                {isPending(status) && <Spinner label="Ferdigstiller brev og oppgave" />}
              </>
            </InfobrevKnapperad>
          </Column>
        </GridContainer>
      )
    },
  })
}

export function InfobrevKnapperad(props: {
  ferdigstill?: () => void
  status?: Result<unknown>
  tekst?: string
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
          <Button onClick={props.ferdigstill} loading={isPending(props.status)}>
            {props.tekst ? props.tekst : 'Ferdigstill brev'}
          </Button>
        )}
      </HStack>
    </Box>
  )
}
