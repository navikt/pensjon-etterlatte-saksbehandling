import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { Heading } from '@navikt/ds-react'

import { AktivitetsgradIOppgave } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/AktivitetsgradIOppgave'
import { LeggTilUnntak } from '~components/aktivitetsplikt/vurdering/unntak/LeggTilUnntak'
import { LeggTilNyVurdering } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/LeggTilNyVurdering'
import { UnntakIOppgave } from '~components/aktivitetsplikt/vurdering/unntak/UnntakIOppgave'

export function Vurderinger(props: { doedsdato: Date }) {
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const { doedsdato } = props
  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  return (
    <>
      <Heading size="small">Vurderinger i oppgave</Heading>
      <AktivitetsgradIOppgave doedsdato={doedsdato} />
      {oppgaveErRedigerbar && <LeggTilNyVurdering doedsdato={doedsdato} />}
      <UnntakIOppgave />
      {oppgaveErRedigerbar && <LeggTilUnntak />}
    </>
  )
}
