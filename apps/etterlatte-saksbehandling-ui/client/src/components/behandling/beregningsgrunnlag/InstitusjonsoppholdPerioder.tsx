import { ErrorSummary } from '@navikt/ds-react'
import styled from 'styled-components'
import { InstitusjonsoppholdGrunnlagData } from '~shared/types/Beregning'
import { formaterDato } from '~utils/formattering'

const FeilIPerioderOppsummering = styled(ErrorSummary)`
  margin: 2em auto;
  width: 30em;
`

export const FeilIPerioder = (props: { feil: [number, FeilIPeriode][] }) => {
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

export const validerInstitusjonsopphold = (institusjonsopphold: InstitusjonsoppholdGrunnlagData): boolean => {
  return !institusjonsopphold.some((e) => e.fom === undefined)
}

export type FeilIPeriode = 'PERIODE_OVERLAPPER_MED_NESTE'[number]

const teksterFeilIPeriode: Record<FeilIPeriode, string> = {
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
} as const

export const PeriodeVisning = (props: { fom: Date; tom: Date | undefined }) => {
  const { fom, tom } = props

  return <>{`${formaterDato(fom)} - ${tom ? formaterDato(tom) : ''}`}</>
}
