import { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { ButtonWrapper } from '~shared/modal/modal'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'

export default function AvbrytBehandling() {
  const navigate = useNavigate()
  const behandling = useBehandling()
  const [isOpen, setIsOpen] = useState(false)

  return behandling?.status && erFerdigBehandlet(behandling.status) ? (
    <Button variant={'tertiary'} className="textButton" onClick={() => navigate('/')}>
      Tilbake til oppgavelisten
    </Button>
  ) : (
    <>
      <Button variant={'tertiary'} className="textButton" onClick={() => setIsOpen(true)}>
        Avbryt
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className={'padding-modal'}>
        <Modal.Content style={{ textAlign: 'center' }}>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil avbryte behandlingen?
          </Heading>

          <BodyLong spacing>
            Du vil bli sendt tilbake til oppgavebenken. <br />
            Endringene dine er lagret og du kan fortsette der du slapp når du går tilbake til saken.
          </BodyLong>

          <ButtonWrapper>
            <Button variant="secondary" size="medium" className="button" onClick={() => setIsOpen(false)}>
              Nei, fortsett behandling
            </Button>
            <Button variant="primary" size="medium" className="button" onClick={() => navigate('/')}>
              Ja, avbryt
            </Button>
          </ButtonWrapper>
        </Modal.Content>
      </Modal>
    </>
  )
}
