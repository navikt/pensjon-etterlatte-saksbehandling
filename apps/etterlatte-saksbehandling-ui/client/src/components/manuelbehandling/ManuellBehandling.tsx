import { Alert, Button, Checkbox, Select, TextField } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { SakType } from '~shared/types/sak'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBehandling } from '~shared/api/behandling'
import { opprettOverstyrBeregning } from '~shared/api/beregning'
import {
  InputRow,
  NyBehandlingSkjema,
} from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { Spraak } from '~shared/types/Brev'
import { opprettTrygdetidOverstyrtMigrering } from '~shared/api/trygdetid'
import { isPending, isSuccess, mapAllApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~components/person/EndreEnhet'
import { useParams } from 'react-router-dom'
import { hentOppgave } from '~shared/api/oppgaver'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import { useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { formaterSpraak } from '~utils/formattering'

interface ManuellBehandingSkjema extends NyBehandlingSkjema {
  kilde: string
  pesysId: number | undefined
  enhet: String
  foreldreloes: boolean
  ufoere: boolean
  gradering: boolean
}

export default function ManuellBehandling() {
  const [oppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const { '*': oppgaveId } = useParams()

  const [status, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [nyBehandlingId, setNyId] = useState('')
  const [vedtaksloesning, setVedtaksloesning] = useState<string>('')

  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [overstyrBeregning, setOverstyrBeregning] = useState<boolean>(false)

  const [overstyrTrygdetidStatus, opprettOverstyrtTrygdetidReq] = useApiCall(opprettTrygdetidOverstyrtMigrering)
  const [overstyrTrygdetid, setOverstyrTrygdetid] = useState<boolean>(false)

  const [pesysId, setPesysId] = useState<number | undefined>(undefined)
  const [fnrFraOppgave, setFnr] = useState<string | undefined>(undefined)

  useEffect(() => {
    if (oppgaveId) {
      apiHentOppgave(oppgaveId, (oppgave) => {
        oppgave.fnr && setFnr(oppgave.fnr)
        oppgave.referanse && setPesysId(Number(oppgave.referanse))
        if (oppgave.type == 'GJENOPPRETTING_ALDERSOVERGANG') {
          setVedtaksloesning('GJENOPPRETTA')
        }
      })
    }
  }, [oppgaveId])

  const [enhet, setEnhet] = useState<EnhetFilterKeys>('VELGENHET')

  const [erForeldreloes, setErForeldreloes] = useState<boolean>(false)
  const [erUfoere, setErUfoere] = useState<boolean>(false)

  const [gradering, setGradering] = useState<string>('')
  const [error, setError] = useState<boolean>(false)

  // TODO Felles util?
  const mapRHFArrayToStringArray = (rhfArray?: Array<{ value: string }>): string[] => {
    return !!rhfArray ? rhfArray.map((val) => val.value) : []
  }

  const methods = useForm<ManuellBehandingSkjema>({
    defaultValues: {
      persongalleri: {
        innsender: undefined,
        soeker: fnrFraOppgave,
        gjenlevende: [],
        soesken: [],
        avdoed: [],
      },
    },
  })

  const ferdigstill = (data: ManuellBehandingSkjema) => {
    // TODO Bedre validering med react hook form?
    if (!gradering || !vedtaksloesning || data.mottattDato) {
      setError(true)
      return
    }
    // TODO kan bruke mer unpacking her?
    opprettNyBehandling(
      {
        persongalleri: {
          ...data.persongalleri,
          gjenlevende: mapRHFArrayToStringArray(data.persongalleri.gjenlevende),
          avdoed: mapRHFArrayToStringArray(data.persongalleri.avdoed).filter((val) => val !== ''),
          soesken: mapRHFArrayToStringArray(data.persongalleri.soesken),
        },
        spraak: data.spraak!, // TODO ?
        mottattDato: new Date(data.mottattDato).toISOString(),
        sakType: SakType.BARNEPENSJON,
        kilde: vedtaksloesning,
        pesysId: pesysId,
        enhet: enhet === 'VELGENHET' ? undefined : filtrerEnhet(enhet), // TODO ?
        foreldreloes: erForeldreloes,
        ufoere: erUfoere,
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

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    getValues,
  } = methods

  if (isPending(oppgaveStatus)) {
    return <div>Henter oppgave</div>
  }
  return (
    <FormWrapper>
      <h1>Manuell behandling</h1>

      <Select
        label="Er det sak fra Pesys? (påkrevd)"
        value={vedtaksloesning ?? ''}
        onChange={(e) => setVedtaksloesning(e.target.value)}
      >
        <option>Velg ...</option>
        <option value="PESYS">Løpende i Pesys til 1.1.2024</option>
        <option value="GJENOPPRETTA">Gjenoppretting av opphørt aldersovergang</option>
        <option value="GJENNY">Nei</option>
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
      <Select
        label="Gradering - Adressebeskyttelse (påkrevd)"
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
        {...register('spraak', {
          required: { value: true, message: 'Du må velge språk/målform for behandlingen' },
        })}
        label="Hva skal språket/målform være?"
        error={errors.spraak?.message}
      >
        <option value="">Velg ...</option>
        <option value={Spraak.NB}>{formaterSpraak(Spraak.NB)}</option>
        <option value={Spraak.NN}>{formaterSpraak(Spraak.NN)}</option>
        <option value={Spraak.EN}>{formaterSpraak(Spraak.EN)}</option>
      </Select>

      <ControlledDatoVelger
        name="mottattDato"
        label="Mottatt dato"
        description="Datoen søknaden ble mottatt"
        control={control}
        errorVedTomInput="Du må legge inn datoen søknaden ble mottatt"
        defaultValue={getValues().mottattDato}
      />

      <Checkbox checked={erForeldreloes} onChange={() => setErForeldreloes(!erForeldreloes)}>
        Er foreldreløs
      </Checkbox>

      <Checkbox checked={erUfoere} onChange={() => setErUfoere(!erUfoere)}>
        Søker har en sak for uføretrygd løpende eller under behandling.
      </Checkbox>

      <PersongalleriBarnepensjon />

      <Knapp>
        <Button
          variant="secondary"
          onClick={handleSubmit(ferdigstill)}
          loading={isPending(status) || isPending(overstyrBeregningStatus) || isPending(overstyrTrygdetidStatus)}
        >
          Opprett behandling
        </Button>
      </Knapp>
      {error && <Alert variant="error">Alle påkrevde felter er ikke fylt ut</Alert>}
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
