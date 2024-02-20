import { BodyShort, Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { Persongalleri } from '~shared/types/Person'
import {
  InputList,
  InputRow,
  NyBehandlingSkjema,
} from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import React from 'react'
import { ControlledTekstFelt } from '~shared/components/tekstFelt/ControlledTekstFelt'
import { Control } from 'react-hook-form'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { ControlledListMedTekstFelter } from '~shared/components/tekstFelt/ControlledListMedTekstFelter'

export default function PersongalleriOmstillingsstoenad({ control }: { control: Control<NyBehandlingSkjema> }) {
  return (
    <>
      <InputRow>
        <ControlledTekstFelt
          name="persongalleri.soeker"
          control={control}
          label="Søker (gjenlevende)"
          description="Fødselsnummeret er automatisk hentet fra oppgaven"
          validate={validateFnrObligatorisk}
          readOnly
        />
      </InputRow>
      <InputRow>
        <ControlledTekstFelt
          name="persongalleri.innsender"
          control={control}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
          validate={validerFnrValgfri}
        />
      </InputRow>
      {/* TODO: huske å konvertere denne til array */}
      <InputRow>
        <ControlledTekstFelt
          name="persongalleri.avdoed"
          control={control}
          label="Avdød"
          description="Oppgi fødselsnummer (dersom det er tilgjengelig)"
          validate={validerFnrValgfri}
        />
      </InputRow>
      <Panel border>
        <Heading size="small" spacing>
          Barn
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledListMedTekstFelter
          name="persongalleri.soesken"
          label="Barn"
          control={control}
          validate={validateFnrObligatorisk}
          addButtonLabel="Legg til barn"
        />
      </Panel>
    </>
  )
}
