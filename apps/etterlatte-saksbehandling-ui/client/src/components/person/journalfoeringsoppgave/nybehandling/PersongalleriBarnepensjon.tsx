import React from 'react'
import { BodyShort, Heading, Panel, TextField } from '@navikt/ds-react'
import { InputRow, NyBehandlingSkjema } from './OpprettNyBehandling'
import { Control, useFormContext } from 'react-hook-form'
import { ControlledTekstFelt } from '~shared/components/tekstFelt/ControlledTekstFelt'
import { ControlledListMedTekstFelter } from '~shared/components/tekstFelt/ControlledListMedTekstFelter'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'

export default function PersongalleriBarnepensjon({ erManuellMigrering = false }: { erManuellMigrering?: boolean }) {
  const { register } = useFormContext()

  return (
    <>
      <InputRow>
        <TextField
          {...register('persongalleri.soeker', { validate: validateFnrObligatorisk })}
          label="Søker (barnet)"
          description={
            erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'
          }
          readOnly={!erManuellMigrering}
        />
      </InputRow>

      <InputRow>
        <TextField
          {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
        />
      </InputRow>

      <Panel border>
        <Heading size="small" spacing>
          Gjenlevende forelder
          <BodyShort textColor="subtle">Legg til gjenlevende hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledListMedTekstFelter
          name="persongalleri.gjenlevende"
          label="Gjenlevende forelder"
          description="Oppgi fødselsnummer"
          validate={validateFnrObligatorisk}
          addButtonLabel="Legg til gjenlevende"
          maxLength={2}
        />
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Avdød forelder
          <BodyShort textColor="subtle">Legg til avdød hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledListMedTekstFelter
          name="persongalleri.avdoed"
          label="Avdød forelder"
          validate={validateFnrObligatorisk}
          addButtonLabel="Legg til avdød"
          maxLength={2}
        />
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Søsken
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledListMedTekstFelter
          name="persongalleri.soesken"
          label="Søsken"
          description="Oppgi fødselsnummer"
          validate={validateFnrObligatorisk}
          addButtonLabel="Legg til søsken"
        />
      </Panel>
    </>
  )
}
