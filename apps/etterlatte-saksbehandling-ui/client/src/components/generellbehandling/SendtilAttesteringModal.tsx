import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { useNavigate } from 'react-router-dom'
import React, { useState } from 'react'
import { Alert, BodyShort, Button, Modal } from '@navikt/ds-react'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { sendTilAttesteringGenerellBehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigererTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtland'

export const SendtilAttesteringModal = ({
  utlandsBehandling,
}: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland }
}) => {
  const navigate = useNavigate()
  const [sendTilAttesteringStatus, sendTilAttestering] = useApiCall(sendTilAttesteringGenerellBehandling)
  const [open, setOpen] = useState<boolean>(false)

  return (
    <>
      <Button onClick={() => setOpen((prev) => !prev)}>Send til attestering</Button>
      <Modal open={open} header={{ heading: 'Overskrift' }} onClose={() => setOpen(false)}>
        <Modal.Body>
          <BodyShort>Vil du sende kravpakken til attestering?</BodyShort>
        </Modal.Body>
        <Modal.Footer>
          <Button
            disabled={isSuccess(sendTilAttesteringStatus)}
            type="button"
            onClick={() => {
              sendTilAttestering(utlandsBehandling, () => {
                setTimeout(() => {
                  hentSakOgNavigererTilSaksoversikt(utlandsBehandling.sakId, navigate)
                }, 4000)
              })
            }}
          >
            Send til attestering
          </Button>
          <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
            Nei, avbryt
          </Button>
          {isSuccess(sendTilAttesteringStatus) && (
            <Alert variant="success">Behandlingen ble sendt til attestering</Alert>
          )}
          {isFailure(sendTilAttesteringStatus) && (
            <ApiErrorAlert>Klarte ikke å sende til attestering kravpakke utland. Prøv igjen senere.</ApiErrorAlert>
          )}
        </Modal.Footer>
      </Modal>
    </>
  )
}
