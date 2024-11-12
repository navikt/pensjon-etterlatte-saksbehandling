import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { UnntakAktivitetspliktOppgaveForm } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgaveForm'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { setAktivitetspliktVurdering } from '~store/reducers/Aktivitetsplikt12mnd'

//TODO: hvorfor er denne ubrukt?
export function LeggTilUnntak() {
  const [leggerTilUnntak, setLeggerTilUnntak] = useState(false)
  const dispatch = useDispatch()

  function oppdaterTilstandVedLagring(data: IAktivitetspliktVurderingNyDto) {
    setLeggerTilUnntak(false)
    dispatch(setAktivitetspliktVurdering(data))
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
