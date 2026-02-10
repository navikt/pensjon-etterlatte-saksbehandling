import React, { useState } from 'react'
import { BodyLong, Button, Heading, HStack, Modal } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGrunnlag } from '~shared/api/behandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'

export default function OppdaterGrunnlagModal({
  behandlingId,
  behandlingStatus,
  enhetId,
}: {
  behandlingId: string
  behandlingStatus: IBehandlingStatus
  enhetId: string
}) {
  const [isOpen, setIsOpen] = useState(false)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const behandles = behandlingErRedigerbar(behandlingStatus, enhetId, innloggetSaksbehandler.skriveEnheter)
  const [oppdatert, apiOppdaterGrunnlag] = useApiCall(oppdaterGrunnlag)

  if (!behandles) return

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
    <>
      <Button
        variant="secondary"
        size="small"
        icon={<ArrowsCirclepathIcon aria-hidden />}
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
            eventuell verge ikke er registrert når behandlingen ble opprettet). Status på behandlingen vil da bli satt
            til &quot;Opprettet&quot;. Hvis du har startet behandlingen allerede, må du gå gjennom stegene på nytt.
          </BodyLong>

          <HStack gap="space-4" justify="center">
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
          </HStack>
          {isFailureHandler({ apiResult: oppdatert, errorMessage: 'Oppdatering feilet' })}
        </Modal.Body>
      </Modal>
    </>
  )
}
