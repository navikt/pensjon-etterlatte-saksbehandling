import { BodyShort, Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import {
  InputList,
  InputRow,
  NyBehandlingSkjema,
} from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import React from 'react'
import { useFieldArray, useFormContext } from 'react-hook-form'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { SpaceChildren } from '~shared/styled'

export default function PersongalleriOmstillingsstoenad() {
  const {
    register,
    formState: { errors },
  } = useFormContext<NyBehandlingSkjema>()

  const avdoedFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.avdoed' })
  const soeskenFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.soesken' })

  return (
    <SpaceChildren>
      <InputRow>
        <TextField
          {...register('persongalleri.soeker')}
          label="Søker (gjenlevende)"
          description="Fødselsnummeret er automatisk hentet fra oppgaven"
          readOnly
        />
      </InputRow>
      <InputRow>
        <TextField
          {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
          error={errors.persongalleri?.innsender?.message}
        />
      </InputRow>
      {avdoedFormArray.fields.map((field, index) => (
        <InputRow key={index}>
          <TextField
            {...register(`persongalleri.avdoed.${index}.value`, { validate: validerFnrValgfri })}
            key={field.id}
            label="Avdød"
            description="Oppgi fødselsnummer (dersom det er tilgjengelig)"
            error={errors?.persongalleri?.avdoed?.[index]?.value?.message}
          />
        </InputRow>
      ))}
      <Panel border>
        <Heading size="small" spacing>
          Barn
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <InputList>
          {soeskenFormArray.fields.map((field, index) => (
            <InputRow key={index}>
              <TextField
                {...register(`persongalleri.soesken.${index}.value`, { validate: validateFnrObligatorisk })}
                key={field.id}
                label="Barn"
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
            Legg til barn
          </Button>
        </InputList>
      </Panel>
    </SpaceChildren>
  )
}
