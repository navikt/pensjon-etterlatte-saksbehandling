import { Control, Controller, FieldValues, Path } from 'react-hook-form'
import { TextField } from '@navikt/ds-react'

interface Props<T extends FieldValues> {
  name: Path<T>
  control: Control<T>
  label?: string
  description?: string
}

export const ControlledInntektTextField = <T extends FieldValues>({ name, control, label, description }: Props<T>) => {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field: { value, onChange } }) => (
        <TextField
          label={label}
          description={description}
          value={value}
          // Fjerne alt som ikke er tall og gjøre om til Number
          onChange={(e) => onChange(new Intl.NumberFormat('nb').format(Number(e.target.value.replace(/[^0-9.]/g, ''))))}
          inputMode="numeric"
        />
      )}
    />
  )
}
