import React, { useState } from 'react'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { avbrytBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { ButtonWrapper } from '~shared/modal/modal'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SidebarPanel } from '~components/behandling/SideMeny/SideMeny'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { SakType } from '~shared/types/sak'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { formaterBehandlingstype } from '~utils/formattering'

export default function AnnullerBehandling() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [status, avbrytBehandlingen] = useApiCall(avbrytBehandling)

  const behandling = useBehandling()
  const erFoerstegangsbehandling = behandling?.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING
  const erFoerstegangsbehandlingOgOmstillingsstoenad =
    behandling?.sakType == SakType.OMSTILLINGSSTOENAD && erFoerstegangsbehandling

  const behandles = hentBehandlesFraStatus(behandling?.status ?? IBehandlingStatus.IVERKSATT)
  if (!behandles || erFoerstegangsbehandlingOgOmstillingsstoenad) {
    return null
  }

  const avbryt = () => {
    if (behandling?.id) {
      avbrytBehandlingen(behandling.id, () => {
        if (behandling.søker?.foedselsnummer) {
          navigate(`/person/${behandling.søker?.foedselsnummer}`)
        } else {
          window.location.reload() // Bare refresh behandling
        }
      })
    }
  }

  return (
    <>
      <SidebarPanel>
        <Heading size={'small'} spacing>
          Avbryt {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
        </Heading>

        <BodyLong spacing size={'small'}>
          {erFoerstegangsbehandling
            ? 'Hvis denne behandlingen ikke kan behandles i Gjenny må sak og behandling avbrytes.'
            : 'Hvis denne behandlingen er uaktuell, kan du avbryte den her.'}
        </BodyLong>

        <div className="flex">
          <Button variant={'danger'} size={'xsmall'} className="textButton" onClick={() => setIsOpen(true)}>
            Avbryt {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
          </Button>
        </div>
      </SidebarPanel>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className={'padding-modal'}>
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil avbryte behandlingen?
          </Heading>
          <BodyLong spacing>
            {erFoerstegangsbehandling
              ? 'Behandlingen blir avbrutt og kan ikke behandles videre i Gjenny. Saken må annulleres fra ' +
                'saksoversikten og deretter opprettes på nytt i Pesys.'
              : 'Behandlingen blir avsluttet og kan opprettes på nytt.'}
          </BodyLong>

          <ButtonWrapper>
            <Button
              variant="secondary"
              size="medium"
              className="button"
              onClick={() => setIsOpen(false)}
              disabled={isPending(status)}
            >
              Nei, fortsett {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
            </Button>
            <Button
              variant="danger"
              size="medium"
              className="button"
              onClick={avbryt}
              loading={isPending(status)}
              disabled={isPending(status)}
            >
              Ja, avbryt {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
            </Button>
          </ButtonWrapper>

          {isFailure(status) && <Alert variant={'error'}>Det oppsto en feil ved avbryting av behandlingen.</Alert>}
        </Modal.Body>
      </Modal>
    </>
  )
}
