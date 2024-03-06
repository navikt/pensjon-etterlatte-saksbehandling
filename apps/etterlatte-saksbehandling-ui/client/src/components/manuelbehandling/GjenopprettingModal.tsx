import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { erOppgaveRedigerbar, ferdigstillOppgave, Oppgavestatus } from '~shared/api/oppgaver'
import { useNavigate } from 'react-router-dom'

export default function GjenopprettingModal(props: { oppgaveId: string; oppgaveStatus: Oppgavestatus }) {
  const { oppgaveId, oppgaveStatus } = props
  const [ferdigstillOppgaveStatus, avsluttOppgave] = useApiCall(ferdigstillOppgave)
  const [isOpen, setIsOpen] = useState(false)
  const navigate = useNavigate()

  const avbryt = () => {
    avsluttOppgave(oppgaveId, () => navigate('/'))
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
        </Modal.Body>

        <Modal.Footer>
          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} loading={isPending(ferdigstillOppgaveStatus)}>
              Nei
            </Button>
            <Button variant="danger" onClick={avbryt} loading={isPending(ferdigstillOppgaveStatus)}>
              Ja, lukke oppgave
            </Button>
          </FlexRow>
          {isFailureHandler({
            apiResult: ferdigstillOppgaveStatus,
            errorMessage: 'Det oppsto en feil ved avbryting av behandlingen.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}
