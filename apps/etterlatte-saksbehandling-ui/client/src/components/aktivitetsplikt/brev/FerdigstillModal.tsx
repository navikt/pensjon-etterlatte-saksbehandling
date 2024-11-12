import React, { useState } from 'react'
import { Alert, BodyShort, Button, Heading, Modal, VStack } from '@navikt/ds-react'
import { setAktivitetspliktOppgave } from '~store/reducers/Aktivitetsplikt12mnd'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillBrevOgOppgaveAktivitetsplikt } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { isPending, isSuccess, mapFailure, mapSuccess } from '~shared/api/apiUtils'
import { ferdigstillOppgave } from '~shared/api/oppgaver'
import { ApiErrorAlert } from '~ErrorBoundary'

export function FerdigstillAktivitetspliktBrevModal() {
  return <FerdigstillModal type="BREV" key="BREV" />
}

export function FerdigstillAktivitetspliktOppgaveModal() {
  return <FerdigstillModal type="OPPGAVE" key="OPPGAVE" />
}

function FerdigstillModal(props: { type: 'BREV' | 'OPPGAVE' }) {
  const { type } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [open, setOpen] = useState(false)
  const [ferdigstillBrevOgOppgaveStatus, ferdigstillOppgaveOgBrev] = useApiCall(ferdigstillBrevOgOppgaveAktivitetsplikt)
  const [ferdigstillOppgaveStatus, apiFerdigstillOppgave] = useApiCall(ferdigstillOppgave)

  const dispatch = useDispatch()
  const ferdigstill = () => {
    if (type === 'OPPGAVE') {
      apiFerdigstillOppgave(oppgave.id, (oppgave) => {
        dispatch(setAktivitetspliktOppgave(oppgave))
      })
    } else if (type === 'BREV') {
      ferdigstillOppgaveOgBrev({ oppgaveId: oppgave.id }, (oppgave) => {
        dispatch(setAktivitetspliktOppgave(oppgave))
      })
    }
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
            {type === 'BREV' ? (
              <BodyShort>
                Når du ferdigstiller oppgaven vil den bli låst for endringer, og infobrevet vil bli journalført og
                distribuert.
              </BodyShort>
            ) : (
              <BodyShort>
                Når du ferdigstiller oppgaven blir den låst for endringer. Siden det er valgt at ingen brev skal sendes
                vil det ikke bli sendt ut noen infobrev.
              </BodyShort>
            )}

            {mapSuccess(ferdigstillBrevOgOppgaveStatus, () => (
              <Alert variant="success">Oppgaven er ferdigstilt og blir distribuert til mottaker.</Alert>
            ))}
            {mapSuccess(ferdigstillOppgaveStatus, () => (
              <Alert variant="success">Oppgaven er ferdigstilt.</Alert>
            ))}

            {mapFailure(ferdigstillBrevOgOppgaveStatus, (error) => (
              <ApiErrorAlert>Kunne ikke ferdigstille brev og oppgave: {error.detail}</ApiErrorAlert>
            ))}
            {mapFailure(ferdigstillOppgaveStatus, (error) => (
              <ApiErrorAlert>Kunne ikke ferdigstille oppgave: {error.detail}</ApiErrorAlert>
            ))}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="primary"
            onClick={ferdigstill}
            loading={isPending(ferdigstillBrevOgOppgaveStatus) || isPending(ferdigstillOppgaveStatus)}
          >
            Ferdigstill oppgave {type === 'BREV' && ' og brev'}
          </Button>
          <Button
            variant="secondary"
            onClick={() => setOpen(false)}
            disabled={isPending(ferdigstillBrevOgOppgaveStatus) || isPending(ferdigstillOppgaveStatus)}
          >
            {isSuccess(ferdigstillBrevOgOppgaveStatus) || isSuccess(ferdigstillOppgaveStatus) ? 'Lukk' : 'Avbryt'}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
