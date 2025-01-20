import { Button, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'
import { NoeytralEmoji } from '~shared/tilbakemelding/emoji/NoeytralEmoji'
import { GladEmoji } from '~shared/tilbakemelding/emoji/GladEmoji'
import { EkstatiskEmoji } from '~shared/tilbakemelding/emoji/EkstatiskEmoji'

export const Tilbakemelding = () => {
  return (
    <HStack justify="center" gap="6" padding="8">
      <Button variant="secondary">
        <VStack gap="2" align="center">
          <SinnaEmoji />
          Sinna
        </VStack>
      </Button>
      <Button variant="secondary">
        <VStack gap="2" align="center">
          <LeiEmoji />
          Lei
        </VStack>
      </Button>
      <Button variant="secondary">
        <VStack gap="2" align="center">
          <NoeytralEmoji />
          Nøytral
        </VStack>
      </Button>
      <Button variant="secondary">
        <VStack gap="2" align="center">
          <GladEmoji />
          Glad
        </VStack>
      </Button>
      <Button variant="secondary">
        <VStack gap="2" align="center">
          <EkstatiskEmoji />
          Ekstatisk
        </VStack>
      </Button>
    </HStack>
  )
}
