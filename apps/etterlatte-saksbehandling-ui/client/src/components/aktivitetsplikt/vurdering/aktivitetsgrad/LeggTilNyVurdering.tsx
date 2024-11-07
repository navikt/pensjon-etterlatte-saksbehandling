import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { VurderingAktivitetsgradForm } from './VurderingAktivitetsgradForm'
import { PlusIcon } from '@navikt/aksel-icons'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export function LeggTilNyVurdering(props: { doedsdato?: Date }) {
  const { oppdater } = useAktivitetspliktOppgaveVurdering()
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)

  function oppdaterStateVedLagring() {
    setLeggerTilVurdering(false)
    oppdater()
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button size="small" icon={<PlusIcon />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering av aktivitetsplikt
        </Button>
      </Box>
    )
  }

  return (
    <VStack gap="4">
      <Heading size="small">Ny vurdering av aktivitetsplikt</Heading>
      <VurderingAktivitetsgradForm
        doedsdato={props.doedsdato}
        onSuccess={oppdaterStateVedLagring}
        onAvbryt={() => setLeggerTilVurdering(false)}
      />
    </VStack>
  )
}
