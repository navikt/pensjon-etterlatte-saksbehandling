import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { Journalpost } from '~shared/types/Journalpost'
import { oppdaterJournalpost } from '~shared/api/dokument'
import { FlexRow } from '~shared/styled'
import { ISak } from '~shared/types/sak'
import { ApiErrorAlert } from '~ErrorBoundary'

import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import {
  kanFerdigstilleJournalpost,
  temaTilhoererGjenny,
} from '~components/person/journalfoeringsoppgave/journalpost/validering'

interface ModalProps {
  journalpost: Journalpost
  sak: ISak
}

export default function JournalfoerJournalpostModal({ journalpost, sak }: ModalProps) {
  const [open, setOpen] = useState(false)

  const [oppdaterStatus, apiOppdaterJournalpost] = useApiCall(oppdaterJournalpost)
  const [kanFerdigstilles, feilmeldinger] = kanFerdigstilleJournalpost(journalpost)

  const ferdigstill = () => {
    apiOppdaterJournalpost({ journalpost, forsoekFerdigstill: true, journalfoerendeEnhet: sak.enhet }, () => {
      setTimeout(() => window.location.reload(), 3000)
    })
  }

  return (
    <>
      <Button
        variant="primary"
        onClick={() => setOpen(true)}
        disabled={!sak?.id || !sak?.sakType || !temaTilhoererGjenny(journalpost)}
      >
        Journalfør
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading" spacing>
            Journalfør
          </Heading>

          {kanFerdigstilles ? (
            <BodyLong spacing>
              Du journalfører nå journalposten. Denne handlingen kan ikke angres. <br />
              Er du sikker på at du vil fortsette?
            </BodyLong>
          ) : (
            <Alert variant="error">
              <strong>Kan ikke journalføre før følgende er fikset: </strong>
              <ul>
                {feilmeldinger.map((e, i) => (
                  <li key={`feilmelding-${i}`}>{e}</li>
                ))}
              </ul>
            </Alert>
          )}

          <br />

          {isSuccess(oppdaterStatus) ? (
            <Alert variant="success">Journalpost journalført ok! Laster siden på nytt...</Alert>
          ) : (
            <FlexRow justify="center">
              <Button variant="secondary" onClick={() => setOpen(false)} disabled={isPending(oppdaterStatus)}>
                Nei, avbryt
              </Button>
              <Button
                variant="primary"
                onClick={ferdigstill}
                loading={isPending(oppdaterStatus)}
                disabled={!kanFerdigstilles}
              >
                Ja, journalfør
              </Button>
            </FlexRow>
          )}

          {mapFailure(oppdaterStatus, (error) => (
            <Modal.Footer>
              <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved journalføring av journalposten'}</ApiErrorAlert>
            </Modal.Footer>
          ))}
        </Modal.Body>
      </Modal>
    </>
  )
}
