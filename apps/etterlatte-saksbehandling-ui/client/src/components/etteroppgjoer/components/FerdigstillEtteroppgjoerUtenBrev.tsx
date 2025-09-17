import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillEtteroppgjoerForbehandlingUtenBrev } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { Alert, BodyLong, Button, Modal, VStack } from '@navikt/ds-react'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { kanRedigereEtteroppgjoerBehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'

export function FerdigstillEtteroppgjoerUtenBrev() {
  const { behandling } = useEtteroppgjoer()
  const [modalOpen, setModalOpen] = useState(false)
  const [resultFerdigstillEtteroppgjoer, fetchFerdigstillEtteroppgjoer, resetFerdigstillEtteroppgjoer] = useApiCall(
    ferdigstillEtteroppgjoerForbehandlingUtenBrev
  )
  const dispatch = useAppDispatch()
  const redigerbar = kanRedigereEtteroppgjoerBehandling(behandling.status)

  function avbryt() {
    setModalOpen(false)
    resetFerdigstillEtteroppgjoer()
  }

  function ferdigstillEtteroppgjoer() {
    fetchFerdigstillEtteroppgjoer({ forbehandlingId: behandling.id }, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
      avbryt()
    })
  }

  return (
    <>
      <div>
        {redigerbar ? (
          <Button onClick={() => setModalOpen(true)}>Ferdigstill etteroppgjør</Button>
        ) : (
          <PersonButtonLink fnr={behandling.sak.ident}>Tilbake til saksoversikten</PersonButtonLink>
        )}
      </div>
      <Modal open={modalOpen} onClose={avbryt} header={{ heading: 'Ferdigstill etteroppgjør uten brev' }}>
        <Modal.Body>
          <VStack gap="4">
            <BodyLong>
              Siden etteroppgjøret viser ingen endring og bruker ikke hadde utbetaling i etteroppgjørsåret skal
              etteroppgjøret ferdigstilles uten brev.
            </BodyLong>
            {mapFailure(resultFerdigstillEtteroppgjoer, (error) => (
              <Alert variant="error">
                Kunne ikke ferdigstille etteroppgjøret uten brev, på grunn av feil: {error.detail}
              </Alert>
            ))}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={ferdigstillEtteroppgjoer} loading={isPending(resultFerdigstillEtteroppgjoer)}>
            Ferdigstill
          </Button>
          <Button onClick={avbryt} variant="secondary" disabled={isPending(resultFerdigstillEtteroppgjoer)}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
