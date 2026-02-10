import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'

import { AktivitetsgradOgUnntakIOppgave } from '~components/aktivitetsplikt/vurdering/AktivitetsgradOgUnntakIOppgave'
import { LeggTilNyVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/LeggTilNyVurdering'
import { InformasjonUnntakOppfoelging } from '~components/aktivitetsplikt/vurdering/InformasjonUnntakOppfoelging'

export function Vurderinger(props: { doedsdato?: Date }) {
  const { oppgave, vurdering } = useAktivitetspliktOppgaveVurdering()
  const { doedsdato } = props
  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  return (
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
      <VStack gap="space-6">
        <VStack gap="space-2">
          <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
          <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
        </VStack>
        <AktivitetsgradOgUnntakIOppgave />

        {oppgaveErRedigerbar && (
          <>
            <InformasjonUnntakOppfoelging vurdering={vurdering} />
            <LeggTilNyVurdering doedsdato={doedsdato} />
          </>
        )}
      </VStack>
    </Box>
  )
}
