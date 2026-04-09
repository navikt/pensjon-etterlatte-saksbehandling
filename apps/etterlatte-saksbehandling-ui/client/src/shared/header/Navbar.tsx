import { ReactNode } from 'react'
import { Box } from '@navikt/ds-react'

export const Navbar = ({ children }: { children: ReactNode }) => (
  <Box
    role="navigation"
    paddingBlock="space-12"
    paddingInline="space-20"
    borderWidth="0 0 1 0"
    borderColor="neutral-subtle"
    background="neutral-soft"
  >
    {children}
  </Box>
)
