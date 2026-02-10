import { ISak } from '~shared/types/sak'
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOppgave } from '~shared/api/oppgaver'
import { OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { Alert, BodyShort, Box, Button, Modal, TextField, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { ClickEvent, trackClick } from '~utils/analytics'

interface OpprettJournalpostForm {
  journalpostId: string
}

export function OpprettOppgaveTilJournalpost({ sak }: { sak: ISak }) {
  const [modalOpen, setModalOpen] = useState(false)
  const { handleSubmit, register, reset } = useForm<OpprettJournalpostForm>()
  const [opprettOppgaveStatus, opprettOppgaveFetch, resetOpprettOppgaveStatus] = useApiCall(opprettOppgave)

  function sendInn(skjemadata: OpprettJournalpostForm) {
    trackClick(ClickEvent.OPPRETT_FRIKOBLET_JOURNALFOERINGSOPPGAVE)
    opprettOppgaveFetch({
      sakId: sak.id,
      request: {
        oppgaveType: Oppgavetype.JOURNALFOERING,
        referanse: skjemadata.journalpostId,
        merknad: 'Manuelt opprettet journalføringsoppgave',
        oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
      },
    })
  }

  function lukkModal() {
    resetOpprettOppgaveStatus()
    setModalOpen(false)
    reset()
  }

  return (
    <Box>
      <Modal
        open={modalOpen}
        onClose={lukkModal}
        header={{
          heading: 'Opprett oppgave for journalpost',
        }}
      >
        <form onSubmit={handleSubmit(sendInn)}>
          <Modal.Body>
            <VStack gap="space-4">
              <TextField
                {...register('journalpostId', {
                  required: {
                    value: true,
                    message: 'Du må oppgi journalpost-id',
                  },
                })}
                label="JournalpostId"
                disabled={isPending(opprettOppgaveStatus) || isSuccess(opprettOppgaveStatus)}
              />
              {mapResult(opprettOppgaveStatus, {
                pending: <Spinner label="Oppretter journalføringsoppgave" />,
                error: (e) => <ApiErrorAlert>Kunne ikke opprette oppgaven, på grunn av feil: {e.detail}</ApiErrorAlert>,
                success: (opprettetOppgave) => (
                  <Alert variant="success">
                    <BodyShort>
                      Opprettet en journalføringsoppgave knyttet til journalposten med id: {opprettetOppgave.referanse}.
                    </BodyShort>
                    <BodyShort>
                      <PersonLink fnr={sak.ident}>Gå til saksoversikten</PersonLink>
                    </BodyShort>
                  </Alert>
                ),
              })}
            </VStack>
          </Modal.Body>
          <Modal.Footer>
            <Button
              type="submit"
              variant="primary"
              loading={isPending(opprettOppgaveStatus)}
              disabled={isSuccess(opprettOppgaveStatus)}
            >
              Opprett oppgave
            </Button>
            <Button type="button" variant="secondary" onClick={lukkModal} disabled={isPending(opprettOppgaveStatus)}>
              Avbryt
            </Button>
          </Modal.Footer>
        </form>
      </Modal>

      <Button variant="tertiary" size="small" onClick={() => setModalOpen(true)}>
        Opprett oppgave for annen journalpost
      </Button>
    </Box>
  )
}
