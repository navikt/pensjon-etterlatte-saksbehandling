import React from 'react'
import { BodyShort, Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { InputList, InputRow, NyBehandlingSkjema } from './OpprettNyBehandling'
import { useFieldArray, useFormContext } from 'react-hook-form'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { SpaceChildren } from '~shared/styled'

export default function PersongalleriBarnepensjon({ erManuellMigrering = false }: { erManuellMigrering?: boolean }) {
  const {
    register,
    formState: { errors },
  } = useFormContext<NyBehandlingSkjema>()

  const gjenlevendeFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.gjenlevende' })
  const avdoedFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.avdoed' })
  const soeskenFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.soesken' })

  const kanLeggeTil: boolean = (gjenlevendeFormArray.fields.length || 0) + (avdoedFormArray.fields.length || 0) < 2

  return (
    <SpaceChildren>
      <TextField
        {...register('persongalleri.soeker', { validate: validateFnrObligatorisk })}
        label="Søker (barnet)"
        description={
          erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'
        }
        readOnly={!erManuellMigrering}
      />

      <InputRow>
        <TextField
          {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
          error={errors.persongalleri?.innsender?.message}
        />
      </InputRow>

      <Panel border>
        <Heading size="small" spacing>
          Gjenlevende forelder
          <BodyShort textColor="subtle">Legg til gjenlevende hvis tilgjengelig</BodyShort>
        </Heading>

        <InputList>
          {gjenlevendeFormArray.fields.map((field, index) => (
            <InputRow key={index}>
              <TextField
                {...register(`persongalleri.gjenlevende.${index}.value`, { validate: validateFnrObligatorisk })}
                key={field.id}
                label="Gjenlevende forelder"
                description="Oppgi fødselsnummer"
                error={errors?.persongalleri?.gjenlevende?.[index]?.value?.message}
              />
              <Button
                icon={<XMarkIcon aria-hidden />}
                variant="tertiary"
                onClick={() => gjenlevendeFormArray.remove(index)}
              />
            </InputRow>
          ))}
          <Button
            icon={<PlusIcon aria-hidden />}
            onClick={() => gjenlevendeFormArray.append({ value: '' })}
            disabled={!kanLeggeTil}
            type="button"
          >
            Legg til gjenlevende
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Avdød forelder
          <BodyShort textColor="subtle">Legg til avdød hvis tilgjengelig</BodyShort>
        </Heading>

        <InputList>
          {avdoedFormArray.fields.map((field, index) => (
            <InputRow key={index}>
              <TextField
                {...register(`persongalleri.avdoed.${index}.value`, { validate: validateFnrObligatorisk })}
                key={field.id}
                label="Avdød forelder"
                description="Oppgi fødselsnummer"
                error={errors?.persongalleri?.avdoed?.[index]?.value?.message}
              />
              <Button
                icon={<XMarkIcon aria-hidden />}
                variant="tertiary"
                onClick={() => avdoedFormArray.remove(index)}
              />
            </InputRow>
          ))}
          <Button
            icon={<PlusIcon aria-hidden />}
            onClick={() => avdoedFormArray.append({ value: '' })}
            disabled={!kanLeggeTil}
            type="button"
          >
            Legg til avdød
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Søsken
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>
        <InputList>
          {soeskenFormArray.fields.map((field, index) => (
            <InputRow key={index}>
              <TextField
                {...register(`persongalleri.soesken.${index}.value`, { validate: validateFnrObligatorisk })}
                key={field.id}
                label="Søsken"
                description="Oppgi fødselsnummer"
                error={errors?.persongalleri?.soesken?.[index]?.value?.message}
              />
              <Button
                icon={<XMarkIcon aria-hidden />}
                variant="tertiary"
                onClick={() => soeskenFormArray.remove(index)}
              />
            </InputRow>
          ))}
          <Button icon={<PlusIcon aria-hidden />} onClick={() => soeskenFormArray.append({ value: '' })} type="button">
            Legg til søsken
          </Button>
        </InputList>
      </Panel>
    </SpaceChildren>
  )
}
