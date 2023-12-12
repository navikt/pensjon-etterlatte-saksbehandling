import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { ferdigstillOppgave, OppgaveDTO } from '~shared/api/oppgaver'
import { FlexRow } from '~shared/styled'

import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'

interface ModalProps {
  oppgave: OppgaveDTO
  kanFerdigstilles: boolean
}

export default function FerdigstillOppgaveModal({ oppgave, kanFerdigstilles }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstill = () => {
    apiFerdigstillOppgave(oppgave.id, () => {
      setTimeout(() => {
        navigate(`/`)
      }, 5000)
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)} disabled={!kanFerdigstilles}>
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
            <Alert variant="success">Oppgaven er nå ferdigstilt. Du blir straks sendt tilbake til oppgavelisten.</Alert>
          ) : (
            <FlexRow justify="center">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(ferdigstillOppgaveStatus)}>
                Nei, avbryt
              </Button>
              <Button variant="primary" onClick={ferdigstill} loading={isPending(ferdigstillOppgaveStatus)}>
                Ja, ferdigstill
              </Button>
            </FlexRow>
          )}

          {isFailure(ferdigstillOppgaveStatus) && (
            <Modal.Footer>
              <Alert variant="error">Det oppsto en feil ved ferdigstilling av oppgave.</Alert>
            </Modal.Footer>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
