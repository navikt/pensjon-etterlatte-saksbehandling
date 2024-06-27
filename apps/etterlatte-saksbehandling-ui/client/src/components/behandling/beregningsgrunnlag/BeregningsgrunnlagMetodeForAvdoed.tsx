import { BodyShort, Box, Button, Heading, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import React from 'react'
import { BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { formaterDato } from '~utils/formatering/dato'
import { lastDayOfMonth, startOfDay } from 'date-fns'
import { SubmitHandler, useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

const beskrivelseFor = (metode: BeregningsMetode | null) => {
  switch (metode) {
    case BeregningsMetode.BEST:
      return 'Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter nasjonale regler)'
    case BeregningsMetode.NASJONAL:
      return 'Nasjonal beregning (folketrygdberegning)'
    case BeregningsMetode.PRORATA:
      return 'Prorata (EØS/avtale-land, der rettighet er oppfylt ved sammenlegging)'
    default:
      return ''
  }
}

type BeregningsgrunnlagMetodeForAvdoedOppsummeringProps = {
  beregningsMetode: BeregningsMetode | null
  fom: Date | undefined
  tom: Date | undefined
  begrunnelse: string
  navn: string
  visNavn: boolean
}

export const BeregningsgrunnlagMetodeForAvdoedOppsummering = (
  props: BeregningsgrunnlagMetodeForAvdoedOppsummeringProps
) => {
  const { beregningsMetode, fom, tom, begrunnelse, navn, visNavn } = props

  return (
    <>
      {visNavn && (
        <VStack gap="4">
          <Heading size="small" level="3">
            Trygdetid brukt i beregningen for {navn}
          </Heading>
        </VStack>
      )}

      <BodyShort>{beskrivelseFor(beregningsMetode)}</BodyShort>

      <BodyShort>
        Gyldig for beregning {fom ? formaterDato(fom) : ''} - {tom ? formaterDato(lastDayOfMonth(tom)) : ''}
      </BodyShort>

      {begrunnelse && begrunnelse.length > 0 && (
        <>
          <Heading size="small" level="3">
            Begrunnelse
          </Heading>

          <BodyShort>{begrunnelse}</BodyShort>
        </>
      )}
    </>
  )
}

type BeregningsgrunnlagMetodeForAvdoedProps = {
  ident: string
  navn: string
  grunnlag: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | null
  onUpdate: (data: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => void
}

const BeregningsgrunnlagMetodeForAvdoed = (props: BeregningsgrunnlagMetodeForAvdoedProps) => {
  const { ident, grunnlag, onUpdate, navn } = props

  const { register, control, handleSubmit, getValues, watch } = useForm<
    PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  >({
    defaultValues: {
      data: {
        beregningsMetode: {
          beregningsMetode: grunnlag?.data.beregningsMetode.beregningsMetode ?? null,
          begrunnelse: grunnlag?.data.beregningsMetode.begrunnelse,
        },
        avdoed: ident,
      },
      fom: grunnlag?.fom,
      tom: grunnlag?.tom,
    },
  })

  const lagre: SubmitHandler<PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>> = (
    data: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  ) => {
    if (data.fom && data.data.beregningsMetode.beregningsMetode) {
      onUpdate(data)
    }
  }

  const validerDatoer = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(value))
    const tom = getValues().tom ? startOfDay(new Date(getValues().tom!)) : undefined

    if (tom && fom > tom) {
      return 'Fra-måned kan ikke være etter til-måned.'
    }

    return undefined
  }

  return (
    <form onSubmit={handleSubmit(lagre)}>
      <Box paddingBlock="8">
        <VStack gap="4">
          <ControlledRadioGruppe
            name="data.beregningsMetode.beregningsMetode"
            control={control}
            errorVedTomInput="Du må velge beregningsmetode"
            legend={`Trygdetid brukt i beregningen for ${navn}`}
            radios={
              <>
                <Radio value={BeregningsMetode.NASJONAL}>{beskrivelseFor(BeregningsMetode.NASJONAL)}</Radio>
                <Radio value={BeregningsMetode.PRORATA}>{beskrivelseFor(BeregningsMetode.PRORATA)}</Radio>
                <Radio value={BeregningsMetode.BEST}>{beskrivelseFor(BeregningsMetode.BEST)}</Radio>
              </>
            }
          />
          <Heading size="small" level="3">
            Gyldig for beregning
          </Heading>

          <ControlledMaanedVelger label="Fra og med" name="fom" control={control} validate={validerDatoer} required />

          <ControlledMaanedVelger
            label="Til og med"
            name="tom"
            control={control}
            validate={validerDatoer}
            required={false}
          />

          <HStack gap="4">
            <Textarea
              label="Begrunnelse"
              hideLabel={false}
              placeholder="Valgfritt"
              minRows={3}
              autoComplete="off"
              disabled={grunnlag === undefined}
              {...register('data.beregningsMetode.begrunnelse')}
            />
          </HStack>

          <HStack gap="4">
            <Button
              variant="secondary"
              size="small"
              onClick={handleSubmit(lagre)}
              disabled={watch().data.beregningsMetode.beregningsMetode === null || watch().fom === undefined}
            >
              Lagre beregningsmetode
            </Button>
          </HStack>
        </VStack>
      </Box>
    </form>
  )
}

export default BeregningsgrunnlagMetodeForAvdoed
