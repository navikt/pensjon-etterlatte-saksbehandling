import { TilbakekrevingBehandling, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOmgjoeringTilbakekreving } from '~shared/api/tilbakekreving'
import { Alert, BodyShort, Button, Modal, VStack } from '@navikt/ds-react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { lenkeTilTilbakekrevingMedId } from '~components/person/sakOgBehandling/TilbakekrevingListe'

export function OmgjoerTilbakekreving({ tilbakekreving }: { tilbakekreving: TilbakekrevingBehandling }) {
  const [modalOpen, setModalOpen] = useState(false)
  const [opprettOmgjoeringResult, opprettOmgjoeringFetch, resetOpprettOmgjoering] = useApiCall(
    opprettOmgjoeringTilbakekreving
  )

  if (tilbakekreving.status !== TilbakekrevingStatus.ATTESTERT) return null

  function lukkModal() {
    setModalOpen(false)
    resetOpprettOmgjoering()
  }

  return (
    <>
      <Button variant="secondary" size="small" onClick={() => setModalOpen(true)}>
        Omgjør
      </Button>
      <Modal open={modalOpen} header={{ heading: 'Omgjør tilbakekreving' }} onClose={lukkModal}>
        <Modal.Body>
          <VStack gap="4">
            <BodyShort>En tilbakekreving kan omgjøres på grunn av feil i vedtaket.</BodyShort>
            {mapResult(opprettOmgjoeringResult, {
              success: (tilbakekreving) => (
                <Alert variant="success">
                  <VStack gap="2">
                    <BodyShort>Omgjøring av tilbakekrevingen er opprettet.</BodyShort>
                    <Button size="small" as="a" href={lenkeTilTilbakekrevingMedId(tilbakekreving.id)}>
                      Gå til tilbakekreving
                    </Button>
                  </VStack>
                </Alert>
              ),
              error: (error) => (
                <ApiErrorAlert>
                  Kunne ikke lage omgjøring av tilbakekrevingen, på grunn av feil: {error.detail}
                </ApiErrorAlert>
              ),
            })}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button
            loading={isPending(opprettOmgjoeringResult)}
            onClick={() => opprettOmgjoeringFetch({ tilbakekrevingId: tilbakekreving.id })}
          >
            Lag omgjøring
          </Button>
          <Button disabled={isPending(opprettOmgjoeringResult)} variant="secondary" onClick={lukkModal}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
