import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'
import { NoeytralEmoji } from '~shared/tilbakemelding/emoji/NoeytralEmoji'
import { GladEmoji } from '~shared/tilbakemelding/emoji/GladEmoji'
import { EkstatiskEmoji } from '~shared/tilbakemelding/emoji/EkstatiskEmoji'

export const Tilbakemelding = () => {
  return (
    <Box borderRadius="large" width="fit-content" background="surface-subtle">
      <VStack padding="8" width="fit-content">
        <Heading size="medium" spacing>
          Hvor fornøyd er du med informasjonen fra bruker i søknaden?
        </Heading>

        <HStack justify="center" gap="6" width="fit-content">
          <Button variant="tertiary">
            <VStack gap="1-alt" align="center">
              <SinnaEmoji />
              Veldig misfornøyd
            </VStack>
          </Button>
          <Button variant="tertiary">
            <VStack gap="2" align="center">
              <LeiEmoji />
              Misfornøyd
            </VStack>
          </Button>
          <Button variant="tertiary">
            <VStack gap="2" align="center">
              <NoeytralEmoji />
              Nøytral
            </VStack>
          </Button>
          <Button variant="tertiary">
            <VStack gap="2" align="center">
              <GladEmoji />
              Fornøyd
            </VStack>
          </Button>
          <Button variant="tertiary">
            <VStack gap="2" align="center">
              <EkstatiskEmoji />
              Veldig fornøyd
            </VStack>
          </Button>
        </HStack>
      </VStack>
    </Box>
  )
}
