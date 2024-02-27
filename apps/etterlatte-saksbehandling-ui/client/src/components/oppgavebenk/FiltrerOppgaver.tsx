import React, { ReactNode } from 'react'
import { FlexRow, SpaceChildren } from '~shared/styled'
import { Button, Checkbox, CheckboxGroup, Heading, Select, TextField, UNSAFE_Combobox } from '@navikt/ds-react'
import styled from 'styled-components'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { useForm } from 'react-hook-form'
import { TrashIcon } from '@navikt/aksel-icons'

export const FiltrerOppgaver = (): ReactNode => {
  const { register, control } = useForm()

  return (
    <FiltreWrapper>
      <BottomRightBorderBox>
        <FlexRow justify="space-between" align="center">
          <Heading size="medium">Filter</Heading>
          <Button variant="tertiary" icon={<TrashIcon aria-hidden />}>
            Nullstill filtre
          </Button>
        </FlexRow>
      </BottomRightBorderBox>
      <BottomRightBorderBox>
        <Checkbox>Velg mine oppgaver</Checkbox>
      </BottomRightBorderBox>
      <SpaceChildren>
        <BottomRightBorderBox>
          <SpaceChildren>
            <TextField label="Føselsnummer" />
            <FlexRow>
              <ControlledDatoVelger name="fristFra" label="Frist fra" control={control} errorVedTomInput="" />
              <ControlledDatoVelger name="fristTil" label="Frist til" control={control} errorVedTomInput="" />
            </FlexRow>
            <UNSAFE_Combobox label="Tildeling" options={['EY', 'PESYS', 'Lars Monsen']} />
            <Select label="Enhet">
              <option>Vis alle</option>
              <option>4815-Ålesund</option>
              <option>0001-Utland</option>
            </Select>
            <Select label="Oppgavetype">
              <option>Vis alle</option>
              <option>Førstegangsbehandling</option>
              <option>Revurdering</option>
            </Select>
            <CheckboxGroup legend="Ytelse">
              <Checkbox>Barnepensjon</Checkbox>
              <Checkbox>Omstillingsstønad</Checkbox>
            </CheckboxGroup>
            <CheckboxGroup legend="Oppgavestatus">
              <Checkbox>Ny</Checkbox>
              <Checkbox>Under behandling</Checkbox>
              <Checkbox>På vent</Checkbox>
              <Checkbox>Ferdigstilt</Checkbox>
              <Checkbox>Feilregistrert</Checkbox>
              <Checkbox>Avbrutt</Checkbox>
            </CheckboxGroup>
          </SpaceChildren>
        </BottomRightBorderBox>
      </SpaceChildren>
    </FiltreWrapper>
  )
}

const FiltreWrapper = styled.div`
  width: fit-content;
`

const BottomRightBorderBox = styled.div`
  border-color: lightgray;
  border-right-style: solid;
  border-bottom-style: solid;
  border-bottom-width: thin;
  border-right-width: thin;
  padding: 1rem;
`
