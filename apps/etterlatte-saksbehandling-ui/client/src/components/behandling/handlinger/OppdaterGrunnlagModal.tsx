import { useState } from 'react'
import { Alert, BodyLong, Button, Heading, Modal } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGrunnlag } from '~shared/api/behandling'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG = 'pensjon-etterlatte.kan-bruke-oppdater-grunnlag'

export default function OppdaterGrunnlagModal({ behandling }: { behandling: IDetaljertBehandling }) {
  const [isOpen, setIsOpen] = useState(false)
  const featureAktiv = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_OPPDATER_GRUNNLAG, false)
  const behandles = hentBehandlesFraStatus(behandling.status)
  const [oppdatertStatus, apiOppdaterGrunnlag] = useApiCall(oppdaterGrunnlag)

  const oppdaterGrunnlagWrapper = () => {
    apiOppdaterGrunnlag(
      {
        behandlingId: behandling.id,
      },
      () => {
        setIsOpen(false)
      }
    )
  }

  if (!featureAktiv || !behandles) return

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
            Oppdater grunnlaget hvis det ikke samsvarer med virkeligheten (f.eks. hvis dødsfallet eller ev. verge ikke
            er registrert når behandlingen ble opprettet). Status på behandlingen vil da bli satt til
            &quot;Opprettet&quot;. Hvis du har startet behandlingen allerede, må du gå gjennom stegene på nytt.
          </BodyLong>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Nei, fortsett behandling
            </Button>
            <Button variant="primary" onClick={() => oppdaterGrunnlagWrapper()} loading={isPending(oppdatertStatus)}>
              Ja, oppdater grunnlaget
            </Button>
          </FlexRow>
          {isFailure(oppdatertStatus) && <Alert variant="error">Oppdatering feilet</Alert>}
        </Modal.Body>
      </Modal>
    </>
  )
}
