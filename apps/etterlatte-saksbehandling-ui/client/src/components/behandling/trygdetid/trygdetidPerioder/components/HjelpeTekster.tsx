import React from 'react'
import { ReadMore } from '@navikt/ds-react'

export const FaktiskTrygdetidHjelpeTekst = () => {
  return (
    <ReadMore header="Mer om faktisk trygdetid">
      Legg til aktuell trygdetid fra aktuelle land (inkludert Norge) fra avdøde var 16 år frem til og med måneden før
      hen døde. Hvis trygdetid fra flere land med ulike avtaler, må det foretas beregning innen hver avtale. Huk da av
      for &quot;Ikke med i prorata&quot; for trygdetidsperioder i land som ikke skal med i de ulike beregningene. Velg
      beste alternativ for prorata-beregning.
    </ReadMore>
  )
}

export const FremtidigTrygdetidHjelpeTekst = () => {
  return (
    <ReadMore header="Mer om fremtidig trygdetid">
      Det registreres maks fremtidig trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år. Denne vil
      automatisk bli justert i beregningen hvis faktisk trygdetid er mindre enn 4/5 av opptjeningstiden. Hvis det er
      annen grunn for reduksjon av fremtidig trygdetid må perioden redigeres.
    </ReadMore>
  )
}
