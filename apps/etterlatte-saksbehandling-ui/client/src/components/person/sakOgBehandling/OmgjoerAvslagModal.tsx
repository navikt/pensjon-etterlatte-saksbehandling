import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOmgjoeringFoerstegangsbehandling } from '~shared/api/revurdering'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export function OmgjoerAvslagModal(props: { sakId: number }) {
  const sakId = props.sakId
  const [open, setOpen] = useState(false)
  const [opprettOmgjoeringStatus, opprettOmgjoering] = useApiCall(opprettOmgjoeringFoerstegangsbehandling)

  function opprett() {
    opprettOmgjoering({ sakId })
  }

  function lukkModal() {
    setOpen(false)
  }

  return (
    <>
      <Button variant="secondary" size="medium" onClick={() => setOpen(true)}>
        Omgjør førstegangsbehandling
      </Button>
      <Modal open={open} onClose={lukkModal} aria-labelledby="omgjoer-foerstegangsbehandling-modal-header">
        <Modal.Header closeButton>
          <Heading id="omgjoer-foerstegangsbehandling-modal-header" level="2" size="medium">
            Omgjør førstegangsbehandling
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <BodyShort>
            Hvis det kun er avslåtte / avbrutte førstegangsbehandlinger i saken (ingenting er iverksatt mot oppdrag) må
            en eventuell omgjøring / rebehandling gjøres som en ny førstegangsbehandling.
          </BodyShort>
          {isFailureHandler({
            apiResult: opprettOmgjoeringStatus,
            errorMessage:
              'Kunne ikke opprette omgjøring av førstegangsbehandling. Prøv på nytt om litt, og meld sak hvis det fremdeles er feil',
          })}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="primary" loading={isPending(opprettOmgjoeringStatus)} onClick={opprett}>
            Opprett ny førstegangsbehandling
          </Button>
          <Button variant="secondary" disabled={isPending(opprettOmgjoeringStatus)} onClick={lukkModal}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
