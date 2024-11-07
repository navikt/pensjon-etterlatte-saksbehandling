import { Oppgavestatus } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { Alert, BodyShort, VStack } from '@navikt/ds-react'
import { mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'
import { InfobrevKnapperad } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export function UtenBrevVisning() {
  const { oppgave, oppdater, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstillOppgaveWrapper = () => {
    apiFerdigstillOppgave(oppgave.id, () => {
      oppdater()
    })
  }
  const oppgaveErFerdigstilt = oppgave.status === Oppgavestatus.FERDIGSTILT
  const oppgaveKanFerdigstilles = !oppgaveErFerdigstilt && !!aktivtetspliktbrevdata && !aktivtetspliktbrevdata.brevId

  return (
    <VStack gap="4" justify="center">
      {oppgaveErFerdigstilt ? (
        <Alert variant="success">Oppgaven er ferdigstilt</Alert>
      ) : oppgaveKanFerdigstilles ? (
        <>
          <BodyShort>Brev skal ikke sendes for denne oppgaven, du kan nå ferdigstille oppgaven.</BodyShort>
        </>
      ) : (
        <>
          <Alert variant="error">
            Brev er ikke opprettet for oppgaven. Du må gå tilbake til forrige steg for å opprette brevet
          </Alert>
        </>
      )}
      <InfobrevKnapperad
        ferdigstill={!oppgaveKanFerdigstilles ? ferdigstillOppgaveWrapper : undefined}
        status={ferdigstillOppgaveStatus}
        tekst="Ferdigstill oppgave"
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
