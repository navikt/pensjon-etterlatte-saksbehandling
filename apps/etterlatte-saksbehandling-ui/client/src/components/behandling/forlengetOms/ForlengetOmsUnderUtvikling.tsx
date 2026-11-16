import { Alert, Box } from '@navikt/ds-react'

export const ForlengetOmsUnderUtvikling = () => (
  <Box padding="space-32">
    <Alert variant="warning">
      Forlenget OMS er under utvikling. Nye aktivitets- og vilkårsregler og vedtaksbrev er ikke ferdigstilt.
      Behandlingen kan ikke ferdigstilles.
    </Alert>
  </Box>
)
