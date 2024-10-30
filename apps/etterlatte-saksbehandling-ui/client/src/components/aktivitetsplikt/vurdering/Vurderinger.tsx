import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React from 'react'
import { Aktivitetsgrad } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Aktivitetsgrad'
import { Unntak } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/Unntak'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { Heading } from '@navikt/ds-react'
import { BrevAktivitetsplikt } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/BrevAktivitetsplikt'

export function Vurderinger() {
  const { oppgave, vurdering } = useAktivitetspliktOppgaveVurdering()

  return (
    <>
      <Heading size="small">
        {erOppgaveRedigerbar(oppgave.status)
          ? 'Redigering av vurderinger i en oppgave er ikke støttet enda'
          : 'Vurderinger i oppgave'}
      </Heading>
      <Aktivitetsgrad aktiviteter={vurdering.aktivitet} />
      <Unntak unntaker={vurdering.unntak} />
      <BrevAktivitetsplikt />
    </>
  )
}
