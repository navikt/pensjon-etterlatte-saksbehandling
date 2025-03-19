import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'
import { GladEmoji } from '~shared/tilbakemelding/emoji/GladEmoji'
import { EkstatiskEmoji } from '~shared/tilbakemelding/emoji/EkstatiskEmoji'
import { ClickEvent, trackClickMedSvar } from '~utils/amplitude'
import { useState } from 'react'
import {
  leggTilbakemeldingGittILocalStorage,
  tilbakemeldingForBehandlingEksisterer,
} from '~shared/tilbakemelding/tilbakemeldingLocalStorage'

interface Props {
  spoersmaal: string
  clickEvent: ClickEvent
  behandlingId: string
}

export const EnigUenigTilbakemelding = ({ spoersmaal, clickEvent, behandlingId }: Props) => {
  const [tilbakemeldingAlleredeGitt, setTilbakemeldingAlleredeGitt] = useState<boolean>(
    tilbakemeldingForBehandlingEksisterer({ behandlingId, clickEvent })
  )
  const [harGittTilbakemelding, setHarGittTilbakemelding] = useState<boolean>(false)

  const trackTilbakemelding = (svar: string) => {
    trackClickMedSvar(clickEvent, svar)
    setHarGittTilbakemelding(true)
    setTimeout(() => {
      leggTilbakemeldingGittILocalStorage({ behandlingId, clickEvent })
      setTilbakemeldingAlleredeGitt(true)
    }, 3000)
  }

  return (
    !tilbakemeldingAlleredeGitt && (
      <Box borderRadius="large" width="fit-content" background="surface-subtle">
        <VStack gap="2" padding="8">
          {harGittTilbakemelding ? (
            <Heading size="small" level="3">
              Takk for din tilbakemelding!
            </Heading>
          ) : (
            <>
              <Heading size="small" level="2" spacing>
                {spoersmaal}
              </Heading>
              <HStack gap="6" justify="center" width="100%">
                <Button variant="tertiary" onClick={() => trackTilbakemelding('helt uenig')} size="small">
                  <VStack gap="1-alt" align="center">
                    <SinnaEmoji />
                    Helt uenig
                  </VStack>
                </Button>
                <Button variant="tertiary" onClick={() => trackTilbakemelding('uenig')} size="small">
                  <VStack gap="1-alt" align="center">
                    <LeiEmoji />
                    Uenig
                  </VStack>
                </Button>
                <Button variant="tertiary" onClick={() => trackTilbakemelding('enig')} size="small">
                  <VStack gap="1-alt" align="center">
                    <GladEmoji />
                    Enig
                  </VStack>
                </Button>
                <Button variant="tertiary" onClick={() => trackTilbakemelding('helt enig')} size="small">
                  <VStack gap="1-alt" align="center">
                    <EkstatiskEmoji />
                    Helt enig
                  </VStack>
                </Button>
              </HStack>
            </>
          )}
        </VStack>
      </Box>
    )
  )
}
