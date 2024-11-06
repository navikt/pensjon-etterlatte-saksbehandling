import { Box, Heading } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React from 'react'
import { Aktivitetspliktbrev } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'
import { UtenBrevVisning } from '~components/aktivitetsplikt/brev/UtenBrevVisning'

export function VurderingInfoBrevOgOppsummering({ fetchOppgave }: { fetchOppgave: () => void }) {
  useSidetittel('Aktivitetsplikt brev og oppsummering')

  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()

  if (!aktivtetspliktbrevdata?.brevId) {
    return (
      <Box paddingInline="16" paddingBlock="16">
        <Heading size="large">Vurdering av {aktivtetspliktbrevdata?.skalSendeBrev ? 'brev' : 'oppgave'}</Heading>
        <UtenBrevVisning oppgave={oppgave} fetchOppgave={fetchOppgave} />
      </Box>
    )
  }

  return <Aktivitetspliktbrev brevId={aktivtetspliktbrevdata.brevId} sakId={oppgave.sakId} oppgaveid={oppgave.id} />
}
