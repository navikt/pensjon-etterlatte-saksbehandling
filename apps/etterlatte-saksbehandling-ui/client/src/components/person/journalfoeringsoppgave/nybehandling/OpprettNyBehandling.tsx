import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype } from '~utils/formattering'
import { Button, Heading, Select, Tag } from '@navikt/ds-react'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import styled from 'styled-components'
import { gyldigPersongalleri } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { FlexRow } from '~shared/styled'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { DatoVelger } from '~shared/DatoVelger'
import React from 'react'
import { useAppDispatch } from '~store/Store'

export default function OpprettNyBehandling() {
  const { oppgave, nyBehandlingRequest } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  if (!oppgave) {
    return <Navigate to="../" relative="path" />
  }

  const { sakType } = oppgave

  const neste = () => navigate('oppsummering', { relative: 'path' })
  const tilbake = () => navigate('../', { relative: 'path' })

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett behandling{' '}
        <Tag variant="success" size="medium">
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>

      <Select
        label="Hva skal språket/målform være?"
        value={nyBehandlingRequest?.spraak || ''}
        onChange={(e) => dispatch(settNyBehandlingRequest({ ...nyBehandlingRequest, spraak: e.target.value }))}
      >
        <option>Velg ...</option>
        <option value="nb">Bokmål</option>
        <option value="nn">Nynorsk</option>
        <option value="en">Engelsk</option>
      </Select>

      <DatoVelger
        label="Mottatt dato"
        description="Datoen søknaden ble mottatt"
        value={nyBehandlingRequest?.mottattDato ? new Date(nyBehandlingRequest?.mottattDato) : undefined}
        onChange={(mottattDato) =>
          dispatch(
            settNyBehandlingRequest({
              ...nyBehandlingRequest,
              mottattDato: mottattDato?.toISOString(),
            })
          )
        }
      />

      <hr />

      <Heading size="medium" spacing>
        Persongalleri
      </Heading>

      {sakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad />}
      {sakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon />}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <Button
            variant="primary"
            onClick={neste}
            disabled={!gyldigPersongalleri(sakType, nyBehandlingRequest?.persongalleri)}
          >
            Neste
          </Button>
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}

export const InputList = styled.div`
  display: flex;
  flex-direction: column;
  align-items: start;
  gap: 1rem;
`

export const InputRow = styled.div`
  display: flex;
  align-items: flex-start;

  input {
    width: 20rem;
  }

  button {
    align-self: flex-end;
  }
`
