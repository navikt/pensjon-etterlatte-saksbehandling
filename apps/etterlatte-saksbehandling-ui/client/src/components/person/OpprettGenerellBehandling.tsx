import { Alert, Button, Heading, Modal, Select } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { useState } from 'react'
import { opprettNyGenerellBehandling } from '~shared/api/generellbehandling'
import { GenerellBehandlingType, Innholdstyper } from '~shared/types/Generellbehandling'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexRow } from '~shared/styled'

const AlertWrapper = styled(Alert)`
  margin: 2rem 0rem;
`

type VELG = 'VELG'
type GenBehandlingTyperOgDefaultState = GenerellBehandlingType | VELG

function erGenerellBehandlingType(valg: GenBehandlingTyperOgDefaultState): valg is GenerellBehandlingType {
  return valg !== 'VELG'
}

const OpprettGenerellBehandling = (props: { sakId: number }) => {
  const { sakId } = props
  const [opprettGenBehandlingStatus, opprettGenBehandlingKall, resetkall] = useApiCall(opprettNyGenerellBehandling)
  const [open, setOpen] = useState<boolean>(false)
  const [generellBehandlingType, setGenerellBehandlingType] = useState<GenBehandlingTyperOgDefaultState>('VELG')
  const uValgt = generellBehandlingType === 'VELG'

  const opprettGenBehandlingKallWrapper = () => {
    if (erGenerellBehandlingType(generellBehandlingType)) {
      opprettGenBehandlingKall({ sakId, type: generellBehandlingType }, () => {})
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
            label=""
            value={generellBehandlingType}
            onChange={(e) => setGenerellBehandlingType(e.target.value as GenerellBehandlingType)}
          >
            <option value={'VELG'} disabled={true}>
              Velg behandlingstype
            </option>
            {Object.entries(Innholdstyper).map(([type, text]) => (
              <option key={type} value={type}>
                {text}
              </option>
            ))}
          </Select>
          {isFailure(opprettGenBehandlingStatus) && (
            <ApiErrorAlert>Kunne ikke opprette generell behandling</ApiErrorAlert>
          )}
          {isSuccess(opprettGenBehandlingStatus) && (
            <AlertWrapper variant="success">Behandlingen er opprettet</AlertWrapper>
          )}
          <FlexRow justify={'center'}>
            <Button
              onClick={() => opprettGenBehandlingKallWrapper()}
              loading={isPending(opprettGenBehandlingStatus)}
              disabled={uValgt}
            >
              Opprett generell behandling
            </Button>
            <Button
              onClick={() => {
                resetkall()
                setOpen(false)
                setGenerellBehandlingType('VELG')
              }}
            >
              Avbryt
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}

export default OpprettGenerellBehandling
