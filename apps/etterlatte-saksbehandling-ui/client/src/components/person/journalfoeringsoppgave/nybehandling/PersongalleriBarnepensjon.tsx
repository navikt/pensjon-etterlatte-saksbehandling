import React from 'react'
import { BodyShort, Heading, Panel } from '@navikt/ds-react'
import { InputRow } from './OpprettNyBehandling'
import { Control } from 'react-hook-form'
import { ControlledTextField } from '~components/person/journalfoeringsoppgave/nybehandling/ControlledTextField'
import { ControlledInputArray } from '~components/person/journalfoeringsoppgave/nybehandling/ControlledInputArray'

export default function PersongalleriBarnepensjon({
  erManuellMigrering = false,
  control,
}: {
  erManuellMigrering?: boolean
  control: Control
}) {
  const validerFnrValgfri = (fnr: string): string | undefined => {
    if (fnr && !new RegExp(/[0-9]{11}/).test(fnr)) {
      return 'Fødselsnummer er på ugyldig format'
    }
    return undefined
  }

  const validateFnrObligatorisk = (fnr: string): string | undefined => {
    if (!fnr) {
      return 'Fødselsnummer må være satt'
    } else if (!new RegExp(/[0-9]{11}/).test(fnr)) {
      return 'Fødselsnummer er på ugyldig format'
    }
    return undefined
  }

  return (
    <>
      <InputRow>
        <ControlledTextField
          name="persongalleri.soeker"
          control={control}
          validate={validateFnrObligatorisk}
          label="Søker (barnet)"
          description={
            erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'
          }
          readOnly={!erManuellMigrering}
        />
      </InputRow>

      <InputRow>
        <ControlledTextField
          name="persongalleri.innsender"
          control={control}
          validate={validerFnrValgfri}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
        />
      </InputRow>

      <Panel border>
        <Heading size="small" spacing>
          Gjenlevende forelder
          <BodyShort textColor="subtle">Legg til gjenlevende hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledInputArray
          name="persongalleri.gjenlevende"
          label="Gjenlevende forelder"
          description="Oppgi fødselsnummer"
          control={control}
          validate={validerFnrValgfri}
          addButtonLabel="Legg til gjenlevende"
          maxLength={2}
        />
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Avdød forelder
          <BodyShort textColor="subtle">Legg til avdød hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledInputArray
          name="persongalleri.avdoed"
          label="Avdød forelder"
          control={control}
          validate={validerFnrValgfri}
          addButtonLabel="Legg til avdød"
          maxLength={2}
        />
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Søsken
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <ControlledInputArray
          name="persongalleri.soesken"
          label="Søsken"
          description="Oppgi fødselsnummer"
          control={control}
          validate={validerFnrValgfri}
          addButtonLabel="Legg til søsken"
        />
      </Panel>
    </>
  )
}
