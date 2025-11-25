import { updateEtteroppgjoerForbehandling, useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillEtteroppgjoerForbehandlingUtenBrev } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { Alert, BodyLong, Button, Modal, VStack } from '@navikt/ds-react'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { kanRedigereEtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { JaNei } from '~shared/types/ISvar'

export function FerdigstillEtteroppgjoerForbehandlingUtenBrev() {
  const { forbehandling } = useEtteroppgjoerForbehandling()
  const [modalOpen, setModalOpen] = useState(false)
  const [ferdigstillForbehandlingUtenBrevResult, ferdigstillForbehandlingUtenBrevRequest, resetForbehandlingUtenBrev] =
    useApiCall(ferdigstillEtteroppgjoerForbehandlingUtenBrev)

  const dispatch = useAppDispatch()
  const erRedigerbar = forbehandling && kanRedigereEtteroppgjoerForbehandling(forbehandling.status)

  function avbryt() {
    setModalOpen(false)
    resetForbehandlingUtenBrev()
  }

  function ferdigstillForbehandling() {
    ferdigstillForbehandlingUtenBrevRequest({ forbehandlingId: forbehandling.id }, (etteroppgjoer) => {
      dispatch(updateEtteroppgjoerForbehandling(etteroppgjoer))
    })
  }

  const doedsfallIEtteroppgjoersaaret = forbehandling.opphoerSkyldesDoedsfallIEtteroppgjoersaar === JaNei.JA

  return (
    <>
      <div>
        {erRedigerbar ? (
          <Button onClick={() => setModalOpen(true)}>Ferdigstill etteroppgjør</Button>
        ) : (
          <PersonButtonLink fnr={forbehandling?.sak?.ident}>Tilbake til saksoversikten</PersonButtonLink>
        )}
      </div>
      <Modal open={modalOpen} onClose={avbryt} header={{ heading: 'Ferdigstill etteroppgjør uten brev' }}>
        <Modal.Body>
          <VStack gap="4">
            {doedsfallIEtteroppgjoersaaret ? (
              <BodyLong>
                Siden opphøret gjelder dødsfall i etteroppgjørsåret skal forbehandlingen ferdigstilles uten brev.
              </BodyLong>
            ) : (
              <BodyLong>
                Siden etteroppgjøret viser ingen endring og bruker ikke hadde utbetaling i etteroppgjørsåret skal
                etteroppgjøret ferdigstilles uten brev.
              </BodyLong>
            )}
            {mapResult(ferdigstillForbehandlingUtenBrevResult, {
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
          {isSuccess(ferdigstillForbehandlingUtenBrevResult) ? (
            <PersonButtonLink fnr={forbehandling?.sak?.ident}>Tilbake til saksoversikten</PersonButtonLink>
          ) : (
            <Button onClick={ferdigstillForbehandling} loading={isPending(ferdigstillForbehandlingUtenBrevResult)}>
              Ferdigstill
            </Button>
          )}
          <Button onClick={avbryt} variant="secondary" disabled={isPending(ferdigstillForbehandlingUtenBrevResult)}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
