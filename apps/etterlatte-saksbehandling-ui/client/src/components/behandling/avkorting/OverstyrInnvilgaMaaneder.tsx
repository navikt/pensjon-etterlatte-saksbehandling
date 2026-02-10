import React from 'react'
import { Box, Heading, HStack, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { UseFormRegister, UseFormStateReturn, UseFormWatch } from 'react-hook-form'
import {
  AvkortingFlereInntekter,
  hentLesbarTekstForInnvilgaMaanederType,
  OverstyrtInnvilgaMaanederAarsak,
  SystemOverstyrtInnvilgaMaanederAarsak,
} from '~shared/types/IAvkorting'

interface Props {
  register: UseFormRegister<AvkortingFlereInntekter>
  watch: UseFormWatch<AvkortingFlereInntekter>
  errors: UseFormStateReturn<AvkortingFlereInntekter>['errors']
  index: number
}

export default function OverstyrInnvilgaMaander({ register, watch, errors, index }: Props) {
  return (
    <HStack marginBlock="space-4" gap="space-1" align="start" wrap={false}>
      <VStack>
        <Box marginBlock="space-2">
          <Heading size="small">Overstyrt innvilga måneder</Heading>
        </Box>
        <HStack marginBlock="space-2" gap="space-4" align="start" wrap={false}>
          <Box width="13rem">
            <TextField
              {...register(`inntekter.${index}.overstyrtInnvilgaMaaneder.antall`, {
                pattern: { value: /^\d+$/, message: 'Kun tall' },
                required: { value: true, message: 'Må fylles ut' },
                min: {
                  value: 0,
                  message: 'Kan ikke ha negativt antall innvilgede måneder',
                },
                max: {
                  value: 12,
                  message: 'Kan maks ha 12 innvilgede måneder',
                },
              })}
              label="Antall måneder"
              size="medium"
              inputMode="numeric"
              error={errors?.inntekter?.[index]?.overstyrtInnvilgaMaaneder?.antall?.message}
            />
          </Box>
          <Box width="19rem">
            <Select
              label="Årsak"
              {...register(`inntekter.${index}.overstyrtInnvilgaMaaneder.aarsak`, {
                required: { value: true, message: 'Du må velge årsak' },
              })}
              error={errors?.inntekter?.[index]?.overstyrtInnvilgaMaaneder?.aarsak?.message}
            >
              <option value="">Velg årsak</option>
              {watch(`inntekter.${index}.overstyrtInnvilgaMaaneder.aarsak`) ===
                SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67 && (
                <option value={SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67} disabled={true}>
                  {hentLesbarTekstForInnvilgaMaanederType(SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67)} (satt av
                  Gjenny)
                </option>
              )}
              {Object.values(OverstyrtInnvilgaMaanederAarsak).map((type) => (
                <option key={type} value={type}>
                  {hentLesbarTekstForInnvilgaMaanederType(type as OverstyrtInnvilgaMaanederAarsak)}
                </option>
              ))}
            </Select>
          </Box>
        </HStack>
        <Box width="33rem">
          <Textarea
            {...register(`inntekter.${index}.overstyrtInnvilgaMaaneder.begrunnelse`, {
              required: { value: true, message: 'Må fylles ut' },
            })}
            label="Begrunnelse"
            inputMode="text"
            error={errors?.inntekter?.[index]?.overstyrtInnvilgaMaaneder?.begrunnelse?.message}
          />
        </Box>
      </VStack>
    </HStack>
  )
}
