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
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { useAppDispatch } from '~store/Store'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { formatDateToLocalDateTimeOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'

interface PersonISkjema {
  value: string
}

export interface NyBehandlingSkjema {
  spraak: Spraak | null
  mottattDato: string
  persongalleri: {
    soeker: string
    innsender?: string
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

  const methods = useForm<NyBehandlingSkjema>({
    defaultValues: {
      persongalleri: { soeker: nyBehandlingRequest?.persongalleri?.soeker },
    },
  })
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = methods

  const onSubmit = (data: NyBehandlingSkjema) => {
    // TODO gjøre switch på om det er barnepensjon eller omstillingstønad
    // const gjenlevende: string[] = (data.persongalleri?.gjenlevende as unknown as PersonISkjema[]).map(
    //   (val: PersonISkjema) => val.value
    // )
    // const avdoed: string[] = (data.persongalleri?.avdoed as unknown as PersonISkjema[]).map(
    //   (val: PersonISkjema) => val.value
    // )
    // const soesken: string[] = (data.persongalleri?.soesken as unknown as PersonISkjema[]).map(
    //   (val: PersonISkjema) => val.value
    // )

    // dispatch(
    //   settNyBehandlingRequest({
    //     ...data,
    //     mottattDato: formatDateToLocalDateTimeOrEmptyString(new Date(data.mottattDato!)),
    //     persongalleri: {
    //       ...data.persongalleri,
    //       gjenlevende,
    //       avdoed,
    //       soesken,
    //     },
    //   })
    // )

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
          />

          <hr />

          <Heading size="medium" spacing>
            Persongalleri
          </Heading>

          {sakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad control={control} />}
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
