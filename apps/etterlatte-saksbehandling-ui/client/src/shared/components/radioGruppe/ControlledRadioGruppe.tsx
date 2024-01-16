import React, { ReactNode } from 'react'
import { Control, Controller, FieldValues, Path } from 'react-hook-form'
import { RadioGroup } from '@navikt/ds-react'

interface Props<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  errorVedTomInput: string
  legend: ReactNode | string
  radios: ReactNode | Array<ReactNode>
}

export const ControlledRadioGruppe = <T extends FieldValues>({
  name,
  control,
  radios,
  errorVedTomInput,
  legend,
}: Props<T>): ReactNode => {
  return (
    <Controller
      name={name}
      rules={{
        required: {
          value: true,
          message: errorVedTomInput,
        },
      }}
      control={control}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <RadioGroup onChange={onChange} value={value} error={error?.message} legend={legend}>
          {radios}
        </RadioGroup>
      )}
    />
  )
}
