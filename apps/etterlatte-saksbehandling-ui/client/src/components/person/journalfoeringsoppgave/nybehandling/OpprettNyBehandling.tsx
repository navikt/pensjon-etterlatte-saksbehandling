import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype, formaterSpraak } from '~utils/formattering'
import { Button, Heading, Select, Tag } from '@navikt/ds-react'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import React from 'react'
import { Spraak } from '~shared/types/Brev'
import { FormProvider, useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'

export interface NyBehandlingSkjema {
  spraak: Spraak | null
  mottattDato: string
  persongalleri: {
    soeker: string
    innsender: string
    gjenlevende?: Array<{ value: string }>
    avdoed?: Array<{ value: string }>
    soesken?: Array<{ value: string }>
  }
}

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

  const mapStringArrayToRHFArray = (stringArray?: string[]): Array<{ value: string }> => {
    return !!stringArray
      ? stringArray.map((value) => {
          return { value }
        })
      : []
  }

  const mapRHFArrayToStringArray = (rhfArray?: Array<{ value: string }>): string[] => {
    return !!rhfArray ? rhfArray.map((val) => val.value) : []
  }

  const methods = useForm<NyBehandlingSkjema>({
    defaultValues: {
      ...nyBehandlingRequest,
      persongalleri: {
        innsender: nyBehandlingRequest?.persongalleri?.innsender,
        soeker: nyBehandlingRequest?.persongalleri?.soeker,
        gjenlevende: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.gjenlevende),
        soesken: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.soesken),
        avdoed:
          sakType === SakType.OMSTILLINGSSTOENAD
            ? [{ value: '' }]
            : mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.avdoed),
      },
    },
  })
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    getValues,
  } = methods

  const onSubmit = (data: NyBehandlingSkjema) => {
    dispatch(
      settNyBehandlingRequest({
        sakType,
        spraak: data.spraak!,
        mottattDato: new Date(data.mottattDato).toISOString(),
        persongalleri: {
          ...data.persongalleri,
          gjenlevende: mapRHFArrayToStringArray(data.persongalleri.gjenlevende),
          avdoed: mapRHFArrayToStringArray(data.persongalleri.avdoed).filter((val) => val !== ''),
          soesken: mapRHFArrayToStringArray(data.persongalleri.soesken),
        },
      })
    )

    neste()
  }

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett behandling{' '}
        <Tag variant="success" size="medium">
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>
      <FormProvider {...methods}>
        <form onSubmit={handleSubmit(onSubmit)}>
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

          <hr />

          <Heading size="medium" spacing>
            Persongalleri
          </Heading>

          {sakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad />}
          {sakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon />}

          <div>
            <FlexRow justify="center" $spacing>
              <Button variant="secondary" onClick={tilbake} type="button">
                Tilbake
              </Button>

              <Button variant="primary" type="submit">
                Neste
              </Button>
            </FlexRow>
            <FlexRow justify="center">
              <AvbrytBehandleJournalfoeringOppgave />
            </FlexRow>
          </div>
        </form>
      </FormProvider>
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
