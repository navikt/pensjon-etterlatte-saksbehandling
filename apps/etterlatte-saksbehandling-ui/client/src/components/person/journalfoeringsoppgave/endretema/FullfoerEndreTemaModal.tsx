import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyLong, Button, Heading, Modal, Tag } from '@navikt/ds-react'
import { ferdigstillOppgave, OppgaveDTO } from '~shared/api/oppgaver'
import { Journalpost, OppdaterJournalpostTemaRequest } from '~shared/types/Journalpost'
import { endreTemaJournalpost } from '~shared/api/dokument'
import { FlexRow } from '~shared/styled'

interface ModalProps {
  oppgave: OppgaveDTO
  journalpost: Journalpost
  endreTemaRequest?: OppdaterJournalpostTemaRequest
}

export default function FullfoerEndreTemaModal({ oppgave, endreTemaRequest, journalpost }: ModalProps) {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const [endreTemaStatus, apiEndreTemaJournalpost] = useApiCall(endreTemaJournalpost)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const endreTema = () => {
    if (!endreTemaRequest?.nyttTema) {
      return
    }

    apiEndreTemaJournalpost({ journalpostId: journalpost.journalpostId, nyttTema: endreTemaRequest.nyttTema }, () => {
      apiFerdigstillOppgave(oppgave.id, () => {
        setTimeout(() => {
          navigate(`/`)
        }, 5000)
      })
    })
  }

  return (
    <>
      <Button variant="primary" onClick={() => setOpen(true)} disabled={endreTemaRequest?.nyttTema?.length !== 3}>
        Endre tema
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Endring av tema
          </Heading>

          <BodyLong spacing>
            Du endrer nå tema på journalposten fra <Tag variant="warning">{journalpost.tema}</Tag> til{' '}
            <Tag variant="success">{endreTemaRequest?.nyttTema}</Tag>.
          </BodyLong>

          {isSuccess(endreTemaStatus) && isSuccess(ferdigstillOppgaveStatus) ? (
            <Alert variant="success">
              Tema endret til <strong>{endreTemaRequest?.nyttTema}</strong> og oppgave lukket. Du blir straks sendt
              tilbake til oppgavelisten.
            </Alert>
          ) : (
            <FlexRow justify="center">
              <Button
                variant="secondary"
                onClick={() => setOpen(false)}
                disabled={isPending(endreTemaStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                onClick={endreTema}
                loading={isPending(endreTemaStatus) || isPending(ferdigstillOppgaveStatus)}
              >
                Lagre nytt tema
              </Button>
            </FlexRow>
          )}

          {isFailure(ferdigstillOppgaveStatus) && (
            <Modal.Footer>
              <Alert variant="error">Det oppsto en feil ved ferdigstilling av oppgave.</Alert>
            </Modal.Footer>
          )}
          {isFailure(endreTemaStatus) && (
            <Modal.Footer>
              <Alert variant="error">Det oppsto en feil ved endring av tema.</Alert>
            </Modal.Footer>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
