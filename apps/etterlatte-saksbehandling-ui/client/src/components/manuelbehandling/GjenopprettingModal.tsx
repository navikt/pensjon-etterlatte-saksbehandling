import { BodyLong, Button, Heading, HStack, Modal, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgaveMedMerknad } from '~shared/api/oppgaver'
import { useNavigate } from 'react-router-dom'
import { Oppgavestatus, erOppgaveRedigerbar } from '~shared/types/oppgave'

export default function GjenopprettingModal(props: { oppgaveId: string; oppgaveStatus: Oppgavestatus }) {
  const { oppgaveId, oppgaveStatus } = props
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgaveMedMerknad)
  const [isOpen, setIsOpen] = useState(false)
  const [begrunnelse, setBegrunnelse] = useState<string | null>(null)
  const navigate = useNavigate()

  const avbryt = () => {
    avsluttOppgave({ id: oppgaveId, merknad: begrunnelse }, () => navigate('/'))
  }

  return (
    <>
      <Button
        variant="danger"
        onClick={() => {
          setIsOpen(true)
        }}
        disabled={!erOppgaveRedigerbar(oppgaveStatus)}
      >
        Avslutt oppgave uten behandling
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil avslutte oppgave?
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <BodyLong>Dette er kun nødvendig hvis du skal lukke oppgave uten å lage behandling.</BodyLong>
          <TextField
            onChange={(e) => {
              if (e.target.value === '') {
                setBegrunnelse(null)
              } else {
                setBegrunnelse(e.target.value)
              }
            }}
            label=""
            placeholder="Begrunnelse"
          />
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4" justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} loading={isPending(ferdigstillOppgaveStatus)}>
              Nei
            </Button>
            <Button variant="danger" onClick={avbryt} loading={isPending(ferdigstillOppgaveStatus)}>
              Ja, lukke oppgave
            </Button>
          </HStack>
          {isFailureHandler({
            apiResult: ferdigstillOppgaveStatus,
            errorMessage: 'Det oppsto en feil ved avbryting av behandlingen.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}
