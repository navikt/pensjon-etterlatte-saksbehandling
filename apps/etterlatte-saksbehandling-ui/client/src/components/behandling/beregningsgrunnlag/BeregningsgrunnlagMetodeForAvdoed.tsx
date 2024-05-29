import { BodyShort, Button, Heading, Radio, Textarea } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import { BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { formaterDato } from '~utils/formattering'
import { lastDayOfMonth, startOfDay } from 'date-fns'
import { SubmitHandler, useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

const beskrivelseFor = (metode: BeregningsMetode) => {
  switch (metode) {
    case BeregningsMetode.BEST:
      return 'Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter nasjonale regler)'
    case BeregningsMetode.NASJONAL:
      return 'Nasjonal beregning (folketrygdberegning)'
    case BeregningsMetode.PRORATA:
      return 'Prorata (EØS/avtale-land, der rettighet er oppfylt ved sammenlegging)'
  }
}

type BeregningsgrunnlagMetodeForAvdoedOppsummeringProps = {
  beregningsMetode: BeregningsMetode
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
        <GyldigHeading size="small" level="3">
          Trygdetid brukt i beregningen for {navn}
        </GyldigHeading>
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

  const { register, control, handleSubmit, getValues } = useForm<
    PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  >({
    defaultValues: {
      data: {
        beregningsMetode: {
          beregningsMetode: grunnlag?.data.beregningsMetode.beregningsMetode,
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
    <BeregningsgrunnlagMetodeWrapper>
      <form onSubmit={handleSubmit(lagre)}>
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

        <GyldigHeading size="small" level="3">
          Gyldig for beregning
        </GyldigHeading>

        <DatoWrapper>
          <ControlledMaanedVelger label="Fra og med" name="fom" control={control} validate={validerDatoer} required />
        </DatoWrapper>
        <DatoWrapper>
          <ControlledMaanedVelger
            label="Til og med"
            name="tom"
            control={control}
            validate={validerDatoer}
            required={false}
          />
        </DatoWrapper>

        <Begrunnelse
          label="Begrunnelse"
          disabled={grunnlag === undefined}
          {...register('data.beregningsMetode.begrunnelse')}
        />

        <Button
          variant="secondary"
          size="small"
          onClick={handleSubmit(lagre)}
          disabled={getValues().data.beregningsMetode.beregningsMetode === null || getValues().fom === undefined}
        >
          Lagre beregningsmetode
        </Button>
      </form>
    </BeregningsgrunnlagMetodeWrapper>
  )
}

const BeregningsgrunnlagMetodeWrapper = styled.div`
  padding-top: 1em;
  max-width: 70em;
  margin-bottom: 1rem;
`

const Begrunnelse = styled(Textarea).attrs({
  label: 'Begrunnelse',
  hideLabel: false,
  placeholder: 'Valgfritt',
  minRows: 3,
  autoComplete: 'off',
})`
  margin-bottom: 10px;
  margin-top: 10px;
  width: 250px;
`

const GyldigHeading = styled(Heading)`
  padding-top: 1em;
`

const DatoWrapper = styled.div`
  padding-top: 1em;
`

export default BeregningsgrunnlagMetodeForAvdoed
