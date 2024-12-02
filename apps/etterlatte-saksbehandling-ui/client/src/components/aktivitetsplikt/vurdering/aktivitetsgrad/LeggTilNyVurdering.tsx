import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { useDispatch } from 'react-redux'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { setAktivitetspliktVurdering } from '~store/reducers/Aktivitetsplikt12mnd'
import { VurderingAktivitetsgradOgUnntak } from '~components/aktivitetsplikt/vurdering/VurderingAktivitetsgradOgUnntak'

export function LeggTilNyVurdering(props: { doedsdato?: Date }) {
  const dispatch = useDispatch()
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setAktivitetspliktVurdering(data))
    setLeggerTilVurdering(false)
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
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <VStack gap="6">
        <Heading size="medium">Vurdering av brukers aktivitet ved 12 måneder</Heading>
        <VurderingAktivitetsgradOgUnntak
          doedsdato={props.doedsdato}
          onSuccess={oppdaterStateVedLagring}
          onAvbryt={() => setLeggerTilVurdering(false)}
        />
      </VStack>
    </Box>
  )
}
