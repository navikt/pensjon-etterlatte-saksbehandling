import { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { WarningText } from '~shared/styled'
import { annullerBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { ApiResponse } from '~shared/api/apiClient'
import { ButtonWrapper } from '~shared/modal/modal'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'

export default function AnnullerBehandling() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [error, setError] = useState(false)

  const behandling = useBehandling()
  const erFoerstegangsbehandling = behandling?.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING

  const behandles = hentBehandlesFraStatus(behandling?.status ?? IBehandlingStatus.IVERKSATT)
  if (!behandles) {
    return null
  }

  const annuller = () => {
    if (behandling?.id) {
      annullerBehandling(behandling.id).then((response: ApiResponse<any>) => {
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
      <SidebarPanel>
        <Heading size={'small'} spacing>
          {erFoerstegangsbehandling ? 'Annullering av saken' : 'Avbryte behandling'}.
        </Heading>

        <BodyLong spacing size={'small'}>
          Dersom behandlingen ikke kan behandles i Gjenny må
          {erFoerstegangsbehandling ? ' saken annulleres' : ' den avbrytes'}.
        </BodyLong>

        <div className="flex">
          <Button variant={'danger'} size={'xsmall'} className="textButton" onClick={() => setIsOpen(true)}>
            {erFoerstegangsbehandling ? 'Avbryt behandling og annuller saken' : 'Avbryt behandling'}
          </Button>
        </div>
      </SidebarPanel>

      <Modal
        open={isOpen}
        onClose={() => {
          setIsOpen(false)
          setError(false)
        }}
        aria-labelledby="modal-heading"
        className={'padding-modal'}
      >
        <Modal.Content style={{ textAlign: 'center' }}>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil avbryte behandlingen?
          </Heading>
          <BodyLong spacing>
            {erFoerstegangsbehandling
              ? 'Saken blir annullert og kan ikke behandles videre i Gjenny. Saken må manuelt opprettes på nytt i Pesys.'
              : 'Behandlingen blir avsluttet og kan opprettes på nytt.'}
          </BodyLong>

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
              Nei, fortsett behandling
            </Button>
            <Button variant="danger" size="medium" className="button" onClick={annuller}>
              {erFoerstegangsbehandling ? 'Ja, avbryt behandling og annuller saken' : 'Ja, avbryt behandling'}
            </Button>
          </ButtonWrapper>
          {error && <WarningText>Det oppsto en feil ved avbryting av behandlingen.</WarningText>}
        </Modal.Content>
      </Modal>
    </>
  )
}
