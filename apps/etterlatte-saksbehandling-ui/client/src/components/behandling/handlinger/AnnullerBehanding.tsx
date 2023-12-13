import { useState } from 'react'
import { BodyLong, Button, ExpansionCard, Heading, Modal } from '@navikt/ds-react'
import { avbrytBehandling } from '~shared/api/behandling'
import { useNavigate } from 'react-router'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ExclamationmarkTriangleFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export default function AnnullerBehandling() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [status, avbrytBehandlingen] = useApiCall(avbrytBehandling)

  const behandling = useBehandling()
  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const erFoerstegangsbehandling = behandling?.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING

  const behandles = behandlingErRedigerbar(behandling?.status ?? IBehandlingStatus.IVERKSATT)
  if (!behandles) {
    return null
  }

  const avbryt = () => {
    avbrytBehandlingen(behandling!!.id, () => {
      if (soeker?.foedselsnummer) {
        navigate(`/person/${soeker?.foedselsnummer}`)
      } else {
        window.location.reload() // Bare refresh behandling
      }
    })
  }

  return (
    <>
      <ExpansionCardSpaced aria-labelledby="card-heading">
        <ExpansionCard.Header>
          <div className="with-icon">
            <div className="icon">
              <ExclamationmarkTriangleFillIcon aria-hidden />
            </div>
            <div>
              <Heading size="xsmall" id="card-heading">
                Annuller behandling
              </Heading>
            </div>
          </div>
        </ExpansionCard.Header>

        <ExpansionCard.Content>
          <BodyLong>
            {erFoerstegangsbehandling
              ? 'Hvis denne behandlingen ikke kan behandles i Gjenny må sak og behandling annulleres.'
              : 'Hvis denne behandlingen er uaktuell, kan du annullere den her.'}
          </BodyLong>
          <br />
          <div className="flex">
            <Button size="small" variant="danger" onClick={() => setIsOpen(true)} icon={<XMarkIcon />}>
              Annuller behandling
            </Button>
          </div>
        </ExpansionCard.Content>
      </ExpansionCardSpaced>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil annullere behandlingen?
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <BodyLong>
            {erFoerstegangsbehandling
              ? 'Behandlingen blir annullert og kan ikke tas videre i Gjenny. Du vil bli sendt til saksoversikten til bruker der behandlingen får status avbrutt.'
              : 'Behandlingen blir avsluttet og kan opprettes på nytt.'}
          </BodyLong>
        </Modal.Body>

        <Modal.Footer>
          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} loading={isPending(status)}>
              Nei, fortsett behandling
            </Button>
            <Button variant="danger" onClick={avbryt} loading={isPending(status)}>
              Ja, annuller behandling
            </Button>
          </FlexRow>
          {isFailureHandler({ apiResult: status, errorMessage: 'Det oppsto en feil ved avbryting av behandlingen.' })}
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
