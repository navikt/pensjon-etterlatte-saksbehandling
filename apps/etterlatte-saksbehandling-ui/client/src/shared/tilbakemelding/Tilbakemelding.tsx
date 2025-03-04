import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { SinnaEmoji } from '~shared/tilbakemelding/emoji/SinnaEmoji'
import { LeiEmoji } from '~shared/tilbakemelding/emoji/LeiEmoji'
import { NoeytralEmoji } from '~shared/tilbakemelding/emoji/NoeytralEmoji'
import { GladEmoji } from '~shared/tilbakemelding/emoji/GladEmoji'
import { EkstatiskEmoji } from '~shared/tilbakemelding/emoji/EkstatiskEmoji'
import { ClickEvent, trackClickMedSvar } from '~utils/amplitude'
import { JSX, useState } from 'react'
import {
  leggTilbakemeldingGittILocalStorage,
  tilbakemeldingForBehandlingEksisterer,
} from '~shared/tilbakemelding/tilbakemeldingLocalStorage'

enum AlternativEmoji {
  SINNA = 'SINNA',
  LEI = 'LEI',
  NOEYTRAL = 'NOEYTRAL',
  GLAD = 'GLAD',
  EKSTATISK = 'EKSTATISK',
}

type Alternativ = {
  emoji: AlternativEmoji
  tekstVisning: string
  tekstEvent: string
}

const emojiForAlternativ: Record<AlternativEmoji, JSX.Element> = {
  [AlternativEmoji.EKSTATISK]: <EkstatiskEmoji />,
  [AlternativEmoji.GLAD]: <GladEmoji />,
  [AlternativEmoji.LEI]: <LeiEmoji />,
  [AlternativEmoji.NOEYTRAL]: <NoeytralEmoji />,
  [AlternativEmoji.SINNA]: <SinnaEmoji />,
}

const FORNOEYD_MISFORNOEYD_ALTERNATIVER: Alternativ[] = [
  {
    emoji: AlternativEmoji.SINNA,
    tekstVisning: 'Veldig misfornøyd',
    tekstEvent: 'veldig misfornøyd',
  },
  {
    emoji: AlternativEmoji.LEI,
    tekstVisning: 'Misfornøyd',
    tekstEvent: 'misfornøyd',
  },
  {
    emoji: AlternativEmoji.NOEYTRAL,
    tekstVisning: 'Nøytral',
    tekstEvent: 'nøytral',
  },
  {
    emoji: AlternativEmoji.GLAD,
    tekstVisning: 'Fornøyd',
    tekstEvent: 'fornøyd',
  },
  {
    emoji: AlternativEmoji.EKSTATISK,
    tekstVisning: 'Veldig fornøyd',
    tekstEvent: 'veldig fornøyd',
  },
] as const

const ENIG_UNIG_ALTERNATIVER: Alternativ[] = [
  {
    emoji: AlternativEmoji.SINNA,
    tekstVisning: 'Helt uenig',
    tekstEvent: 'helt uenig',
  },
  {
    emoji: AlternativEmoji.LEI,
    tekstVisning: 'Uenig',
    tekstEvent: 'uenig',
  },
  {
    emoji: AlternativEmoji.GLAD,
    tekstVisning: 'Enig',
    tekstEvent: 'enig',
  },
  {
    emoji: AlternativEmoji.EKSTATISK,
    tekstVisning: 'Helt enig',
    tekstEvent: 'helt enig',
  },
] as const

interface Props {
  spoersmaal: string
  clickEvent: ClickEvent
  behandlingId: string
}

export const FornoeydMisfornoeydTilbakemelding = (props: Props) => (
  <Tilbakemelding {...props} alternativer={FORNOEYD_MISFORNOEYD_ALTERNATIVER} />
)
export const EnigUenigTilbakemelding = (props: Props) => (
  <Tilbakemelding {...props} alternativer={ENIG_UNIG_ALTERNATIVER} />
)

interface TilbakemeldingProps extends Props {
  alternativer: Alternativ[]
}

export const Tilbakemelding = ({ spoersmaal, clickEvent, behandlingId, alternativer }: TilbakemeldingProps) => {
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
          <Heading size="small" level="2" spacing>
            {spoersmaal}
          </Heading>

          {harGittTilbakemelding ? (
            <Heading size="small" level="3">
              Takk for din tilbakemelding!
            </Heading>
          ) : (
            <HStack gap="6" justify="center" width="100%">
              {alternativer.map((alternativ) => (
                <Button
                  variant="tertiary"
                  key={alternativ.tekstEvent}
                  onClick={() => trackTilbakemelding(alternativ.tekstEvent)}
                  size="small"
                >
                  <VStack gap="1-alt" align="center">
                    {emojiForAlternativ[alternativ.emoji]}
                    {alternativ.tekstVisning}
                  </VStack>
                </Button>
              ))}
            </HStack>
          )}
        </VStack>
      </Box>
    )
  )
}
