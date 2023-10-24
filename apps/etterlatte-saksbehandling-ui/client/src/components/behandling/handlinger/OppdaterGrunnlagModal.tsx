import { useState } from 'react'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { useBehandling } from '~components/behandling/useBehandling'
import { FlexRow } from '~shared/styled'
import { isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { SakType } from '~shared/types/sak'
import { oppdaterGrunnlag } from '~shared/api/behandling'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

export const FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG = 'pensjon-etterlatte.kan-bruke-oppdater-grunnlag'

export default function OppdaterGrunnlagModal() {
  const behandling = useBehandling()
  const [isOpen, setIsOpen] = useState(false)

  const kanOppdatereGrunnlag = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG, false)

  if (!kanOppdatereGrunnlag || behandling == null) return

  const [oppdatert, apiOppdaterGrunnlag] = useApiCall(oppdaterGrunnlag)

  const lagre = () => {
    return apiOppdaterGrunnlag(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        sakType: SakType.BARNEPENSJON,
      },
      () => {
        setIsOpen(false)
      }
    )
  }

  return (
    <>
      <Button variant="primary" onClick={() => setIsOpen(true)}>
        Oppdater grunnlag
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading" className="padding-modal">
        <Modal.Body style={{ textAlign: 'center' }}>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil oppdatere grunnlaget?
          </Heading>

          <BodyLong spacing>
            Hvis grunnlaget ikke samsvarer med virkeligheten må du oppdatere grunnlaget. <br />
            Statusen vil bli satt til Opprettet og hvis du har startet behandlingen allerede må du gå gjennom stegene på
            nytt.
          </BodyLong>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett behandling
            </Button>
            <Button variant="primary" onClick={() => lagre()}>
              Ja, oppdater grunnlaget
            </Button>
          </FlexRow>
          {isFailure(oppdatert) && <Alert variant="error">Oppdatering feilet</Alert>}
        </Modal.Body>
      </Modal>
    </>
  )
}
