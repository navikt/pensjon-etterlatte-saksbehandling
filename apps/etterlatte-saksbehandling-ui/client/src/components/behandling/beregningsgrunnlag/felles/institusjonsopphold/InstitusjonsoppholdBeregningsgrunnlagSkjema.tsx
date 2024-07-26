import React from 'react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { InstitusjonsoppholdIBeregning, ReduksjonKey, ReduksjonOMS } from '~shared/types/Beregning'
import { Box, Button, HelpText, HStack, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'

interface Props {
  institusjonsopphold?: PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>
  paaAvbryt: () => void
  paaLagre: () => void
}

export const InstitusjonsoppholdBeregningsgrunnlagSkjema = ({ institusjonsopphold, paaAvbryt, paaLagre }: Props) => {
  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>>({
    defaultValues: institusjonsopphold
      ? institusjonsopphold
      : {
          fom: undefined,
          tom: undefined,
          data: {
            reduksjon: 'VELG_REDUKSJON',
            egenReduksjon: undefined,
            begrunnelse: '',
          },
        },
  })

  const validerReduksjon = (reduksjon: ReduksjonKey): string | undefined => {
    if (reduksjon === 'VELG_REDUKSJON') return 'Må settes'
    return undefined
  }

  const lagrePeriode = (institusjonsoppholdPeriode: PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>) => {
    console.log(institusjonsoppholdPeriode)
    paaLagre()
  }

  return (
    <form onSubmit={handleSubmit(lagrePeriode)}>
      <VStack gap="4">
        <HStack gap="4">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
          <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
          <Select
            {...register('data.reduksjon', { validate: validerReduksjon })}
            label="Reduksjon"
            error={errors.data?.reduksjon?.message}
          >
            {Object.entries(ReduksjonOMS).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </Select>
          {watch().data.reduksjon === 'JA_EGEN_PROSENT_AV_G' && (
            <TextField
              {...register('data.egenReduksjon')}
              label={
                <HStack gap="2">
                  Reduksjonsbeløp
                  <HelpText>
                    Oppgi den prosentsatsen av G som ytelsen skal reduseres med for å få riktig beløp i beregningen.
                  </HelpText>
                </HStack>
              }
            />
          )}
        </HStack>
        <Box maxWidth="15rem">
          <Textarea {...register('data.begrunnelse')} label="Begrunnelse (valgfritt)" />
        </Box>
        <HStack gap="4">
          <Button variant="secondary" type="button" size="small" icon={<XMarkIcon aria-hidden />} onClick={paaAvbryt}>
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
