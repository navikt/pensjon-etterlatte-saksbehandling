import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'
import { NoeytralEmoji } from '~shared/tilbakemelding/emoji/NoeytralEmoji'
import { GladEmoji } from '~shared/tilbakemelding/emoji/GladEmoji'
import { EkstatiskEmoji } from '~shared/tilbakemelding/emoji/EkstatiskEmoji'
import { ClickEvent, trackClickMedSvar } from '~utils/amplitude'
import { useState } from 'react'

enum TilbakemeldingSvar {
  VELDIG_MISFORNOEYD = 'veldig misfornøyd',
  MISFORNOEYD = 'misfornøyd',
  NOEYTRAL = 'nøytral',
  FORNOYED = 'fornøyd',
  VELDIG_FORNOYED = 'veldig fornøyd',
}

interface Props {
  spoersmaal: string
  clickEvent: ClickEvent
}

export const Tilbakemelding = ({ spoersmaal, clickEvent }: Props) => {
  const [harGittTilbakemelding, setHarGittTilbakemelding] = useState<boolean>(false)

  const trackTilbakemelding = (svar: TilbakemeldingSvar) => {
    trackClickMedSvar(clickEvent, svar)
    setHarGittTilbakemelding(true)
  }

  return (
    <Box borderRadius="large" width="fit-content" background="surface-subtle">
      <VStack padding="8" width="fit-content">
        <Heading size="medium" spacing>
          {spoersmaal}
        </Heading>

        {harGittTilbakemelding ? (
          <Heading size="medium">Takk for din tilbakemelding!</Heading>
        ) : (
          <HStack justify="center" gap="6" width="fit-content">
            <Button variant="tertiary" onClick={() => trackTilbakemelding(TilbakemeldingSvar.VELDIG_MISFORNOEYD)}>
              <VStack gap="1-alt" align="center">
                <SinnaEmoji />
                Veldig misfornøyd
              </VStack>
            </Button>
            <Button variant="tertiary" onClick={() => trackTilbakemelding(TilbakemeldingSvar.MISFORNOEYD)}>
              <VStack gap="2" align="center">
                <LeiEmoji />
                Misfornøyd
              </VStack>
            </Button>
            <Button variant="tertiary" onClick={() => trackTilbakemelding(TilbakemeldingSvar.NOEYTRAL)}>
              <VStack gap="2" align="center">
                <NoeytralEmoji />
                Nøytral
              </VStack>
            </Button>
            <Button variant="tertiary" onClick={() => trackTilbakemelding(TilbakemeldingSvar.FORNOYED)}>
              <VStack gap="2" align="center">
                <GladEmoji />
                Fornøyd
              </VStack>
            </Button>
            <Button variant="tertiary" onClick={() => trackTilbakemelding(TilbakemeldingSvar.VELDIG_FORNOYED)}>
              <VStack gap="2" align="center">
                <EkstatiskEmoji />
                Veldig fornøyd
              </VStack>
            </Button>
          </HStack>
        )}
      </VStack>
    </Box>
  )
}
