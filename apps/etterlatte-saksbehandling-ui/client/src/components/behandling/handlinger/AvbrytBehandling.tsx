import { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { FlexRow } from '~shared/styled'

export default function AvbrytBehandling() {
  const navigate = useNavigate()
  const behandling = useBehandling()
  const [isOpen, setIsOpen] = useState(false)

  return behandling?.status && erFerdigBehandlet(behandling.status) ? (
    <Button variant="tertiary" onClick={() => navigate('/')}>
      Tilbake til oppgavelisten
    </Button>
  ) : (
    <>
      <Button variant="tertiary" onClick={() => setIsOpen(true)}>
        Avbryt
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className="padding-modal">
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil avbryte behandlingen?
          </Heading>

          <BodyLong spacing>
            Du vil bli sendt tilbake til oppgavebenken. <br />
            Endringene dine er lagret og du kan fortsette der du slapp når du går tilbake til saken.
          </BodyLong>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett behandling
            </Button>
            <Button variant="primary" onClick={() => navigate('/')}>
              Ja, avbryt
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}
