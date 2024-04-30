import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype, formaterSpraak, mapRHFArrayToStringArray } from '~utils/formattering'
import { Button, Heading, Select, Tag } from '@navikt/ds-react'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import styled from 'styled-components'
import { FlexRow, SpaceChildren } from '~shared/styled'
import React from 'react'
import { Spraak } from '~shared/types/Brev'
import { FormProvider, useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'

export interface NyBehandlingSkjema {
  spraak: Spraak | null
  mottattDato: string
  persongalleri: {
    soeker: string
    innsender: string | null
    gjenlevende?: Array<{ value: string }>
    avdoed?: Array<{ value: string }>
    soesken?: Array<{ value: string }>
  }
}

export default function OpprettNyBehandling() {
  const { oppgave, nyBehandlingRequest } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  if (!oppgave || !erOppgaveRedigerbar(oppgave.status)) {
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

  const konverterAvdoedForOmstillingstoenad = (avdoed?: string[]): Array<{ value: string }> => {
    if (avdoed && avdoed[0]) {
      return [{ value: avdoed[0] }]
    } else {
      return [{ value: '' }]
    }
  }

  const methods = useForm<NyBehandlingSkjema>({
    defaultValues: {
      ...nyBehandlingRequest,
      persongalleri: {
        innsender: nyBehandlingRequest?.persongalleri?.innsender || null,
        soeker: nyBehandlingRequest?.persongalleri?.soeker,
        gjenlevende: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.gjenlevende),
        soesken: mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.soesken),
        avdoed:
          sakType === SakType.OMSTILLINGSSTOENAD
            ? konverterAvdoedForOmstillingstoenad(nyBehandlingRequest?.persongalleri?.avdoed)
            : mapStringArrayToRHFArray(nyBehandlingRequest?.persongalleri?.avdoed),
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
      <FormProvider {...methods}>
        <SpaceChildren>
          <Heading size="medium" spacing>
            Opprett behandling{' '}
            <Tag variant="success" size="medium">
              {formaterSakstype(sakType)}
            </Tag>
          </Heading>

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

          <PersongalleriHeading size="medium" spacing>
            Persongalleri
          </PersongalleriHeading>

          {sakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad />}
          {sakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon />}

          <div>
            <FlexRow justify="center" $spacing>
              <Button variant="secondary" onClick={tilbake} type="button">
                Tilbake
              </Button>

              <Button variant="primary" type="submit" onClick={handleSubmit(onSubmit)}>
                Neste
              </Button>
            </FlexRow>
            <FlexRow justify="center">
              <AvbrytBehandleJournalfoeringOppgave />
            </FlexRow>
          </div>
        </SpaceChildren>
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

const PersongalleriHeading = styled(Heading)`
  margin-top: 1.5rem;
`
