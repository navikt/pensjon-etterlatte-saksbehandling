import { useState } from 'react'
import { Alert, Button, ExpansionCard, Heading, Modal } from '@navikt/ds-react'
import { avbrytBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { ButtonWrapper } from '~shared/modal/modal'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { SakType } from '~shared/types/sak'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { formaterBehandlingstype } from '~utils/formattering'
import { ExclamationmarkTriangleFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'

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
    avbrytBehandlingen(behandling!!.id, () => {
      if (behandling?.søker?.foedselsnummer) {
        navigate(`/person/${behandling.søker?.foedselsnummer}`)
      } else {
        window.location.reload() // Bare refresh behandling
      }
    })
  }

  return (
    <>
      <ExpansionCardSpaced aria-labelledby={'card-heading'}>
        <ExpansionCard.Header>
          <div className="with-icon">
            <div className="icon">
              <ExclamationmarkTriangleFillIcon aria-hidden />
            </div>
            <div>
              <Heading size={'xsmall'} id={'card-heading'}>
                Annuller {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
              </Heading>
            </div>
          </div>
        </ExpansionCard.Header>

        <ExpansionCard.Content>
          <Alert variant={'warning'}>
            {erFoerstegangsbehandling
              ? 'Hvis denne behandlingen ikke kan behandles i Gjenny må sak og behandling annulleres.'
              : 'Hvis denne behandlingen er uaktuell, kan du annullere den her.'}
          </Alert>
          <br />
          <div className="flex">
            <Button variant={'danger'} onClick={() => setIsOpen(true)} icon={<XMarkIcon />}>
              Annuller {formaterBehandlingstype(behandling!!.behandlingType).toLowerCase()}
            </Button>
          </div>
        </ExpansionCard.Content>
      </ExpansionCardSpaced>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level={'1'} spacing size={'medium'} id="modal-heading">
            Er du sikker på at du vil annullere behandlingen?
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <Alert variant={'warning'}>
            {erFoerstegangsbehandling
              ? 'Saken blir annullert og kan ikke behandles videre i Gjenny. Saken må manuelt opprettes på nytt i Pesys.'
              : 'Behandlingen blir avsluttet og kan opprettes på nytt.'}
          </Alert>
        </Modal.Body>

        <Modal.Footer>
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
        </Modal.Footer>
      </Modal>
    </>
  )
}

const ExpansionCardSpaced = styled(ExpansionCard)`
  margin: 20px 8px 0 8px;
  border-radius: 3px;

  .title {
    white-space: nowrap;
  }

  .navds-expansioncard__header {
    border-radius: 3px;
  }

  .with-icon {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .icon {
    font-size: 2rem;
    flex-shrink: 0;
    display: grid;
    place-content: center;
  }
`
