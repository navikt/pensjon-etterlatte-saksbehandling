import React, { useState } from 'react'
import { Box, Button, Heading, HStack, Select, TextField, VStack } from '@navikt/ds-react'
import { useFormContext } from 'react-hook-form'
import {
  IAvkortingGrunnlagLagre,
  innvilgaMaanederType,
  IOverstyrtInnvilaMaanederAarsak,
} from '~shared/types/IAvkorting'

export default function OverstyrInnvilgaMaander() {
  const [skalOverstyre, setSkalOverstyre] = useState(false)

  const {
    register,
    setValue,
    formState: { errors },
  } = useFormContext<IAvkortingGrunnlagLagre>()

  const toggle = () => {
    if (skalOverstyre) {
      setValue('overstyrtInnvilgaMaaneder', undefined)
    }
    setSkalOverstyre(!skalOverstyre)
  }

  return (
    <HStack marginBlock="4" gap="1" align="start" wrap={false}>
      {skalOverstyre ? (
        <>
          <VStack>
            <Heading size="xsmall">Overstyrt innvilga måneder</Heading>
            <HStack marginBlock="2" gap="2" align="start" wrap={false}>
              <TextField
                {...register('overstyrtInnvilgaMaaneder.antall', {
                  pattern: { value: /^\d+$/, message: 'Kun tall' },
                  required: { value: true, message: 'Må fylles ut' },
                })}
                label="Antall"
                size="medium"
                inputMode="numeric"
                error={errors?.overstyrtInnvilgaMaaneder?.antall?.message}
              />
              <Select
                label="Årsak"
                {...register('overstyrtInnvilgaMaaneder.aarsak', {
                  required: { value: true, message: 'Du må velge årsak' },
                })}
                error={errors?.overstyrtInnvilgaMaaneder?.aarsak?.message}
              >
                <option value="">Velg årsak</option>
                {Object.values(IOverstyrtInnvilaMaanederAarsak).map((type) => (
                  <option key={type} value={type}>
                    {innvilgaMaanederType(type as IOverstyrtInnvilaMaanederAarsak)}
                  </option>
                ))}
              </Select>
              <Box width="17.5rem">
                <TextField
                  {...register('overstyrtInnvilgaMaaneder.begrunnelse', {
                    required: { value: true, message: 'Må fylles ut' },
                  })}
                  label="Begrunnelse"
                  size="medium"
                  inputMode="text"
                  error={errors?.overstyrtInnvilgaMaaneder?.begrunnelse?.message}
                />
              </Box>
            </HStack>
            <HStack>
              <Button size="small" variant="secondary" onClick={toggle}>
                Fjern overstyrt innvilga måneder
              </Button>
            </HStack>
          </VStack>
        </>
      ) : (
        <Button size="small" variant="danger" onClick={toggle}>
          Overstyr innvilga måneder
        </Button>
      )}
    </HStack>
  )
}
