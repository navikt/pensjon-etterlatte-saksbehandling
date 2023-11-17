import { Alert, BodyShort, Button, Modal, Textarea } from '@navikt/ds-react'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useState } from 'react'
import { underkjennGenerellbehandling } from '~shared/api/generellbehandling'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { useNavigate } from 'react-router-dom'

export const UnderkjenneModal = ({
  utlandsBehandling,
}: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const [underkjennStatus, underkjennFetch] = useApiCall(underkjennGenerellbehandling)
  const [open, setOpen] = useState<boolean>(false)
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')
  const [error, setError] = useState<string>('')
  const navigate = useNavigate()

  return (
    <>
      <Button onClick={() => setOpen((prev) => !prev)}>Underkjenn</Button>
      <Modal open={open} header={{ heading: 'Overskrift' }} onClose={() => setOpen(false)}>
        <Modal.Body>
          <BodyShort>Vil du underkjenne kravpakken? Vennligst begrunn hvorfor nedenfor:</BodyShort>
          <Textarea
            label="Beskriv hvorfor"
            size="small"
            value={fritekstgrunn}
            onChange={(e) => {
              setFritekstgrunn(e.target.value)
              setError('')
            }}
          />
          {error && (
            <Alert variant="error" style={{ marginTop: '2rem' }}>
              Du må fylle inn en begrunnelse
            </Alert>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button
            disabled={isSuccess(underkjennStatus)}
            type="button"
            onClick={() => {
              if (fritekstgrunn === '') {
                setError('Du må fylle ut en begrunnelse')
              } else {
                underkjennFetch({ generellbehandling: utlandsBehandling, begrunnelse: fritekstgrunn }, () => {
                  setTimeout(() => {
                    navigate('/')
                  }, 5000)
                })
              }
            }}
          >
            Underkjenn
          </Button>
          <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
            Avbryt
          </Button>
          {isSuccess(underkjennStatus) && <Alert variant="success">Behandlingen ble underkjent</Alert>}
          {isFailure(underkjennStatus) && <Alert variant="error">Behandlingen ble ikke underkjent</Alert>}
        </Modal.Footer>
      </Modal>
    </>
  )
}
