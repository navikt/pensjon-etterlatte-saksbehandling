import { Box, Heading, VStack } from '@navikt/ds-react'
import React from 'react'
import { Vurderinger } from '~components/aktivitetsplikt/vurdering/Vurderinger'

export function VurderAktivitet() {
  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading level="1" size="large">
          Oppfølging av aktivitet
        </Heading>
      </Box>
      <Box paddingBlock="16">
        <VStack gap="4">
          <Vurderinger />
        </VStack>
      </Box>
    </>
  )
}
