import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillOppgave } from '~shared/api/oppgaver'

export default function GjenopprettingModal() {
  const [status, avsluttOppgave] = useApiCall(ferdigstillOppgave)
  const [isOpen, setIsOpen] = useState(false)

  const avbryt = () => {
    avsluttOppgave('', () => {})
  }

  return (
    <>
      <Button
        variant="danger"
        onClick={() => {
          setIsOpen(true)
        }}
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
            <Button variant="secondary" onClick={() => setIsOpen(false)} loading={isPending(status)}>
              Nei
            </Button>
            <Button variant="danger" onClick={avbryt} loading={isPending(status)}>
              Ja, lukke oppgave
            </Button>
          </FlexRow>
          {isFailureHandler({
            apiResult: status,
            errorMessage: 'Det oppsto en feil ved avbryting av behandlingen.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}
