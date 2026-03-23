import React from 'react'
import { Alert, Box, List, VStack, Heading } from '@navikt/ds-react'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

export const BeregningErOverstyrtAlert = () => {
  return (
    <Box maxWidth="42.5rem">
      <Alert variant="warning">
        <VStack gap="space-16">
          Denne saken har overstyrt beregning. Sjekk om du kan skru av overstyrt beregning. Husk at saken da må
          revurderes fra første virkningstidspunkt /konverteringstidspunkt.
          <div>
            <Heading as="h3" size="xsmall">
              Saker som fortsatt trenger overstyrt beregning er:
            </Heading>
            <List as="ul" size="small">
              {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
                <List.Item key={key}>{value}</List.Item>
              ))}
            </List>
          </div>
        </VStack>
      </Alert>
    </Box>
  )
}
