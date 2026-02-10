import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { Journalpost } from '~shared/types/Journalpost'
import { oppdaterJournalpost } from '~shared/api/dokument'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { useNavigate } from 'react-router-dom'
import { Toast } from '~shared/alerts/Toast'

interface ModalProps {
  journalpost: Journalpost
  oppgaveId: string
}

export default function FerdigstillJournalpostModal({ journalpost, oppgaveId }: ModalProps) {
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)

  const [oppdaterStatus, apiOppdaterJournalpost, resetOppdaterStatus] = useApiCall(oppdaterJournalpost)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const oppdater = () => {
    apiOppdaterJournalpost({ journalpost })
  }

  const oppdaterOgAvsluttOppgave = () => {
    apiOppdaterJournalpost({ journalpost }, () => {
      apiFerdigstillOppgave(oppgaveId, () => {
        setTimeout(() => {
          navigate(`/`)
        }, 5000)
      })
    })
  }

  useEffect(() => {
    if (!open) {
      resetOppdaterStatus()
    }
  }, [open, resetOppdaterStatus])

  if (temaTilhoererGjenny(journalpost)) {
    return (
      <>
        <Button variant="secondary" onClick={oppdater} loading={isPending(oppdaterStatus)}>
          Lagre utkast
        </Button>

        {isSuccess(oppdaterStatus) && <Toast melding="Journalpost oppdatert!" />}
        {mapFailure(oppdaterStatus, (error) => (
          <Alert variant="error">{error.detail || 'Det oppsto en feil ved oppdatering av journalposten'}</Alert>
        ))}
      </>
    )
  } else
    return (
      <>
        <Button variant="secondary" onClick={() => setOpen(true)}>
          Lagre utkast
        </Button>

        <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
          <Modal.Body>
            <Heading size="medium" id="modal-heading" spacing>
              Endre tema og ferdigstille oppgave?
            </Heading>

            <br />

            <Alert variant="info">
              Du endrer nå til tema {journalpost.tema}, som ikke kan behandles i Gjenny. Oppgaven vil bli ferdigstilt i
              Gjenny og overføres til enheten som eier tema {journalpost.tema}.
            </Alert>
            <br />

            {isSuccess(oppdaterStatus) && isSuccess(ferdigstillOppgaveStatus) ? (
              <Alert variant="success">
                Tema er endret til {journalpost.tema} og oppgaven ferdigstilt! Du blir straks sendt tilbake til
                oppgavebenken.
              </Alert>
            ) : (
              <HStack gap="space-4" justify="center">
                <Button
                  variant="secondary"
                  onClick={() => setOpen(false)}
                  disabled={isPending(oppdaterStatus) || isPending(ferdigstillOppgaveStatus)}
                >
                  Nei, avbryt
                </Button>
                <Button
                  variant="primary"
                  onClick={oppdaterOgAvsluttOppgave}
                  loading={isPending(oppdaterStatus) || isPending(ferdigstillOppgaveStatus)}
                >
                  Ja, fortsett
                </Button>
              </HStack>
            )}
          </Modal.Body>

          <Modal.Footer>
            {mapFailure(oppdaterStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppdatering av journalposten'}</ApiErrorAlert>
            ))}
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved ferdigstilling av oppgaven'}</ApiErrorAlert>
            ))}
          </Modal.Footer>
        </Modal>
      </>
    )
}
