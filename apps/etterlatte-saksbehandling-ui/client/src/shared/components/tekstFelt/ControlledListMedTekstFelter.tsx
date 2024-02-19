import React, { ReactNode } from 'react'
import { Control, useFieldArray } from 'react-hook-form'
import { Button } from '@navikt/ds-react'
import { InputList, InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { ControlledTekstFelt } from '~components/person/journalfoeringsoppgave/nybehandling/ControlledTekstFelt'

export const ControlledListMedTekstFelter = ({
  name,
  label,
  description,
  control,
  validate,
  addButtonLabel,
  maxLength,
}: {
  name: string
  label: string
  description?: string | ReactNode
  control: Control
  validate: (input: string) => string | undefined
  addButtonLabel: string
  maxLength?: number
}): ReactNode => {
  const { fields, append, remove } = useFieldArray({ name, control })

  return (
    <InputList>
      {fields.map((field, index) => (
        <InputRow key={index}>
          <ControlledTekstFelt
            key={field.id}
            name={`${name}.${index}.value`}
            control={control}
            label={label}
            description={description}
            validate={validate}
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
