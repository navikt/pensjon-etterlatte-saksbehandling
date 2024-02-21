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
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'

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
  const navigate = useNavigate()

  if (!oppgave) {
    return <Navigate to="../" relative="path" />
  }

  const { sakType } = oppgave

  const tilbake = () => navigate('../', { relative: 'path' })

  const methods = useForm<NyBehandlingSkjema>({
    defaultValues: {
      persongalleri: {
        soeker: nyBehandlingRequest?.persongalleri?.soeker,
        avdoed: sakType === SakType.OMSTILLINGSSTOENAD ? [{ value: '' }] : [],
      },
    },
  })
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = methods

  const onSubmit = (data: NyBehandlingSkjema) => {
    const state: NyBehandlingRequest = {
      sakType,
      spraak: data.spraak!,
      mottattDato: new Date(data.mottattDato).toISOString(),
      persongalleri: {
        ...data.persongalleri,
        gjenlevende: data.persongalleri.gjenlevende?.map((val) => val.value),
        avdoed: data.persongalleri.avdoed?.map((val) => val.value).filter((val) => val !== ''),
        soesken: data.persongalleri.soesken?.map((val) => val.value),
      },
    }

    navigate('oppsummering', { relative: 'path', state })
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
