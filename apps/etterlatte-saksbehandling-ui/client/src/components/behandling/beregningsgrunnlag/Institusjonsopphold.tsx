import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import React, { useState } from 'react'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { Button, ErrorSummary, Heading } from '@navikt/ds-react'
import { PlusCircleIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdGrunnlag } from '~shared/types/Beregning'
import { useFieldArray, useForm } from 'react-hook-form'
import InstitusjonsoppholdPeriode from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPeriode'
import Insthendelser from '~components/behandling/beregningsgrunnlag/Insthendelser'
import { SuccessColored } from '@navikt/ds-icons'
import { feilIKomplettePerioderOverIntervallInstitusjonsopphold } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'

type InstitusjonsoppholdProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: InstitusjonsoppholdGrunnlag) => void
}

const Institusjonsopphold = (props: InstitusjonsoppholdProps) => {
  const { behandling, onSubmit } = props
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const [visFeil, setVisFeil] = useState(false)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const { control, register, watch, handleSubmit, formState } = useForm<{
    institusjonsOppholdForm: InstitusjonsoppholdGrunnlag
  }>({
    defaultValues: {
      institusjonsOppholdForm: behandling.beregningsGrunnlag?.institusjonsopphold,
    },
  })
  const { isValid, errors } = formState
  const { fields, append, remove } = useFieldArray({
    name: 'institusjonsOppholdForm',
    control,
  })
  const heleSkjemaet = watch('institusjonsOppholdForm')
  const feilOverlappendePerioder: [number, FeilIPeriode][] = [
    ...feilIKomplettePerioderOverIntervallInstitusjonsopphold(heleSkjemaet),
  ]
  const ferdigstilleForm = (data: { institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }) => {
    if (validerInstitusjonsopphold(data.institusjonsOppholdForm) && isValid && feilOverlappendePerioder?.length === 0) {
      onSubmit(data.institusjonsOppholdForm)
      setVisFeil(false)
      setVisOkLagret(true)
      setTimeout(() => {
        setVisOkLagret(false)
      }, 1000)
    } else {
      setVisFeil(true)
      setVisOkLagret(false)
    }
  }

  return (
    <InstitusjonsoppholdsWrapper>
      <>
        <LovtekstMedLenke
          tittel={'Institusjonsopphold'}
          hjemler={[
            {
              tittel: '§ 18-8.Barnepensjon under opphold i institusjon',
              lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8',
            },
          ]}
          status={null}
        >
          <p>
            Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig ansvar
            eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske sykehusavdelinger.
            Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at barnepensjonen skal bli redusert.
            Dersom barnet har faste og nødvendige utgifter til bolig, kan arbeids- og velferdsetaten bestemme at
            barnepensjonen ikke skal reduseres eller reduseres mindre enn hovedregelen sier.
          </p>
        </LovtekstMedLenke>
        <Insthendelser sakid={behandling.sak} />
        <Heading level="3" size="small">
          Beregningsperiode institusjonsopphold
        </Heading>
      </>
      <form id="forminstitusjonsopphold">
        {fields.map((item, index) => {
          return (
            <InstitusjonsoppholdPeriode
              key={item.id}
              item={item}
              index={index}
              control={control}
              register={register}
              remove={remove}
              watch={watch}
              setVisFeil={setVisFeil}
              errors={errors.institusjonsOppholdForm?.[index]}
            />
          )
        })}
      </form>
      {behandles && (
        <Button
          type="button"
          icon={<PlusCircleIcon title="legg til" />}
          iconPosition="left"
          variant="tertiary"
          onClick={() => {
            setVisFeil(false)
            append([
              {
                fom: new Date(Date.now()),
                tom: undefined,
                data: { reduksjon: 'VELG_REDUKSJON', egenReduksjon: undefined, begrunnelse: '' },
              },
            ])
          }}
        >
          Legg til beregningsperiode
        </Button>
      )}
      {behandles && fields.length > 0 && (
        <Button type="submit" onClick={handleSubmit(ferdigstilleForm)}>
          Lagre institusjonsopphold
        </Button>
      )}
      {visFeil && feilOverlappendePerioder?.length > 0 && (
        <>
          <FeilIPerioder feil={feilOverlappendePerioder} />
        </>
      )}
      {visOkLagret && <SuccessColored fontSize={20} />}
    </InstitusjonsoppholdsWrapper>
  )
}

export default Institusjonsopphold

const InstitusjonsoppholdsWrapper = styled.div`
  padding: 0em 2em;
  max-width: 60em;
`
const FeilIPerioder = (props: { feil: [number, FeilIPeriode][] }) => {
  return (
    <FeilIPerioderOppsummering heading="Du må fikse feil i periodiseringen før du kan beregne">
      {props.feil.map(([index, feil]) => (
        <ErrorSummary.Item key={`${index}${feil}`} href={`#institusjonsopphold.${index}`}>
          {`${teksterFeilIPeriode[feil]}, opphold nummer ${index}`}
        </ErrorSummary.Item>
      ))}
    </FeilIPerioderOppsummering>
  )
}

const FeilIPerioderOppsummering = styled(ErrorSummary)`
  margin: 2em auto;
  width: 30em;
`

const validerInstitusjonsopphold = (institusjonsopphold: InstitusjonsoppholdGrunnlag): boolean => {
  return !institusjonsopphold.some((e) => e.fom === undefined)
}

export type FeilIPeriode = 'PERIODE_OVERLAPPER_MED_NESTE'[number]

const teksterFeilIPeriode: Record<FeilIPeriode, string> = {
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
} as const
