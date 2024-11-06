import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { Alert, BodyShort, Button, VStack } from '@navikt/ds-react'
import { isPending } from '@reduxjs/toolkit'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'
import { InfobrevKnapperad } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'

export function UtenBrevVisning({ oppgave, fetchOppgave }: { oppgave: OppgaveDTO; fetchOppgave: () => void }) {
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstillOppgaveWrapper = () => {
    apiFerdigstillOppgave(oppgave.id, () => {
      fetchOppgave()
    })
  }
  const oppgaveErFerdigstilt = oppgave.status === Oppgavestatus.FERDIGSTILT
  return (
    <VStack gap="4" justify="center">
      {oppgaveErFerdigstilt ? (
        <Alert variant="success">Oppgaven er ferdigstilt</Alert>
      ) : (
        <>
          <BodyShort>Brev skal ikke sendes for denne oppgaven, du kan nå ferdigstille oppgaven.</BodyShort>
          <Button onClick={ferdigstillOppgaveWrapper} loading={isPending(ferdigstillOppgaveStatus)}>
            Ferdigstill oppgave
          </Button>
        </>
      )}
      <InfobrevKnapperad
        ferdigstill={
          oppgaveErFerdigstilt
            ? undefined
            : {
                ferdigstillBrev: ferdigstillOppgaveWrapper,
                status: ferdigstillOppgaveStatus,
              }
        }
      >
        <>
          {mapFailure(ferdigstillOppgaveStatus, (error) => (
            <ApiErrorAlert>Kunne ikke ferdigstille oppgave.{error.detail}</ApiErrorAlert>
          ))}
        </>
      </InfobrevKnapperad>
    </VStack>
  )
}
