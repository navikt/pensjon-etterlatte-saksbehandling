import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { UnntakAktivitetspliktOppgaveForm } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgaveForm'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export function LeggTilUnntak() {
  const { oppdater } = useAktivitetspliktOppgaveVurdering()
  const [leggerTilUnntak, setLeggerTilUnntak] = useState(false)

  function oppdaterTilstandVedLagring() {
    setLeggerTilUnntak(false)
    oppdater()
  }

  if (!leggerTilUnntak) {
    return (
      <Box>
        <Button variant="secondary" icon={<PlusIcon />} size="small" onClick={() => setLeggerTilUnntak(true)}>
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
