import { BodyShort, Button, HStack, Modal, VStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'

import { OppgaveDTO } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'
import { omgjoerEtteroppgjoerRevurdering as omgjoerEtteroppgjoerRevurderingApi } from '~shared/api/etteroppgjoer'

type Props = {
  oppgave: OppgaveDTO
}

export const EtteroppgjoerOmgjoerRevurderingModal = ({ oppgave }: Props) => {
  const [open, setOpen] = useState(false)

  const navigate = useNavigate()

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const [omgjoerEtteroppgjoerResult, omgjoerEtteroppgjoerRevurderingRequest] = useApiCall(
    omgjoerEtteroppgjoerRevurderingApi
  )

  const omgjoerEoRevurdering = () => {
    omgjoerEtteroppgjoerRevurderingRequest({ behandlingId: oppgave.referanse!! }, (result) => {
      navigate(`/behandling/${result.id}`)
    })
  }

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Omgjør
      </Button>

      <Modal
        open={open}
        aria-labelledby="modal-heading"
        width="medium"
        onClose={() => setOpen(false)}
        header={{ heading: 'Omgjør behandling - revurdering' }}
      >
        <Modal.Body>
          <VStack gap="4">
            <BodyShort>Ønsker du å omgjøre denne behandlingen?</BodyShort>

            {mapResult(omgjoerEtteroppgjoerResult, {
              error: (error) => <ApiErrorAlert>Kunne ikke omgjøre behandling. {error.detail}</ApiErrorAlert>,
            })}

            <HStack gap="4" justify="end">
              {erTildeltSaksbehandler && (
                <HStack gap="4">
                  <Button size="small" onClick={() => setOpen(false)} variant="secondary">
                    Nei
                  </Button>
                  <Button size="small" onClick={omgjoerEoRevurdering} loading={isPending(omgjoerEtteroppgjoerResult)}>
                    Ja, omgjør
                  </Button>
                </HStack>
              )}
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
