import React, { useState } from 'react'
import { BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGrunnlag } from '~shared/api/behandling'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG = 'pensjon-etterlatte.kan-bruke-oppdater-grunnlag'

export default function OppdaterGrunnlagModal({
  behandlingId,
  behandlingStatus,
}: {
  behandlingId: string
  behandlingStatus: IBehandlingStatus
}) {
  const [isOpen, setIsOpen] = useState(false)
  const featureAktiv = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG, false)
  const behandles = behandlingErRedigerbar(behandlingStatus)
  const [oppdatert, apiOppdaterGrunnlag] = useApiCall(oppdaterGrunnlag)

  if (!featureAktiv || !behandles) return

  const lagre = () => {
    return apiOppdaterGrunnlag(
      {
        behandlingId: behandlingId,
      },
      () => {
        window.location.reload()
      }
    )
  }

  return (
    <div style={{ float: 'right', marginBottom: '2rem' }}>
      <Button
        variant="secondary"
        size="small"
        icon={<ArrowsCirclepathIcon />}
        iconPosition="right"
        onClick={() => setIsOpen(true)}
      >
        Oppdater grunnlag
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className="padding-modal">
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil oppdatere grunnlaget?
          </Heading>

          <BodyLong spacing>
            Oppdater grunnlaget hvis opplysningene i saken ikke samsvarer med hverandre (f.eks. hvis dødsfallet eller
            evenutell verge ikke er registrert når behandlingen ble opprettet). Status på behandlingen vil da bli satt
            til &quot;Opprettet&quot;. Hvis du har startet behandlingen allerede, må du gå gjennom stegene på nytt.
          </BodyLong>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(oppdatert)}>
              Nei, fortsett behandling
            </Button>
            <Button
              variant="primary"
              onClick={() => lagre()}
              loading={isPending(oppdatert)}
              disabled={isPending(oppdatert)}
            >
              Ja, oppdater grunnlaget
            </Button>
          </FlexRow>
          {isFailureHandler({ apiResult: oppdatert, errorMessage: 'Oppdatering feilet' })}
        </Modal.Body>
      </Modal>
    </div>
  )
}
