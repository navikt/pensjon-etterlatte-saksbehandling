import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyLong, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO } from '~shared/types/oppgave'

export default function FerdigstillOppgaveModal({ oppgave }: { oppgave: OppgaveDTO }) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)}>
        Ferdigstill
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Ferdigstill oppgaven
          </Heading>

          <BodyLong spacing>
            Oppgaven vil bli markert som ferdigstilt. Denne handlingen kan ikke angres. Er du sikker på at du vil
            fortsette?
          </BodyLong>

          {isSuccess(ferdigstillOppgaveStatus) ? (
            <>
              <Alert variant="success">Oppgaven er nå ferdigstilt!</Alert>

              <br />

              <HStack gap="space-4" justify="center">
                <Button variant="secondary" onClick={() => navigate('/')}>
                  Gå til oppgavelisten
                </Button>
                <Button variant="primary" onClick={() => navigate('/person', { state: { fnr: oppgave.fnr } })}>
                  Gå til sakoversikten
                </Button>
              </HStack>
            </>
          ) : (
            <HStack gap="space-4" justify="center">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillOppgaveStatus)}>
                Nei, avbryt
              </Button>
              <Button
                variant="primary"
                onClick={() => apiFerdigstillOppgave(oppgave.id)}
                loading={isPending(ferdigstillOppgaveStatus)}
              >
                Ja, ferdigstill
              </Button>
            </HStack>
          )}

          {mapFailure(ferdigstillOppgaveStatus, (error) => (
            <Modal.Footer>
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgave'}</ApiErrorAlert>
            </Modal.Footer>
          ))}
        </Modal.Body>
      </Modal>
    </>
  )
}
