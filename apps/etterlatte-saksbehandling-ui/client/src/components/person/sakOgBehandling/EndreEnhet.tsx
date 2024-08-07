import { Alert, Button, Heading, HStack, Modal, Select, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '~shared/api/apiUtils'
import { byttEnhetPaaSak } from '~shared/api/sak'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~shared/types/Enhet'
import { PencilIcon } from '@navikt/aksel-icons'

export const EndreEnhet = ({ sakId }: { sakId: number }) => {
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [enhetsFilter, setEnhetsfilter] = useState<EnhetFilterKeys>('VELGENHET')
  const [endreEnhetStatus, endreEnhetKall, resetApiCall] = useApiCall(byttEnhetPaaSak)

  function endreEnhet() {
    if (enhetsFilter === 'VELGENHET') {
      return setError('Du må velge en enhet')
    }
    endreEnhetKall({ sakId: sakId, enhet: filtrerEnhet(enhetsFilter) }, () => {
      closeAndReset()
      setTimeout(() => window.location.reload(), 200)
    })
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
          <VStack gap="4">
            <Alert variant="warning">
              Hvis du endrer til en enhet du selv ikke har tilgang til, vil du ikke kunne flytte saken tilbake
            </Alert>

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

            <HStack gap="2" justify="end">
              <Button variant="secondary" onClick={closeAndReset}>
                Avbryt
              </Button>
              <Button loading={isPending(endreEnhetStatus)} onClick={endreEnhet}>
                Endre
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </div>
  )
}
