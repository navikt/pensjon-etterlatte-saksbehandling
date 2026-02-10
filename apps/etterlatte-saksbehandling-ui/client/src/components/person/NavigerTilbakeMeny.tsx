import { ChevronLeftIcon } from '@navikt/aksel-icons'
import { HStack, Label } from '@navikt/ds-react'
import { NavLink, NavLinkProps } from 'react-router-dom'
import { Navbar } from '~shared/header/Navbar'

export default function NavigerTilbakeMeny({ ...props }: NavLinkProps) {
  return (
    <Navbar>
      <HStack gap="space-1" align="center">
        <ChevronLeftIcon aria-hidden />
        <Label>
          <NavLink {...props} />
        </Label>
      </HStack>
    </Navbar>
  )
}
