import styled from 'styled-components'
import { ChevronLeftIcon } from '@navikt/aksel-icons'
import { Box, HStack, Label } from '@navikt/ds-react'
import { NavLink, NavLinkProps } from 'react-router-dom'

export default function NavigerTilbakeMeny({ ...props }: NavLinkProps) {
  return (
    <NavigerTilbakeBox role="navigation">
      <HStack gap="05" align="center">
        <ChevronLeftIcon aria-hidden />
        <Label>
          <NavLink {...props} />
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
