import { useState } from 'react'
import { Button, Heading, Modal } from '@navikt/ds-react'
import { WarningText } from '~shared/styled'
import { avbrytBehandling } from '~shared/api/behandling'
import { useMatch, useNavigate } from 'react-router'
import { handlinger } from './typer'
import { ApiResponse } from '~shared/api/apiClient'
import { ButtonWrapper } from '~shared/modal/modal'

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
      <Button variant={'tertiary'} className="textButton" onClick={() => setIsOpen(true)}>
        {handlinger.AVBRYT.navn}
      </Button>

      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
          setError(false)
        }}
        aria-labelledby="modal-heading"
        className={'padding-modal'}
      >
        <Modal.Content>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil avbryte behandlingen? Saken blir annullert og kan ikke behandles videre i Gjenny.
          </Heading>
          <ButtonWrapper>
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
          </ButtonWrapper>
          {error && <WarningText>Det oppsto enn feil ved avbryting av behandlingen.</WarningText>}
        </Modal.Content>
      </Modal>
    </>
  )
}
