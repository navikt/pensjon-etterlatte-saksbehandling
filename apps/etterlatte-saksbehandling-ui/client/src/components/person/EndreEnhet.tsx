import { Button, Heading, Modal, Select, Alert } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import { isPending } from '~shared/api/apiUtils'
import { byttEnhetPaaSak } from '~shared/api/sak'

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
    <OpprettRevurderingWrapper>
      <>
        <Button size="small" variant="secondary" onClick={() => setOpen(true)}>
          Endre Enhet
        </Button>
        <Modal open={open} onClose={closeAndReset} aria-labelledby="modal-heading">
          <Modal.Body>
            <Modal.Header closeButton={false}>
              <Heading spacing level="2" size="medium" id="modal-heading">
                Endre enhet
              </Heading>
            </Modal.Header>
            <WarningAlert>
              Hvis du endrer til en enhet du selv ikke har tilgang til, vil du ikke kunne flytte saken tilbake
            </WarningAlert>
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
            <ButtonGroup>
              <Button variant="secondary" onClick={closeAndReset}>
                Avbryt
              </Button>
              <Button loading={isPending(endreEnhetStatus)} onClick={endreEnhet}>
                Endre
              </Button>
            </ButtonGroup>
          </Modal.Body>
        </Modal>
      </>
    </OpprettRevurderingWrapper>
  )
}

const OpprettRevurderingWrapper = styled.div`
  margin-top: 0;
`

export const ENHETER = {
  VELGENHET: 'Velg Enhet',
  E4815: 'Ålesund - 4815',
  E4808: 'Porsgrunn - 4808',
  E4817: 'Steinkjer - 4817',
  E4862: 'Ålesund utland - 4862',
  E0001: 'Utland - 0001',
  E4883: 'Egne ansatte - 4883',
  E2103: 'Vikafossen - 2103',
}

export type EnhetFilterKeys = keyof typeof ENHETER

export function filtrerEnhet(enhetsFilter: EnhetFilterKeys): String {
  return enhetsFilter.substring(1)
}

const WarningAlert = styled(Alert).attrs({ variant: 'warning' })`
  margin-bottom: 1em;
`
