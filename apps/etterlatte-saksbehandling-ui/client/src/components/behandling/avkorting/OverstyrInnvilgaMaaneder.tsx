import React from 'react'
import { Box, Heading, HStack, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useFormContext } from 'react-hook-form'
import {
  hentLesbarTekstForInnvilgaMaanederType,
  IAvkortingGrunnlagLagre,
  OverstyrtInnvilaMaanederAarsak,
} from '~shared/types/IAvkorting'

export default function OverstyrInnvilgaMaander() {
  const {
    register,
    formState: { errors },
  } = useFormContext<IAvkortingGrunnlagLagre>()

  return (
    <HStack marginBlock="4" gap="1" align="start" wrap={false}>
      <VStack>
        <Box marginBlock="2">
          <Heading size="small">Overstyrt innvilga måneder</Heading>
        </Box>
        <HStack marginBlock="2" gap="4" align="start" wrap={false}>
          <Box width="13rem">
            <TextField
              {...register('overstyrtInnvilgaMaaneder.antall', {
                pattern: { value: /^\d+$/, message: 'Kun tall' },
                required: { value: true, message: 'Må fylles ut' },
              })}
              label="Antall måneder"
              size="medium"
              inputMode="numeric"
              error={errors?.overstyrtInnvilgaMaaneder?.antall?.message}
            />
          </Box>
          <Box width="19rem">
            <Select
              label="Årsak"
              {...register('overstyrtInnvilgaMaaneder.aarsak', {
                required: { value: true, message: 'Du må velge årsak' },
              })}
              error={errors?.overstyrtInnvilgaMaaneder?.aarsak?.message}
            >
              <option value="">Velg årsak</option>
              {Object.values(OverstyrtInnvilaMaanederAarsak).map((type) => (
                <option key={type} value={type}>
                  {hentLesbarTekstForInnvilgaMaanederType(type as OverstyrtInnvilaMaanederAarsak)}
                </option>
              ))}
            </Select>
          </Box>
        </HStack>
        <Box width="33rem">
          <Textarea
            {...register('overstyrtInnvilgaMaaneder.begrunnelse', {
              required: { value: true, message: 'Må fylles ut' },
            })}
            label="Begrunnelse"
            inputMode="text"
            error={errors?.overstyrtInnvilgaMaaneder?.begrunnelse?.message}
          />
        </Box>
      </VStack>
    </HStack>
  )
}
