import React, { ReactNode } from 'react'
import { Control, Controller, FieldValues, Path } from 'react-hook-form'
import { RadioGroup } from '@navikt/ds-react'

interface Props<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  errorVedTomInput?: string
  legend: ReactNode | string
  hideLegend?: boolean
  radios: ReactNode | Array<ReactNode>
  size?: 'medium' | 'small'
  readOnly?: boolean
  shouldUnregister?: boolean
}

export const ControlledRadioGruppe = <T extends FieldValues>({
  name,
  control,
  radios,
  errorVedTomInput,
  legend,
  hideLegend,
  size,
  readOnly,
  shouldUnregister = false,
}: Props<T>): ReactNode => {
  return (
    <Controller
      name={name}
      rules={{
        required: errorVedTomInput
          ? {
              value: true,
              message: errorVedTomInput,
            }
          : undefined,
      }}
      control={control}
      render={({ field: { onChange, value }, fieldState: { error } }) => (
        <RadioGroup
          onChange={onChange}
          value={value}
          error={error?.message}
          legend={legend}
          hideLegend={hideLegend}
          size={size}
          readOnly={readOnly}
        >
          {radios}
        </RadioGroup>
      )}
      shouldUnregister={shouldUnregister}
    />
  )
}
