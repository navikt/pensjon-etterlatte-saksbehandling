import React from 'react'
import { Alert, BodyShort, Box, List, VStack } from '@navikt/ds-react'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

export const BeregningErOverstyrtAlert = () => {
  return (
    <Box maxWidth="42.5rem">
      <Alert variant="warning">
        <VStack gap="space-4">
          Denne saken har overstyrt beregning. Sjekk om du kan skru av overstyrt beregning. Husk at saken da må
          revurderes fra første virkningstidspunkt /konverteringstidspunkt.
          <BodyShort size="small">Saker som fortsatt trenger overstyrt beregning er:</BodyShort>
          <Box marginBlock="space-12" asChild>
            <List data-aksel-migrated-v8 as="ul" size="small">
              {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
                <List.Item key={key}>{value}</List.Item>
              ))}
            </List>
          </Box>
        </VStack>
      </Alert>
    </Box>
  )
}
