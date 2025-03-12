import React from 'react'
import { Box, HStack } from '@navikt/ds-react'

export function EtteroppgjoerFramTilbakeKnapperad({ children }: { children: React.ReactNode }) {
  return (
    <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
      <HStack width="100%" justify="center" gap="6">
        {children}
      </HStack>
    </Box>
  )
}
