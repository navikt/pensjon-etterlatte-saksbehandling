import { BodyShort, Button, HStack, Modal, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'
import { omgjoerEtteroppgjoerRevurdering as omgjoerEtteroppgjoerRevurderingApi } from '~shared/api/etteroppgjoer'

type Props = {
  behandlingId: string
}

export const EtteroppgjoerOmgjoerRevurderingModal = ({ behandlingId }: Props) => {
  const [open, setOpen] = useState(false)

  const navigate = useNavigate()
  const [omgjoerEtteroppgjoerResult, omgjoerEtteroppgjoerRevurderingRequest] = useApiCall(
    omgjoerEtteroppgjoerRevurderingApi
  )

  const omgjoerEoRevurdering = () => {
    omgjoerEtteroppgjoerRevurderingRequest({ behandlingId: behandlingId }, (result) => {
      navigate(`/behandling/${result.id}`)
    })
  }

  return (
    <>
      <Button variant="secondary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Omgjør
      </Button>

      <Modal
        open={open}
        aria-labelledby="modal-heading"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Omgjør revurdering - etteroppgjør' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <BodyShort>
              I tilfeller hvor revurdering for etteroppgjøret er avbrutt ved en feil, kan du omgjøre revurderingen uten å
              måtte behandle etteroppgjøret på nytt via ny forbehandling.
            </BodyShort>

            {mapResult(omgjoerEtteroppgjoerResult, {
              error: (error) => <ApiErrorAlert>Kunne ikke omgjøre behandling. {error.detail}</ApiErrorAlert>,
            })}

            <HStack gap="4" justify="end">
              <HStack gap="4">
                <Button size="small" onClick={() => setOpen(false)} variant="secondary">
                  Nei
                </Button>
                <Button size="small" onClick={omgjoerEoRevurdering} loading={isPending(omgjoerEtteroppgjoerResult)}>
                  Ja, omgjør
                </Button>
              </HStack>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
