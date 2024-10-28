import { ReactNode } from 'react'
import { Box } from '@navikt/ds-react'

export const Navbar = ({ children }: { children: ReactNode }) => (
  <Box
    role="navigation"
    paddingBlock="3"
    paddingInline="5"
    borderWidth="0 0 1 0"
    borderColor="border-subtle"
    background="surface-subtle"
  >
    {children}
  </Box>
)
