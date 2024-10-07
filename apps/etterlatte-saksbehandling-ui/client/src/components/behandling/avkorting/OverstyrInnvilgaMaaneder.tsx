import { useState } from 'react'
import { Box, Button, HStack, TextField, VStack } from '@navikt/ds-react'
import { useFormContext } from 'react-hook-form'
import { IAvkortingGrunnlagLagre } from '~shared/types/IAvkorting'

export default function OverstyrInnvilgaMaander() {
  const [skalOverstyre, setSkalOverstyre] = useState(false)

  const {
    register,
    setValue,
    formState: { errors },
  } = useFormContext<IAvkortingGrunnlagLagre>() // retrieve all hook methods

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
            <HStack marginBlock="2" gap="2" align="start" wrap={false}>
              <TextField
                {...register('overstyrtInnvilgaMaaneder.antall', {
                  pattern: { value: /^\d+$/, message: 'Kun tall' },
                  required: { value: true, message: 'Må fylles ut' },
                })}
                label="Antall innvilga måneder"
                size="medium"
                inputMode="numeric"
                error={errors?.overstyrtInnvilgaMaaneder?.antall?.message}
              />
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
