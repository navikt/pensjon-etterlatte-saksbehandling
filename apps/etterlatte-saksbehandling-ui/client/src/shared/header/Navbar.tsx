import { ReactNode } from 'react'
import { Box } from '@navikt/ds-react'

export const Navbar = ({ children }: { children: ReactNode }) => (
  <Box role="navigation" paddingBlock="space-2" paddingInline="space-4" borderWidth="0 0 1 0">
    {children}
  </Box>
)
