import React from 'react'
import { Box, ReadMore } from '@navikt/ds-react'

export const UnntakReadMore = () => {
  return (
    <Box maxWidth="42.5rem">
      <ReadMore header="Dette menes med unntak">
        I oversikten over unntak ser du hvilke unntak som er satt på den gjenlevende. Det finnes både midlertidige og
        varige unntak
      </ReadMore>
    </Box>
  )
}
