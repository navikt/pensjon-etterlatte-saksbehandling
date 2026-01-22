import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { Alert, BodyLong, Button, Modal } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function AvsluttTilbakekrevingOppgaveUtenKravgrunnlag({
  oppgave,
  oppdaterStatus,
  oppdaterMerknad,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
}) {
  const [modalOpen, setModalOpen] = useState(false)
  const [ferdigstillOppgaveResult, ferdigstillOppgaveFetch, resetFerdigstillOppgave] =
    useApiCall(ferdigstillOppgaveMedMerknad)

  function lukkModal() {
    setModalOpen(false)
    resetFerdigstillOppgave()
  }

  function avslutt() {
    ferdigstillOppgaveFetch({ id: oppgave.id }, (oppdatertOppgave) => {
      oppdaterStatus(oppdatertOppgave.id, oppdatertOppgave.status)
      if (oppdatertOppgave.merknad) {
        oppdaterMerknad(oppdatertOppgave.id, oppdatertOppgave.merknad)
      }
    })
  }

  return (
    <>
      <Button size="small" onClick={() => setModalOpen(true)}>
        Ferdigstill oppgave
      </Button>
      <Modal open={modalOpen} onClose={lukkModal} header={{ heading: 'Avslutt oppgave' }}>
        <Modal.Body>
          <BodyLong>
            Det er opprettet en oppgave for tilbakekreving som venter på et kravgrunnlag for feilutbetaling. Hvis det
            ikke er behov for oppgaven (den ble opprettet ved en feil) så kan denne oppgaven avsluttes.
          </BodyLong>
          <BodyLong>
            Hvis det senere kommer en feilutbetaling i saken så vil det bli opprettet en ny oppgave for tilbakekreving.
          </BodyLong>
          {mapResult(ferdigstillOppgaveResult, {
            error: (error) => <ApiErrorAlert>Kunne ikke avslutte oppgaven: {error.detail}</ApiErrorAlert>,
            success: () => <Alert variant="success">Oppgaven er ferdigstilt. Du kan nå lukke visningen.</Alert>,
          })}
        </Modal.Body>
        <Modal.Footer>
          <Button
            disabled={isSuccess(ferdigstillOppgaveResult)}
            loading={isPending(ferdigstillOppgaveResult)}
            onClick={avslutt}
          >
            Avslutt oppgave
          </Button>
          <Button variant="secondary" onClick={lukkModal} disabled={isPending(ferdigstillOppgaveResult)}>
            Lukk
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
