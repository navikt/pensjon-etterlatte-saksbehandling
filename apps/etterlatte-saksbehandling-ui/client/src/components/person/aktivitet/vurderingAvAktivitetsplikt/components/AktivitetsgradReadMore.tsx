import React from 'react'
import { Box, ReadMore } from '@navikt/ds-react'

export const AktivitetsgradReadMore = () => {
  return (
    <Box maxWidth="42.5rem">
      <ReadMore header="Dette menes med aktivitetsgrad">
        I oversikten over aktivitetsgrad kan du se hvilken aktivitetsgrad brukeren har hatt. For å motta
        omstillingsstønad stilles det ingen krav til aktivitet de første seks månedene etter dødsfall. Etter seks
        måneder forventes det at du er i minst 50 % aktivitet, og etter ett år og fremover forventes det 100 %
        aktivitet. Vær oppmerksom på at det finnes unntak.
      </ReadMore>
    </Box>
  )
}
