import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { ferdigstillOppgave, OppgaveDTO } from '~shared/api/oppgaver'
import { Journalpost } from '~shared/types/Journalpost'
import { ferdigstillJournalpost } from '~shared/api/dokument'
import { FlexRow } from '~shared/styled'
import { ISak } from '~shared/types/sak'

interface ModalProps {
  oppgave: OppgaveDTO
  journalpost: Journalpost
  sak: ISak
}

export default function FerdigstillJournalpostModal({ oppgave, journalpost, sak }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [ferdigstillJournalpostStatus, apiFerdigstillJournalpost] = useApiCall(ferdigstillJournalpost)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const ferdigstill = () => {
    apiFerdigstillJournalpost({ journalpostId: journalpost.journalpostId, sak }, () => {
      apiFerdigstillOppgave(oppgave.id, () => {
        setTimeout(() => {
          navigate(`/`)
        }, 5000)
      })
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)} disabled={!sak?.id || !sak?.sakType}>
        Ferdigstill
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Ferdigstill journalpost
          </Heading>

          <BodyLong spacing>
            Du kobler nå journalpost til sak med id {sak.id} og ferdigstiller den. Er du sikker på at alt er korrekt?
          </BodyLong>

          {isSuccess(ferdigstillJournalpostStatus) && isSuccess(ferdigstillOppgaveStatus) ? (
            <Alert variant="success">
              Journalpost ferdigstilt og oppgave lukket. Du blir straks sendt tilbake til oppgavelisten.
            </Alert>
          ) : (
            <FlexRow justify="center">
              <Button
                variant="secondary"
                onClick={() => setOpen(false)}
                disabled={isPending(ferdigstillJournalpostStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Nei, avbryt
              </Button>
              <Button
                variant="primary"
                onClick={ferdigstill}
                loading={isPending(ferdigstillJournalpostStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Ja, ferdigstill
              </Button>
            </FlexRow>
          )}

          {isFailure(ferdigstillOppgaveStatus) && (
            <Modal.Footer>
              <Alert variant="error">Det oppsto en feil ved ferdigstilling av oppgave.</Alert>
            </Modal.Footer>
          )}
          {isFailure(ferdigstillJournalpostStatus) && (
            <Modal.Footer>
              <Alert variant="error">Det oppsto en feil ved endring av tema.</Alert>
            </Modal.Footer>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
