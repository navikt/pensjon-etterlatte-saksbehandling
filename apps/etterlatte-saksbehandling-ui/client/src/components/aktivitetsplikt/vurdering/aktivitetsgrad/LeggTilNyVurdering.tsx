import React, { useState } from 'react'
import { Box, Button, Heading, VStack } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import { useDispatch } from 'react-redux'
import { AktivitetspliktOppgaveVurderingType, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { VurderAktivitetspliktWrapper } from '~components/aktivitetsplikt/vurdering/VurderingAktivitetsgradOgUnntak'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'

export function LeggTilNyVurdering(props: { doedsdato?: Date }) {
  const dispatch = useDispatch()
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)
  const { vurderingType } = useAktivitetspliktOppgaveVurdering()

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setAktivitetspliktVurdering(data))
    setLeggerTilVurdering(false)
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button size="small" icon={<PlusIcon aria-hidden />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering av aktivitetsplikt
        </Button>
      </Box>
    )
  }

  return (
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
      <VStack gap="space-6">
        <Heading size="medium">
          {vurderingType === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER
            ? 'Vurdering av brukers aktivitet ved 12 måneder'
            : 'Vurdering av brukers aktivitet ved 6 måneder'}
        </Heading>
        <VurderAktivitetspliktWrapper
          doedsdato={props.doedsdato}
          onSuccess={oppdaterStateVedLagring}
          onAvbryt={() => setLeggerTilVurdering(false)}
        />
      </VStack>
    </Box>
  )
}
