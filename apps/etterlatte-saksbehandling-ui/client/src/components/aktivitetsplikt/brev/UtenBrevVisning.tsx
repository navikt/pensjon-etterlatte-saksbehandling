import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { Column, GridContainer } from '~shared/styled'
import { Alert, Button, HStack } from '@navikt/ds-react'
import { isPending } from '@reduxjs/toolkit'
import { isFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'

export function UtenBrevVisning({ oppgave, fetchOppgave }: { oppgave: OppgaveDTO; fetchOppgave: () => void }) {
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstillOppgaveWrapper = () => {
    apiFerdigstillOppgave(oppgave.id, () => {
      fetchOppgave()
    })
  }
  const oppgaveErFerdigstilt = oppgave.status === Oppgavestatus.FERDIGSTILT
  return (
    <GridContainer>
      <Column>
        <HStack gap="4" justify="center">
          {oppgaveErFerdigstilt ? (
            <Alert variant="success">Oppgaven er ferdigstilt</Alert>
          ) : (
            <>
              <div>Brev skal ikke sendes for denne oppgaven, du kan nå ferdigstille oppgaven.</div>
              <Button onClick={ferdigstillOppgaveWrapper} loading={isPending(ferdigstillOppgaveStatus)}>
                Ferdigstill oppgave
              </Button>
              {isFailure(ferdigstillOppgaveStatus) && (
                <ApiErrorAlert>Kunne ikke ferdigstille oppgave.{ferdigstillOppgaveStatus.error.detail}</ApiErrorAlert>
              )}
            </>
          )}
        </HStack>
      </Column>
    </GridContainer>
  )
}
