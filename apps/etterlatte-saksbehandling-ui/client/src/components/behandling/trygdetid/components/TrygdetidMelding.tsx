import React from 'react'
import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'

export const TrygdetidMelding = ({ overskrift, beskrivelse }: { overskrift: string; beskrivelse: string }) => {
  return (
    <Box paddingInline="space-16">
      <VStack gap="space-2">
        <Heading size="small" level="3">
          {overskrift}
        </Heading>
        <BodyShort>{beskrivelse}</BodyShort>
      </VStack>
    </Box>
  )
}
