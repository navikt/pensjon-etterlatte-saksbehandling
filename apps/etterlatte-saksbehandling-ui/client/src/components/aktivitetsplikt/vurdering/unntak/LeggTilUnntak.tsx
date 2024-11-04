import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { UnntakAktivitetspliktOppgaveForm } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgaveForm'

export function LeggTilUnntak() {
  const [leggerTilUnntak, setLeggerTilUnntak] = useState(false)

  function oppdaterTilstandVedLagring() {
    // TODO: oppdater tilstand også
    setLeggerTilUnntak(false)
  }

  if (!leggerTilUnntak) {
    return (
      <Box>
        <Button variant="secondary" icon={<PencilIcon />} size="small" onClick={() => setLeggerTilUnntak(true)}>
          Legg til nytt unntak
        </Button>
      </Box>
    )
  }

  return (
    <VStack gap="4">
      <Heading size="small">Nytt unntak fra aktivitetsplikt</Heading>
      <UnntakAktivitetspliktOppgaveForm
        onSuccess={oppdaterTilstandVedLagring}
        onAvbryt={() => setLeggerTilUnntak(false)}
      />
    </VStack>
  )
}
