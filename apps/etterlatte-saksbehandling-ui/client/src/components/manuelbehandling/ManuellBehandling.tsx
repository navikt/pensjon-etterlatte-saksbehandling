import { Alert, Button, Checkbox, Select, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import { SakType } from '~shared/types/sak'
import { UnControlledDatoVelger } from '~shared/components/datoVelger/UnControlledDatoVelger'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import styled from 'styled-components'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'
import { opprettOverstyrBeregning } from '~shared/api/beregning'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { Spraak } from '~shared/types/Brev'
import { opprettTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'

import { isPending, isSuccess, mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~components/person/EndreEnhet'

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

  const [enhet, setEnhet] = useState<EnhetFilterKeys>('VELGENHET')

  const [erForeldreloes, setErForeldreloes] = useState<boolean>(false)

  const [gradering, setGradering] = useState<string>('')
  const [error, setError] = useState<boolean>(false)

  const ferdigstill = () => {
    if (!gradering) {
      setError(true)
      return
    }
    opprettNyBehandling(
      {
        ...nyBehandlingRequest,
        sakType: SakType.BARNEPENSJON,
        mottattDato: nyBehandlingRequest!!.mottattDato!!.replace('Z', ''),
        kilde: erMigrering ? 'PESYS' : undefined,
        pesysId: pesysId,
        enhet: enhet === 'VELGENHET' ? undefined : filtrerEnhet(enhet),
        foreldreloes: erForeldreloes,
        gradering: gradering,
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
      {error && <Alert variant="error">Gradering må velges</Alert>}
      <Select
        label="Gradering - Adressebeskyttelse(obligatorisk)"
        value={gradering}
        onChange={(e) => {
          setGradering(e.target.value)
          setError(false)
        }}
      >
        <option>Velg ...</option>

        <option key="STRENGT_FORTROLIG" value="STRENGT_FORTROLIG">
          Strengt fortrolig
        </option>
        <option key="STRENGT_FORTROLIG_UTLAND" value="STRENGT_FORTROLIG_UTLAND">
          Strengt fortrolig utland
        </option>
        <option key="FORTROLIG" value="fortrolig">
          Fortrolig
        </option>
        <option key="UGRADERT" value="UGRADERT">
          Ugradert
        </option>
      </Select>
      <Select
        label="Overstyre enhet (valgfritt)"
        value={enhet}
        onChange={(e) => setEnhet(e.target.value as EnhetFilterKeys)}
      >
        {Object.entries(ENHETER).map(([status, statusbeskrivelse]) => (
          <option key={status} value={status}>
            {statusbeskrivelse}
          </option>
        ))}
      </Select>

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

      <UnControlledDatoVelger
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

      <Checkbox checked={erForeldreloes} onChange={() => setErForeldreloes(!erForeldreloes)}>
        Er foreldreløs
      </Checkbox>
      <PersongalleriBarnepensjon erManuellMigrering={true} />

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
      {isFailureHandler({
        apiResult: status,
        errorMessage: 'Det oppsto en feil ved oppretting av behandlingen.',
      })}

      {mapAllApiResult(
        overstyrBeregningStatus,
        <Alert variant="info">Oppretter overstyrt beregning.</Alert>,
        null,
        () => (
          <ApiErrorAlert>Klarte ikke å overstyre beregning.</ApiErrorAlert>
        ),
        () => (
          <Alert variant="success">Overstyrt beregning opprettet!</Alert>
        )
      )}
      {mapAllApiResult(
        overstyrTrygdetidStatus,
        <Alert variant="info">Oppretter overstyrt trygdetid.</Alert>,
        null,
        () => (
          <ApiErrorAlert>Klarte ikke å overstyre trygdetid.</ApiErrorAlert>
        ),
        () => (
          <Alert variant="success">Overstyrt trygdetid opprettet!</Alert>
        )
      )}
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
