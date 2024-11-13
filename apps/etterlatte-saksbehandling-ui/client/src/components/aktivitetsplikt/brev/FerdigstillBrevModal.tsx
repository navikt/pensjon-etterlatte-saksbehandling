import React, { useState } from 'react'
import { Alert, BodyShort, Button, Heading, Modal, VStack } from '@navikt/ds-react'
import { setAktivitetspliktOppgave } from '~store/reducers/Aktivitetsplikt12mnd'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillBrevOgOppgaveAktivitetsplikt } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { isPending, isSuccess, mapFailure, mapSuccess } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function FerdigstillAktivitetspliktBrevModal() {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [open, setOpen] = useState(false)
  const [ferdigstillBrevOgOppgaveStatus, ferdigstillOppgaveOgBrev] = useApiCall(ferdigstillBrevOgOppgaveAktivitetsplikt)

  const dispatch = useDispatch()
  const ferdigstill = () => {
    ferdigstillOppgaveOgBrev({ oppgaveId: oppgave.id }, (oppgave) => {
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
              Når du ferdigstiller oppgaven vil den bli låst for endringer, og infobrevet vil bli journalført og
              distribuert.
            </BodyShort>

            {mapSuccess(ferdigstillBrevOgOppgaveStatus, () => (
              <Alert variant="success">Oppgaven er ferdigstilt og blir distribuert til mottaker.</Alert>
            ))}

            {mapFailure(ferdigstillBrevOgOppgaveStatus, (error) => (
              <ApiErrorAlert>Kunne ikke ferdigstille brev og oppgave: {error.detail}</ApiErrorAlert>
            ))}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="primary" onClick={ferdigstill} loading={isPending(ferdigstillBrevOgOppgaveStatus)}>
            Ferdigstill oppgave og brev
          </Button>
          <Button
            variant="secondary"
            onClick={() => setOpen(false)}
            disabled={isPending(ferdigstillBrevOgOppgaveStatus)}
          >
            {isSuccess(ferdigstillBrevOgOppgaveStatus) ? 'Lukk' : 'Avbryt'}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
