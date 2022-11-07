import { useState } from 'react'
import { Button, Link } from '@navikt/ds-react'
import { Modal } from '../../../shared/modal/modal'
import { WarningText } from '../../../shared/styled'
import { avbrytBehandling } from '../../../shared/api/behandling'
import { useMatch, useNavigate } from 'react-router'
import { handlinger } from './typer'
import { ApiResponse } from '../../../shared/api/apiClient'

export const AvbrytBehandling = () => {
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/*')
  const [isOpen, setIsOpen] = useState(false)
  const [error, setError] = useState(false)

  const avbryt = () => {
    //TODO!
    if (match?.params.behandlingId) {
      avbrytBehandling(match.params.behandlingId).then((response: ApiResponse<any>) => {
        if (response.status === 'ok') {
          navigate('/')
        } else {
          setError(true)
        }
      })
    }
  }

  return (
    <>
      <Link className="textButton" onClick={() => setIsOpen(true)}>
        {handlinger.AVBRYT.navn}
      </Link>

      {isOpen && (
        <Modal
          onClose={() => {
            setIsOpen(false)
            setError(false)
          }}
        >
          <h2>Er du sikker på at du vil avbryte behandlingen?</h2>
          <Button
            variant="secondary"
            size="medium"
            className="button"
            onClick={() => {
              setIsOpen(false)
              setError(false)
            }}
          >
            {handlinger.AVBRYT_MODAL.NEI.navn}
          </Button>
          <Button variant="primary" size="medium" className="button" onClick={avbryt}>
            {handlinger.AVBRYT_MODAL.JA.navn}
          </Button>
          {error && <WarningText>Det oppsto enn feil ved avbryting av behandlingen.</WarningText>}
        </Modal>
      )}
    </>
  )
}
