import { useState } from 'react'
import { Button, Heading, Modal } from '@navikt/ds-react'
import { WarningText } from '~shared/styled'
import { avbrytBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { handlinger } from './typer'
import { ApiResponse } from '~shared/api/apiClient'
import { ButtonWrapper } from '~shared/modal/modal'
import { useAppSelector } from '~store/Store'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export const AvbrytBehandling = () => {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [error, setError] = useState(false)

  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const erFoerstegangsbehandling = behandling?.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING

  const avbryt = () => {
    if (behandling?.id) {
      avbrytBehandling(behandling.id).then((response: ApiResponse<any>) => {
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
        {erFoerstegangsbehandling ? handlinger.AVBRYT.navn : handlinger.AVBRYT_REVURDERING.navn}
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
            Er du sikker på at du vil avbryte behandlingen?
            {erFoerstegangsbehandling ? 'Saken blir annullert og kan ikke behandles videre i Gjenny.' : null}
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
          {error && <WarningText>Det oppsto en feil ved avbryting av behandlingen.</WarningText>}
        </Modal.Content>
      </Modal>
    </>
  )
}
