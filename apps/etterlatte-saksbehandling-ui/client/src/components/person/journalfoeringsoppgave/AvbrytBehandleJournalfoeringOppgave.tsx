import { useNavigate } from 'react-router'
import { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { ButtonWrapper } from '~shared/modal/modal'

export default function AvbrytBehandleJournalfoeringOppgave() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)

  return (
    <>
      <Button variant={'tertiary'} className="textButton" onClick={() => setIsOpen(true)}>
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

          <ButtonWrapper>
            <Button variant="secondary" size="medium" className="button" onClick={() => setIsOpen(false)}>
              Nei, fortsett
            </Button>
            <Button variant="danger" size="medium" className="button" onClick={() => navigate('/')}>
              Ja, avbryt
            </Button>
          </ButtonWrapper>
        </Modal.Body>
      </Modal>
    </>
  )
}
