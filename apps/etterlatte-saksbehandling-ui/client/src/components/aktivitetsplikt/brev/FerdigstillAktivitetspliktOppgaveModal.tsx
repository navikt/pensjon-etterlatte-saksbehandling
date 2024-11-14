import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { useDispatch } from 'react-redux'
import { setAktivitetspliktOppgave } from '~store/reducers/Aktivitetsplikt12mnd'
import { Alert, BodyShort, Button, Heading, Modal, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapFailure, mapSuccess } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function FerdigstillAktivitetspliktOppgaveModal() {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [open, setOpen] = useState(false)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const dispatch = useDispatch()
  const ferdigstill = () => {
    apiFerdigstillOppgave(oppgave.id, (oppgave) => {
      dispatch(setAktivitetspliktOppgave(oppgave))
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)}>
        Ferdigstill
      </Button>
      <Modal open={open} onClose={() => setOpen(false)} aria-label="Ferdigstill oppgave">
        <Modal.Header>
          <Heading size="large">Ferdigstill oppgave</Heading>
        </Modal.Header>
        <Modal.Body>
          <VStack gap="6">
            <BodyShort>
              Når du ferdigstiller oppgaven blir den låst for endringer. Siden det er valgt at ingen brev skal sendes
              vil det ikke bli sendt ut noen infobrev.
            </BodyShort>
            {mapSuccess(ferdigstillOppgaveStatus, () => (
              <Alert variant="success">Oppgaven er ferdigstilt.</Alert>
            ))}

            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>Kunne ikke ferdigstille oppgave: {error.detail}</ApiErrorAlert>
            ))}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="primary" onClick={ferdigstill} loading={isPending(ferdigstillOppgaveStatus)}>
            Ferdigstill oppgave
          </Button>
          <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillOppgaveStatus)}>
            {isSuccess(ferdigstillOppgaveStatus) ? 'Lukk' : 'Avbryt'}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
