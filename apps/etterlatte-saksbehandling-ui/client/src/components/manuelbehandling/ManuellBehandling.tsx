import { Alert, Box, Button, Checkbox, Heading, Select, TextField } from '@navikt/ds-react'
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
import { formaterDatoStrengTilLocaleDateTime, formaterSpraak, mapRHFArrayToStringArray } from '~utils/formattering'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~shared/types/Enhet'
import GjenopprettingModal from '~components/manuelbehandling/GjenopprettingModal'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { overstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

interface ManuellBehandingSkjema extends NyBehandlingSkjema {
  kilde: string
  pesysId: number | undefined
  enhet: EnhetFilterKeys
  foreldreloes: boolean
  ufoere: boolean
  overstyrBeregning: boolean
  overstyrTrygdetid: boolean
  kategori: overstyrtBeregningKategori
}

export default function ManuellBehandling() {
  useSidetittel('Manuell behandling')

  const [opprettBehandlingStatus, opprettNyBehandling] = useApiCall(opprettBehandling)
  const [nyBehandlingId, setNyId] = useState('')
  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [overstyrTrygdetidStatus, opprettOverstyrtTrygdetidReq] = useApiCall(opprettTrygdetidOverstyrtMigrering)

  const [hentOppgaveStatus, apiHentOppgave] = useApiCall(hentOppgave)
  const { '*': oppgaveId } = useParams()

  const [oppgaveStatus, setOppgaveStatus] = useState<Oppgavestatus | undefined>(undefined)

  useEffect(() => {
    if (oppgaveId) {
      apiHentOppgave(oppgaveId, (oppgave) => {
        setOppgaveStatus(oppgave.status)
        oppgave.fnr && methods.setValue('persongalleri.soeker', oppgave.fnr)
        oppgave.referanse && methods.setValue('pesysId', Number(oppgave.referanse))
        if (oppgave.type === Oppgavetype.GJENOPPRETTING_ALDERSOVERGANG) {
          methods.setValue('kilde', 'GJENOPPRETTA')
        }
      })
    }
  }, [oppgaveId])

  const methods = useForm<ManuellBehandingSkjema>({
    defaultValues: {
      persongalleri: {
        gjenlevende: [],
        soesken: [],
        avdoed: [],
      },
    },
  })

  const ferdigstill = (data: ManuellBehandingSkjema) => {
    opprettNyBehandling(
      {
        sakType: SakType.BARNEPENSJON,
        persongalleri: {
          soeker: data.persongalleri.soeker,
          innsender: data.persongalleri.innsender || null,
          gjenlevende: mapRHFArrayToStringArray(data.persongalleri.gjenlevende),
          avdoed: mapRHFArrayToStringArray(data.persongalleri.avdoed).filter((val) => val !== ''),
          soesken: mapRHFArrayToStringArray(data.persongalleri.soesken),
        },
        spraak: data.spraak!,
        mottattDato: formaterDatoStrengTilLocaleDateTime(data.mottattDato),
        kilde: data.kilde,
        pesysId: data.pesysId,
        enhet: data.enhet === 'VELGENHET' ? undefined : filtrerEnhet(data.enhet),
        foreldreloes: data.foreldreloes,
        ufoere: data.ufoere,
      },
      (nyBehandlingRespons) => {
        if (data.overstyrBeregning) {
          opprettOverstyrtBeregningReq({
            behandlingId: nyBehandlingRespons,
            beskrivelse: 'Manuell migrering',
            kategori: data.kategori,
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
    watch,
    formState: { errors },
  } = methods

  if (isPending(hentOppgaveStatus)) {
    return <div>Henter oppgave</div>
  }
  return (
    <FormWrapper>
      <FormProvider {...methods}>
        <Heading size="large">Manuell behandling</Heading>

        <Select
          {...register('kilde', {
            required: { value: true, message: 'Du må spesifisere om det er en sak i fra Pesys' },
          })}
          label="Er det sak fra Pesys? (påkrevd)"
          error={errors.kilde?.message}
        >
          <option>Velg ...</option>
          <option value="PESYS">Løpende i Pesys til 1.1.2024</option>
          <option value="GJENOPPRETTA">Gjenoppretting av opphørt aldersovergang</option>
          <option value="GJENNY">Nei</option>
        </Select>

        <InputRow>
          <TextField
            {...register('pesysId', {
              required: {
                value: ['GJENOPPRETTA', 'PESYS'].includes(methods.getValues().kilde),
                message: 'Du må legge til sakid i fra Pesys',
              },
            })}
            error={errors.pesysId?.message}
            label="Sakid Pesys"
            placeholder="Sakid Pesys"
            pattern="[0-9]{11}"
            maxLength={11}
          />
        </InputRow>

        <Select {...register('enhet')} label="Overstyre enhet (valgfritt)">
          {Object.entries(ENHETER).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>

        <Box paddingBlock="4 4">
          <Checkbox {...register('overstyrBeregning')}>Skal bruke manuell beregning</Checkbox>

          <Select
            label="Årsak overstyrt beregning:"
            {...register('kategori', {
              required: { value: true, message: 'Du må velge kategori' },
            })}
            disabled={!watch('overstyrBeregning')}
            error={errors.kategori?.message}
          >
            <option value="">Velg kategori</option>
            {Object.entries(overstyrtBeregningKategori).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </Select>
        </Box>

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
        />

        <Checkbox {...register('foreldreloes')}>Er foreldreløs</Checkbox>

        <Checkbox {...register('ufoere')}>Søker har en sak for uføretrygd løpende eller under behandling.</Checkbox>

        <PersongalleriBarnepensjon erManuellMigrering />

        <Knapp>
          <Button
            variant="secondary"
            onClick={handleSubmit(ferdigstill)}
            loading={
              isPending(opprettBehandlingStatus) ||
              isPending(overstyrBeregningStatus) ||
              isPending(overstyrTrygdetidStatus)
            }
          >
            Opprett behandling
          </Button>
        </Knapp>

        {oppgaveId && oppgaveStatus && isSuccess(hentOppgaveStatus) && (
          <Knapp>
            <GjenopprettingModal oppgaveId={oppgaveId} oppgaveStatus={oppgaveStatus} />
          </Knapp>
        )}

        {isSuccess(opprettBehandlingStatus) && (
          <Alert variant="success">Behandling med id {nyBehandlingId} ble opprettet!</Alert>
        )}
        {isFailureHandler({
          apiResult: opprettBehandlingStatus,
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
