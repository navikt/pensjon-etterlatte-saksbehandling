import { BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useState } from 'react'
import { handlinger } from './typer'
import { useNavigate } from 'react-router-dom'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexRow } from '~shared/styled'
import { ApiResponse } from '~shared/api/apiClient'
import { useSjekkliste } from '~components/behandling/sjekkliste/useSjekkliste'
import { useAppDispatch } from '~store/Store'
import { visSjekkliste } from '~store/reducers/BehandlingSidemenyReducer'
import { addValideringsfeil } from '~store/reducers/SjekklisteReducer'
import { useBehandling } from '~components/behandling/useBehandling'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export const SendTilAttesteringModal = ({
  behandlingId,
  fattVedtakApi,
}: {
  behandlingId: string
  fattVedtakApi: (id: string) => Promise<ApiResponse<unknown>>
}) => {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [fattVedtakStatus, fattVedtak] = useApiCall(fattVedtakApi)

  const sjekkliste = useSjekkliste()
  const behandling = useBehandling()
  const dispatch = useAppDispatch()

  const goToOppgavebenken = () => {
    navigate('/')
  }

  const send = () => {
    fattVedtak(behandlingId, () => {
      setIsOpen(false)
      goToOppgavebenken()
    })
  }

  const clickAttester = () => {
    if (
      behandling?.behandlingType == IBehandlingsType.FØRSTEGANGSBEHANDLING &&
      (sjekkliste == null || !sjekkliste.bekreftet)
    ) {
      dispatch(addValideringsfeil('Feltet må hukes av for å ferdigstilles'))
      dispatch(visSjekkliste())
    } else {
      setIsOpen(true)
    }
  }

  return (
    <>
      <Button variant="primary" onClick={clickAttester}>
        {handlinger.ATTESTERING.navn}
      </Button>
      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
        }}
        aria-labelledby="modal-heading"
        className="padding-modal"
      >
        <Modal.Body>
          <Heading spacing level="1" id="modal-heading" size="medium">
            Er du sikker på at du vil sende vedtaket til attestering?
          </Heading>
          <BodyShort spacing>Når du sender til attestering vil vedtaket låses og du får ikke gjort endringer</BodyShort>
          <FlexRow justify="center">
            <Button
              variant="secondary"
              onClick={() => {
                setIsOpen(false)
              }}
            >
              {handlinger.ATTESTERING_MODAL.NEI.navn}
            </Button>
            <Button loading={isPending(fattVedtakStatus)} variant="primary" onClick={send}>
              {handlinger.ATTESTERING_MODAL.JA.navn}
            </Button>
          </FlexRow>
          {isFailure(fattVedtakStatus) && (
            <ApiErrorAlert>
              {fattVedtakStatus.error.detail || 'En feil skjedde under attestering av vedtaket'}
            </ApiErrorAlert>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
