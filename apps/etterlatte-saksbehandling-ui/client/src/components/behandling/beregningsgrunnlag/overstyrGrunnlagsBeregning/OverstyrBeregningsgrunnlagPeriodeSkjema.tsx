import React from 'react'
import { BodyShort, Box, Button, HGrid, HStack, Label, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { OverstyrBeregningsperiode, OverstyrtAarsak } from '~shared/types/Beregning'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'

export const OverstyrBeregningsgrunnlagPeriodeSkjema = () => {
  const {
    register,
    control,
    formState: { errors },
  } = useForm<PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>>()

  return (
    <form>
      <HGrid gap="4" columns="min-content min-content max-content">
        <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
        <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
        <Box width="fit-content">
          <TextField {...register('data.utbetaltBeloep')} label="Utbetalt beløp" />
        </Box>
        <TextField {...register('data.trygdetid')} label="Anvendt trygdetid" />
        <TextField {...register('data.trygdetidForIdent')} label="Tilhørende fnr" />
        <div>
          <Label>Prorata brøk (valgfritt)</Label>
          <HStack gap="3" align="center">
            <TextField {...register('data.prorataBroekTeller')} label="" />
            <BodyShort>/</BodyShort>
            <TextField {...register('data.prorataBroekNevner')} label="" />
          </HStack>
        </div>
      </HGrid>
      <VStack gap="4" maxWidth="17rem">
        <Select
          {...register('data.aarsak', {
            required: { value: true, message: 'Må settes' },
            validate: { notDefault: (value) => value !== 'VELG_AARSAK' },
          })}
          label="Årsak"
          error={errors.data?.aarsak?.message}
        >
          {Object.entries(OverstyrtAarsak).map(([key, value]) => (
            <option key={key} value={key}>
              {value}
            </option>
          ))}
        </Select>
        <Textarea {...register('data.beskrivelse')} label="Beskrivelse (valgfritt)" minRows={3} autoComplete="off" />
        <HStack gap="4">
          <Button variant="secondary" type="button" size="small" icon={<XMarkIcon aria-hidden />}>
            Avbryt
          </Button>
          <Button size="small" icon={<FloppydiskIcon aria-hidden />}>
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
