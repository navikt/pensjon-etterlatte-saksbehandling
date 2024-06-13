import styled from 'styled-components'
import { ChevronLeftIcon } from '@navikt/aksel-icons'
import { Box, HStack, Label, Link } from '@navikt/ds-react'

export default function NavigerTilbakeMeny({ label, path }: { label: string; path: string }) {
  return (
    <NavigerTilbakeBox role="navigation">
      <HStack gap="05" align="center">
        <ChevronLeftIcon aria-hidden />
        <Label>
          <Link href={path || '/'} underline={false}>
            {label}
          </Link>
        </Label>
      </HStack>
    </NavigerTilbakeBox>
  )
}

const NavigerTilbakeBox = styled(Box)`
  padding: var(--a-spacing-4) 0 var(--a-spacing-4) var(--a-spacing-4);
  border-bottom: 1px solid var(--a-border-subtle);
  background: #f8f8f8;
`
