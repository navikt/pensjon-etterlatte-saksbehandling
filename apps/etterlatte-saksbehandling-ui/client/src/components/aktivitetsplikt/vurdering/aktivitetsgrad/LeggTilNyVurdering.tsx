import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { VurderingAktivitetsgradForm } from './VurderingAktivitetsgradForm'
import { PencilIcon } from '@navikt/aksel-icons'

export function LeggTilNyVurdering() {
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)

  function oppdaterStateVedLagring() {
    // TODO: Oppdatere state også med ny vurdering
    setLeggerTilVurdering(false)
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button icon={<PencilIcon />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering av aktivitetsplikt
        </Button>
      </Box>
    )
  }

  return (
    <VStack gap="4">
      <Heading size="small">Ny vurdering av aktivitetsplikt</Heading>
      <VurderingAktivitetsgradForm onSuccess={oppdaterStateVedLagring} onAvbryt={() => setLeggerTilVurdering(false)} />
    </VStack>
  )
}
