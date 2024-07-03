import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOmgjoeringFoerstegangsbehandling } from '~shared/api/revurdering'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'

export function OmgjoerAvslagModal(props: { sakId: number; harAapenBehandling: boolean }) {
  const { sakId, harAapenBehandling } = props
  const [open, setOpen] = useState(false)
  const [opprettOmgjoeringStatus, opprettOmgjoering] = useApiCall(opprettOmgjoeringFoerstegangsbehandling)
  const navigate = useNavigate()
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
            Hvis det kun er avslåtte / avbrutte førstegangsbehandlinger i saken må en eventuell omgjøring gjøres som en
            ny førstegangsbehandling.
          </BodyShort>
          {harAapenBehandling && (
            <Alert variant="warning">
              Saken har allerede en åpen behandling. Denne må avsluttes eller avbrytes før en omgjøring kan startes.
            </Alert>
          )}
          {mapResult(opprettOmgjoeringStatus, {
            success: () => (
              <Alert variant="success">
                Ny førstegangsbehandling opprettet! Last siden på nytt for å se den i listen, eller gå rett inn på
                behandlingen.
              </Alert>
            ),
            error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
          })}
        </Modal.Body>
        <Modal.Footer>
          {isSuccess(opprettOmgjoeringStatus) ? (
            <>
              <Button variant="primary" onClick={() => navigate(`/behandling/${opprettOmgjoeringStatus.data.id}`)}>
                Åpne førstegangsbehandling
              </Button>
              <Button variant="secondary" onClick={lukkModal}>
                Lukk
              </Button>
            </>
          ) : (
            <>
              <Button variant="primary" loading={isPending(opprettOmgjoeringStatus)} onClick={opprett}>
                Opprett ny førstegangsbehandling
              </Button>
              <Button variant="secondary" disabled={isPending(opprettOmgjoeringStatus)} onClick={lukkModal}>
                Avbryt
              </Button>
            </>
          )}
        </Modal.Footer>
      </Modal>
    </>
  )
}
