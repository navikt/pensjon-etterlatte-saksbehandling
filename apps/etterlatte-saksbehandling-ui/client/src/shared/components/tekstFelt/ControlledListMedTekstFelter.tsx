import React, { ReactNode } from 'react'
import { ArrayPath, FieldValues, Path, useFieldArray, useFormContext } from 'react-hook-form'
import { Button, TextField } from '@navikt/ds-react'
import {
  InputList,
  InputRow,
  NyBehandlingSkjema,
} from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'

export const ControlledListMedTekstFelter = ({
  name,
  label,
  description,
  validate,
  addButtonLabel,
  maxLength,
}: {
  name: string
  label: string
  description?: string | ReactNode
  validate: (input: string) => string | undefined
  addButtonLabel: string
  maxLength?: number
}): ReactNode => {
  const {
    register,
    formState: { errors },
  } = useFormContext<NyBehandlingSkjema>()

  const { fields, append, remove } = useFieldArray({ name })

  console.log(errors)

  return (
    <InputList>
      {fields.map((field, index) => (
        <InputRow key={index}>
          <TextField
            {...register(`${name}.${index}.value`, { validate: validate })}
            key={field.id}
            label={label}
            description={description}
            error={errors?.persongalleri?.[name]?.[index].error.message}
          />
          <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => remove(index)} />
        </InputRow>
      ))}
      <Button
        icon={<PlusIcon />}
        onClick={() => append({ value: '' })}
        disabled={!!maxLength && fields.length >= maxLength}
        type="button"
      >
        {addButtonLabel}
      </Button>
    </InputList>
  )
}
