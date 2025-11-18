import { updateEtteroppgjoerForbehandling, useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillEtteroppgjoerForbehandlingUtenBrev } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { Alert, BodyLong, Button, Modal, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { kanRedigereEtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'

export function FerdigstillEtteroppgjoerForbehandlingUtenBrev() {
  const { forbehandling } = useEtteroppgjoerForbehandling()
  const [modalOpen, setModalOpen] = useState(false)
  const [
    ferdigstillEtteroppgjoerForbehandlingResult,
    ferdigstillEtteroppgjoerForbehandlingRequest,
    resetFerdigstillEtteroppgjoerForbehandling,
  ] = useApiCall(ferdigstillEtteroppgjoerForbehandlingUtenBrev)
  const dispatch = useAppDispatch()
  const redigerbar = forbehandling && kanRedigereEtteroppgjoerForbehandling(forbehandling.status)

  function avbryt() {
    setModalOpen(false)
    resetFerdigstillEtteroppgjoerForbehandling()
  }

  function ferdigstillForbehandling() {
    ferdigstillEtteroppgjoerForbehandlingRequest({ forbehandlingId: forbehandling.id }, (etteroppgjoer) => {
      dispatch(updateEtteroppgjoerForbehandling(etteroppgjoer))
    })
  }

  return (
    <>
      <div>
        {redigerbar ? (
          <Button onClick={() => setModalOpen(true)}>Ferdigstill etteroppgjør</Button>
        ) : (
          <PersonButtonLink fnr={forbehandling?.sak?.ident}>Tilbake til saksoversikten</PersonButtonLink>
        )}
      </div>
      <Modal open={modalOpen} onClose={avbryt} header={{ heading: 'Ferdigstill etteroppgjør uten brev' }}>
        <Modal.Body>
          <VStack gap="4">
            <BodyLong>
              Siden etteroppgjøret viser ingen endring og bruker ikke hadde utbetaling i etteroppgjørsåret skal
              etteroppgjøret ferdigstilles uten brev.
            </BodyLong>
            {mapResult(ferdigstillEtteroppgjoerForbehandlingResult, {
              success: () => <Alert variant="success">Etteroppgjøret er ferdigstilt uten varselbrev.</Alert>,
              error: (error) => (
                <Alert variant="error">
                  Kunne ikke ferdigstille etteroppgjøret uten brev, på grunn av feil: {error.detail}
                </Alert>
              ),
            })}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          {isSuccess(ferdigstillEtteroppgjoerForbehandlingResult) ? (
            <PersonButtonLink fnr={forbehandling?.sak?.ident}>Tilbake til saksoversikten</PersonButtonLink>
          ) : (
            <Button onClick={ferdigstillForbehandling} loading={isPending(ferdigstillEtteroppgjoerForbehandlingResult)}>
              Ferdigstill
            </Button>
          )}
          <Button
            onClick={avbryt}
            variant="secondary"
            disabled={isPending(ferdigstillEtteroppgjoerForbehandlingResult)}
          >
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
