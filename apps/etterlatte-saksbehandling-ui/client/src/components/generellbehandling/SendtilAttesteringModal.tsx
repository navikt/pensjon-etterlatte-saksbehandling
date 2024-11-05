import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { useNavigate } from 'react-router-dom'
import React, { useState } from 'react'
import { Alert, BodyShort, Button, Modal } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { sendTilAttesteringGenerellBehandling } from '~shared/api/generellbehandling'
import { hentSakOgNavigerTilSaksoversikt } from '~components/generellbehandling/KravpakkeUtlandBehandling'

import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

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
                  hentSakOgNavigerTilSaksoversikt(utlandsBehandling.sakId, navigate)
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
            <Alert variant="success">
              Behandlingen ble sendt til attestering, du blir straks sendt til saksoversikten
            </Alert>
          )}
          {isFailureHandler({
            apiResult: sendTilAttesteringStatus,
            errorMessage: 'Klarte ikke å sende til attestering kravpakke utland. Prøv igjen senere.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}
