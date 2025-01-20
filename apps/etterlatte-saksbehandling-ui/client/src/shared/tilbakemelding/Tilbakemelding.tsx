import { Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'

export const Tilbakemelding = () => {
  return (
    <HStack justify="center" gap="6" padding="8">
      <Button variant="secondary">
        <VStack gap="2" justify="center">
          <SinnaEmoji />
          <Heading size="small">Sinna</Heading>
        </VStack>
      </Button>
      <Button variant="secondary">
        <VStack gap="2" justify="center">
          <LeiEmoji />
          <Heading size="small">Lei</Heading>
        </VStack>
      </Button>
    </HStack>
  )
}
