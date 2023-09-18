import { Button, Heading, Modal, Select } from '@navikt/ds-react'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { useState } from 'react'
import { opprettNyGenerellBehandling } from '~shared/api/generellbehandling'
import { GenerellBehandlingType, Innholdstyper } from '~shared/types/Generellbehandling'
import styled from 'styled-components'

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: center;
  gap: 1em;
  margin: 2rem 0rem;
`

const OpprettGenerellBehandling = (props: { sakId: number }) => {
  const { sakId } = props
  const [opprettGenBehandlingStatus, opprettGenBehandlingKall] = useApiCall(opprettNyGenerellBehandling)
  const [open, setOpen] = useState<boolean>(false)
  const [generellBehandlingType, setGenerellBehandlingType] = useState<GenerellBehandlingType | undefined>(undefined)
  const initialStateOrEmpty = generellBehandlingType === undefined

  const opprettGenBehandlingKallWrapper = () => {
    if (generellBehandlingType) {
      opprettGenBehandlingKall({ sakId, type: generellBehandlingType })
    }
  }
  return (
    <>
      <Button onClick={() => setOpen(!open)}>Lag generell behandling</Button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <Modal.Header closeButton={false}>
          <Heading spacing level="2" size="medium" id="modal-heading">
            Velg generell behandling type
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <Select
            label="Velg generell behandling type"
            value={generellBehandlingType}
            onChange={(e) => setGenerellBehandlingType(e.target.value as GenerellBehandlingType)}
          >
            <option value={''}>Velg behandlingstype</option>
            {Object.entries(Innholdstyper).map(([type, text]) => (
              <option key={type} value={type}>
                {text}
              </option>
            ))}
          </Select>
          <ButtonWrapper>
            <Button
              onClick={() => opprettGenBehandlingKallWrapper()}
              loading={isPending(opprettGenBehandlingStatus)}
              disabled={initialStateOrEmpty}
            >
              Opprett generell behandling
            </Button>
            <Button onClick={() => setOpen(false)}>Avbryt</Button>
          </ButtonWrapper>
        </Modal.Body>
      </Modal>
    </>
  )
}

export default OpprettGenerellBehandling
