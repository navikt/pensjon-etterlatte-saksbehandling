import React from 'react'
import { Alert, Box, List, VStack } from '@navikt/ds-react'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

export const BeregningErOverstyrtAlert = () => {
  return (
    <Box maxWidth="42.5rem">
      <Alert variant="warning">
        <VStack gap="4">
          Denne saken har overstyrt beregning. Sjekk om du kan skru av overstyrt beregning. Husk at saken da må
          revurderes fra første virkningstidspunkt /konverteringstidspunkt.
          <List as="ul" size="small" title="Saker som fortsatt trenger overstyrt beregning er:">
            {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
              <List.Item key={key}>{value}</List.Item>
            ))}
          </List>
        </VStack>
      </Alert>
    </Box>
  )
}
