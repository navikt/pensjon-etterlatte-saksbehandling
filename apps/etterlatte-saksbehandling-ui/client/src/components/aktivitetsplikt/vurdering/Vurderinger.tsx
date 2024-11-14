import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'

import { AktivitetsgradIOppgave } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/AktivitetsgradIOppgave'
import { LeggTilNyVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/LeggTilNyVurdering'
import { UnntakIOppgave } from '~components/aktivitetsplikt/vurdering/unntak/UnntakIOppgave'

export function Vurderinger(props: { doedsdato: Date }) {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const { doedsdato } = props
  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  return (
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <VStack gap="6">
        <VStack gap="2">
          <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
          <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
        </VStack>
        <AktivitetsgradIOppgave doedsdato={doedsdato} />
        <UnntakIOppgave />

        {oppgaveErRedigerbar && <LeggTilNyVurdering doedsdato={doedsdato} />}
      </VStack>
    </Box>
  )
}
