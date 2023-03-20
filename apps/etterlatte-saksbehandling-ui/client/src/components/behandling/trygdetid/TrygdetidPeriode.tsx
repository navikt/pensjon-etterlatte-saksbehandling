import { ITrygdetidGrunnlag } from '~shared/api/trygdetid'
import React from 'react'

type Props = {
  grunnlag: ITrygdetidGrunnlag
}

export const TrygdetidPeriode: React.FC<Props> = ({ grunnlag }) => {
  return (
    <li>
      {grunnlag.bosted} fra {grunnlag.periodeFra} til {grunnlag.periodeTil} ({grunnlag.kilde})
    </li>
  )
}
