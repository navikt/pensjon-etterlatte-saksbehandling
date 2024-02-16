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
import { useForm } from 'react-hook-form'
import { NyBehandlingRequest } from '~shared/types/IDetaljertBehandling'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

export default function OpprettNyBehandling() {
  const { oppgave, nyBehandlingRequest } = useJournalfoeringOppgave()
  const navigate = useNavigate()

  if (!oppgave) {
    return <Navigate to="../" relative="path" />
  }

  const { sakType } = oppgave

  const neste = () => navigate('oppsummering', { relative: 'path' })
  const tilbake = () => navigate('../', { relative: 'path' })

  const { register, control } = useForm<NyBehandlingRequest>({
    defaultValues: nyBehandlingRequest,
  })

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett behandling{' '}
        <Tag variant="success" size="medium">
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>

      <Select
        {...register('spraak', { required: { value: true, message: 'Du må velge språk/målform for behandlingen' } })}
        label="Hva skal språket/målform være?"
      >
        <option>Velg ...</option>
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
      {sakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon control={control} />}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <Button variant="primary" type="submit" onClick={neste}>
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
