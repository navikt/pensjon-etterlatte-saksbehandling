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
import { useParams } from 'react-router-dom'
import { hentOppgave } from '~shared/api/oppgaver'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import { FormProvider, useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { formaterSpraak, mapRHFArrayToStringArray } from '~utils/formattering'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~shared/types/Enhet'

interface ManuellBehandingSkjema extends NyBehandlingSkjema {
  kilde: string
  pesysId: number | undefined
  enhet: EnhetFilterKeys
  gradering: string | undefined
  foreldreloes: boolean
  ufoere: boolean
  overstyrBeregning: boolean
  overstyrTrygdetid: boolean
}

export default function ManuellBehandling() {
  const [status, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [nyBehandlingId, setNyId] = useState('')
  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [overstyrTrygdetidStatus, opprettOverstyrtTrygdetidReq] = useApiCall(opprettTrygdetidOverstyrtMigrering)

  const [oppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const { '*': oppgaveId } = useParams()

  const [pesysId, setPesysId] = useState<number | undefined>(undefined)
  const [fnrFraOppgave, setFnr] = useState<string | undefined>(undefined)
  const [vedtaksloesning, setVedtaksloesning] = useState<string>('')

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

  const methods = useForm<ManuellBehandingSkjema>({
    defaultValues: {
      persongalleri: {
        innsender: undefined,
        soeker: fnrFraOppgave,
        gjenlevende: [],
        soesken: [],
        avdoed: [],
      },
      pesysId: pesysId,
      kilde: vedtaksloesning,
    },
  })

  const ferdigstill = (data: ManuellBehandingSkjema) => {
    opprettNyBehandling(
      {
        sakType: SakType.BARNEPENSJON,
        persongalleri: {
          ...data.persongalleri,
          gjenlevende: mapRHFArrayToStringArray(data.persongalleri.gjenlevende),
          avdoed: mapRHFArrayToStringArray(data.persongalleri.avdoed).filter((val) => val !== ''),
          soesken: mapRHFArrayToStringArray(data.persongalleri.soesken),
        },
        spraak: data.spraak!,
        mottattDato: new Date(data.mottattDato).toISOString().replace('Z', ''),
        kilde: data.kilde,
        pesysId: data.pesysId,
        enhet: data.enhet === 'VELGENHET' ? undefined : filtrerEnhet(data.enhet),
        foreldreloes: data.foreldreloes,
        ufoere: data.ufoere,
        gradering: data.gradering,
      },
      (nyBehandlingRespons) => {
        if (data.overstyrBeregning) {
          opprettOverstyrtBeregningReq({
            behandlingId: nyBehandlingRespons,
            beskrivelse: 'Manuell migrering',
          })
        }
        if (data.overstyrTrygdetid) {
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
      <FormProvider {...methods}>
        <h1>Manuell behandling</h1>

        <Select
          {...register('kilde', {
            required: { value: true, message: 'Du må spesifisere om det er en sak i fra Pesys' },
          })}
          label="Er det sak fra Pesys? (påkrevd)"
          error={errors.spraak?.message}
        >
          <option>Velg ...</option>
          <option value="PESYS">Løpende i Pesys til 1.1.2024</option>
          <option value="GJENOPPRETTA">Gjenoppretting av opphørt aldersovergang</option>
          <option value="GJENNY">Nei</option>
        </Select>

        <InputRow>
          <TextField
            {...register('pesysId')}
            label="Sakid Pesys"
            placeholder="Sakid Pesys"
            pattern="[0-9]{11}"
            maxLength={11}
          />
        </InputRow>

        <Select
          {...register('gradering', {
            required: { value: true, message: 'Du må spesifisere gradering' },
          })}
          label="Gradering - Adressebeskyttelse (påkrevd)"
          error={errors.gradering?.message}
        >
          <option value="">Velg ...</option>
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

        <Select {...register('enhet')} label="Overstyre enhet (valgfritt)">
          {Object.entries(ENHETER).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>

        <Checkbox {...register('overstyrBeregning')}>Skal bruke manuell beregning</Checkbox>

        <Checkbox {...register('overstyrTrygdetid')}>Skal bruke manuell trygdetid</Checkbox>

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

        <Checkbox {...register('foreldreloes')}>Er foreldreløs</Checkbox>

        <Checkbox {...register('ufoere')}>Søker har en sak for uføretrygd løpende eller under behandling.</Checkbox>

        <PersongalleriBarnepensjon erManuellMigrering />

        <Knapp>
          <Button
            variant="secondary"
            onClick={handleSubmit(ferdigstill)}
            loading={isPending(status) || isPending(overstyrBeregningStatus) || isPending(overstyrTrygdetidStatus)}
          >
            Opprett behandling
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
      </FormProvider>
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
