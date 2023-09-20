import { useNavigate } from 'react-router'
import { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'

export default function AvbrytBehandleJournalfoeringOppgave() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)

  return (
    <>
      <Button variant={'tertiary'} onClick={() => setIsOpen(true)}>
        Avbryt
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className={'padding-modal'}>
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil avbryte?
          </Heading>

          <BodyLong spacing>
            Du vil bli sendt tilbake til oppgavebenken. <br />
            Det du har gjort til nå vil bli slettet og du må starte oppgavebehandlingen på nytt.
          </BodyLong>

          <FlexRow justify={'center'}>
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett
            </Button>
            <Button variant="danger" onClick={() => navigate('/')}>
              Ja, avbryt
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}
