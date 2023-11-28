import { Alert, Button, Checkbox, Select, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { SakType } from '~shared/types/sak'
import { DatoVelger } from '~shared/DatoVelger'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import styled from 'styled-components'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'
import { opprettOverstyrBeregning } from '~shared/api/beregning'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { Spraak } from '~shared/types/Brev'
import { opprettTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'

export default function ManuellBehandling() {
  const dispatch = useAppDispatch()
  const { nyBehandlingRequest } = useJournalfoeringOppgave()
  const [status, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [nyBehandlingId, setNyId] = useState('')
  const [erMigrering, setErMigrering] = useState<boolean | null>(null)

  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [overstyrBeregning, setOverstyrBeregning] = useState<boolean>(false)

  const [overstyrTrygdetidStatus, opprettOverstyrtTrygdetidReq] = useApiCall(opprettTrygdetidOverstyrtMigrering)
  const [overstyrTrygdetid, setOverstyrTrygdetid] = useState<boolean>(false)

  const [pesysId, setPesysId] = useState<number | undefined>(undefined)

  const ferdigstill = () => {
    opprettNyBehandling(
      {
        ...nyBehandlingRequest,
        sakType: SakType.BARNEPENSJON,
        mottattDato: nyBehandlingRequest!!.mottattDato!!.replace('Z', ''),
        kilde: erMigrering ? 'PESYS' : undefined,
        pesysId: pesysId,
      },
      (nyBehandlingRespons) => {
        if (overstyrBeregning) {
          opprettOverstyrtBeregningReq({
            behandlingId: nyBehandlingRespons,
            beskrivelse: 'Manuell migrering',
          })
        }
        if (overstyrTrygdetid) {
          opprettOverstyrtTrygdetidReq({ behandlingId: nyBehandlingRespons })
        }
        setNyId(nyBehandlingRespons)
      }
    )
  }

  return (
    <FormWrapper>
      <h1>Manuell behandling</h1>

      <Select
        label="Er det migrering fra Pesys?"
        value={erMigrering == null ? '' : erMigrering ? 'ja' : 'nei'}
        onChange={(e) => {
          const svar = e.target.value
          if (svar === 'ja') setErMigrering(true)
          else if (svar === 'nei') setErMigrering(false)
          else setErMigrering(null)
        }}
      >
        <option>Velg ...</option>
        <option value="ja">Ja</option>
        <option value="nei">Nei</option>
      </Select>

      <InputRow>
        <TextField
          label="Sakid Pesys"
          placeholder="Sakid Pesys"
          value={pesysId || ''}
          pattern="[0-9]{11}"
          maxLength={11}
          onChange={(e) => setPesysId(Number(e.target.value))}
        />
      </InputRow>

      <Checkbox checked={overstyrBeregning} onChange={() => setOverstyrBeregning(!overstyrBeregning)}>
        Skal bruke manuell beregning
      </Checkbox>

      <Checkbox checked={overstyrTrygdetid} onChange={() => setOverstyrTrygdetid(!overstyrTrygdetid)}>
        Skal bruke manuell trygdetid
      </Checkbox>

      <Select
        label="Hva skal språket/målform være?"
        value={nyBehandlingRequest?.spraak || ''}
        onChange={(e) =>
          dispatch(settNyBehandlingRequest({ ...nyBehandlingRequest, spraak: e.target.value as Spraak }))
        }
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

      <PersongalleriBarnepensjon />

      <Knapp>
        <Button
          variant="secondary"
          onClick={ferdigstill}
          loading={isPending(status) || isPending(overstyrBeregningStatus) || isPending(overstyrTrygdetidStatus)}
          disabled={erMigrering == null || (erMigrering && pesysId == null)}
        >
          Send inn
        </Button>
      </Knapp>
      {isSuccess(status) && <Alert variant="success">Behandling med id {nyBehandlingId} ble opprettet!</Alert>}
      {isFailure(status) && <Alert variant="error">Det oppsto en feil ved oppretting av behandlingen.</Alert>}

      {isPending(overstyrBeregningStatus) && <Alert variant="info">Oppretter overstyrt beregning.</Alert>}
      {isSuccess(overstyrBeregningStatus) && <Alert variant="success">Overstyrt beregning opprettet!</Alert>}
      {isFailure(overstyrBeregningStatus) && <Alert variant="error">Klarte ikke å overstyre beregning.</Alert>}

      {isPending(overstyrTrygdetidStatus) && <Alert variant="info">Oppretter overstyrt trygdetid.</Alert>}
      {isSuccess(overstyrTrygdetidStatus) && <Alert variant="success">Overstyrt trygdetid opprettet!</Alert>}
      {isFailure(overstyrTrygdetidStatus) && <Alert variant="error">Klarte ikke å overstyre trygdetid.</Alert>}
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
