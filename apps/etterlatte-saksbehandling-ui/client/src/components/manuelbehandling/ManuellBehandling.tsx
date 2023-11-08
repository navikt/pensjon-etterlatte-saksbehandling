import { Alert, Button, Select } from '@navikt/ds-react'
import React, { useState } from 'react'
import { SakType } from '~shared/types/sak'
import { DatoVelger } from '~shared/DatoVelger'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import styled from 'styled-components'
import { settBehandlingBehov } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'

export default function ManuellBehandling() {
  const dispatch = useAppDispatch()
  const { behandlingBehov } = useJournalfoeringOppgave()
  const [status, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [nyBehandlingId, setNyId] = useState('')

  const ferdigstill = () => {
    opprettNyBehandling(
      {
        ...behandlingBehov,
        sakType: SakType.BARNEPENSJON,
        mottattDato: behandlingBehov!!.mottattDato!!.replace('Z', ''),
      },
      (response) => {
        setNyId(response)
      }
    )
  }

  return (
    <FormWrapper>
      <h1>Manuell behandling</h1>

      <Select
        label="Hva skal språket/målform være?"
        value={behandlingBehov?.spraak || ''}
        onChange={(e) => dispatch(settBehandlingBehov({ ...behandlingBehov, spraak: e.target.value }))}
      >
        <option>Velg ...</option>
        <option value="nb">Bokmål</option>
        <option value="nn">Nynorsk</option>
        <option value="en">Engelsk</option>
      </Select>

      <DatoVelger
        label="Mottatt dato"
        description="Datoen søknaden ble mottatt"
        value={behandlingBehov?.mottattDato ? new Date(behandlingBehov?.mottattDato) : undefined}
        onChange={(mottattDato) =>
          dispatch(
            settBehandlingBehov({
              ...behandlingBehov,
              mottattDato: mottattDato?.toISOString(),
            })
          )
        }
      />

      <PersongalleriBarnepensjon />

      <Knapp>
        <Button variant="secondary" onClick={ferdigstill} loading={isPending(status)} disabled={isPending(status)}>
          Send inn
        </Button>
      </Knapp>
      {isSuccess(status) && <Alert variant="success">Behandling med id {nyBehandlingId} ble opprettet!</Alert>}
      {isFailure(status) && <Alert variant="error">Det oppsto en feil ved oppretting av behandlingen.</Alert>}
    </FormWrapper>
  )
}
const FormWrapper = styled.div`
  margin: 2em;
  width: 25em;
  display: grid;
  gap: var(--a-spacing-4);
`
const Knapp = styled.div`
  margin-top: 1em;
`
