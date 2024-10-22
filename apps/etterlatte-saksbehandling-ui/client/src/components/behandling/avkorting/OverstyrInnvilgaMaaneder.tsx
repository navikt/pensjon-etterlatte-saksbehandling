import React from 'react'
import { Box, Heading, HStack, Select, TextField, VStack } from '@navikt/ds-react'
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
            {Object.values(OverstyrtInnvilaMaanederAarsak).map((type) => (
              <option key={type} value={type}>
                {hentLesbarTekstForInnvilgaMaanederType(type as OverstyrtInnvilaMaanederAarsak)}
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
      </VStack>
    </HStack>
  )
}
