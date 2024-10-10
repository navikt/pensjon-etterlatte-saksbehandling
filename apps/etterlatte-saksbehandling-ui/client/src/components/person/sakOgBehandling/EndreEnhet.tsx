import { Alert, Button, Heading, HStack, Modal, Select, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { byttEnhetPaaSak } from '~shared/api/sak'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~shared/types/Enhet'
import { PencilIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const EndreEnhet = ({ sakId }: { sakId: number }) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [enhetsFilter, setEnhetsfilter] = useState<EnhetFilterKeys>('VELGENHET')
  const [endreEnhetStatus, endreEnhetKall, resetApiCall] = useApiCall(byttEnhetPaaSak)
  const [enhetViByttetTil, setEnhetViByttetTil] = useState<string>('')
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const harTilgangPaaNyEnhet = innloggetSaksbehandler.enheter.includes(enhetViByttetTil)

  function endreEnhet() {
    if (enhetsFilter === 'VELGENHET') {
      return setError('Du må velge en enhet')
    }
    const enhet = filtrerEnhet(enhetsFilter)
    setEnhetViByttetTil(enhet)
    endreEnhetKall({ sakId: sakId, enhet })
  }

  const closeAndReset = () => {
    setOpen(false)
    resetApiCall()
  }

  return (
    <div>
      <Button
        size="small"
        variant="tertiary"
        onClick={() => setOpen(true)}
        icon={<PencilIcon aria-hidden />}
        iconPosition="right"
      >
        Endre enhet
      </Button>

      <Modal open={open} onClose={closeAndReset} aria-labelledby="modal-heading">
        <Modal.Header closeButton={false}>
          <Heading spacing level="2" size="medium" id="modal-heading">
            Endre enhet
          </Heading>
        </Modal.Header>

        <Modal.Body>
          {isSuccess(endreEnhetStatus) ? (
            <VStack gap="4">
              <Alert variant="success">Saken er flyttet til enhet {enhetViByttetTil}.</Alert>

              {!harTilgangPaaNyEnhet && (
                <Alert variant="warning">
                  Du har ikke lenger tilgang til saken, siden du ikke har tilgang til enheten saken er byttet til.
                </Alert>
              )}

              <HStack gap="2" justify="end">
                <Button variant={harTilgangPaaNyEnhet ? 'secondary' : 'primary'} as="a" href="/">
                  Gå til oppgavelisten
                </Button>
                {harTilgangPaaNyEnhet && (
                  <Button variant="primary" onClick={() => window.location.reload()}>
                    Last saken på nytt
                  </Button>
                )}
              </HStack>
            </VStack>
          ) : (
            <VStack gap="4">
              <Alert variant="warning">
                Hvis du endrer til en enhet du selv ikke har tilgang til, vil du ikke kunne flytte saken tilbake
              </Alert>

              {isFailure(endreEnhetStatus) && (
                <ApiErrorAlert>
                  Kunne ikke endre sakens enhet til {enhetsFilter}, på grunn av feil: {endreEnhetStatus.error.detail}
                </ApiErrorAlert>
              )}

              <Select
                label="Endre enhet"
                value={enhetsFilter}
                onChange={(e) => setEnhetsfilter(e.target.value as EnhetFilterKeys)}
                error={error}
              >
                {Object.entries(ENHETER).map(([status, statusbeskrivelse]) => (
                  <option key={status} value={status}>
                    {statusbeskrivelse}
                  </option>
                ))}
              </Select>

              {enhetsFilter !== 'VELGENHET' && !innloggetSaksbehandler.enheter.includes(filtrerEnhet(enhetsFilter)) && (
                <Alert variant="warning">
                  Du har ikke tilgang til enhet {enhetsFilter}, og vil ikke kunne se saken etter flytting.
                </Alert>
              )}

              <HStack gap="2" justify="end">
                <Button variant="secondary" onClick={closeAndReset}>
                  Avbryt
                </Button>
                <Button loading={isPending(endreEnhetStatus)} onClick={endreEnhet}>
                  Endre
                </Button>
              </HStack>
            </VStack>
          )}
        </Modal.Body>
      </Modal>
    </div>
  )
}
