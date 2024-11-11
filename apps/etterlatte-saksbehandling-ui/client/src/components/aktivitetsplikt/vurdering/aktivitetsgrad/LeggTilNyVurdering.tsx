import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { VurderingAktivitetsgradForm } from './VurderingAktivitetsgradForm'
import { PlusIcon } from '@navikt/aksel-icons'
import { useDispatch } from 'react-redux'
import { IAktivitetspliktVurderingNy } from '~shared/types/Aktivitetsplikt'
import { setAktivitetspliktVurdering } from '~store/reducers/Aktivitetsplikt12mnd'

export function LeggTilNyVurdering(props: { doedsdato?: Date }) {
  const dispatch = useDispatch()
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNy) {
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
