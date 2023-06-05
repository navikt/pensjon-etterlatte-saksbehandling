import { ErrorSummary } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'

export const FEIL_I_PERIODE = [
  'TOM_FOER_FOM',
  'PERIODE_OVERLAPPER_MED_NESTE',
  'HULL_ETTER_PERIODE',
  'INGEN_PERIODER',
  'DEKKER_IKKE_START_AV_INTERVALL',
  'DEKKER_IKKE_SLUTT_AV_INTERVALL',
] as const
export type FeilIPeriode = (typeof FEIL_I_PERIODE)[number]
export type FeilIPeriodeGrunnlagAlle = FeilIPeriode | 'IKKE_ALLE_VALGT'

export const FeilIPerioder = (props: {
  feil: [number, FeilIPeriodeGrunnlagAlle][]
  tekster: Record<FeilIPeriodeGrunnlagAlle, string>
  hreftag: string
}) => {
  return (
    <FeilIPerioderOppsummering heading="Du må fikse feil i periodiseringen før du kan beregne">
      {props.feil.map(([index, feil]) => (
        <ErrorSummary.Item key={`${index}${feil}`} href={`#${props.hreftag}̋.${index}`}>
          {props.tekster[feil]}
        </ErrorSummary.Item>
      ))}
    </FeilIPerioderOppsummering>
  )
}

const FeilIPerioderOppsummering = styled(ErrorSummary)`
  margin: 2em auto;
  width: 30em;
`
