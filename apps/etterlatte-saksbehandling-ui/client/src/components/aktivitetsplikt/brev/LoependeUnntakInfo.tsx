import { Alert } from '@navikt/ds-react'
import React from 'react'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'

function loeperUnntakEtterSisteVurdering(vurdering: IAktivitetspliktVurderingNyDto): boolean {
  const nyesteVurdering = [...vurdering.aktivitet]
    .sort((a, b) => new Date(a.fom).getTime() - new Date(b.fom).getTime())
    .pop()
  if (!nyesteVurdering) {
    return true
  }
  const nyesteFom = new Date(nyesteVurdering.fom)

  const harUnntakSomLoeper = vurdering.unntak.some((unntak) => !unntak.tom || new Date(unntak.tom) >= nyesteFom)
  return harUnntakSomLoeper
}

export function LoependeUnntakInfo() {
  const { aktivtetspliktbrevdata, vurdering } = useAktivitetspliktOppgaveVurdering()
  const harUnntakLoepende = loeperUnntakEtterSisteVurdering(vurdering)
  if (aktivtetspliktbrevdata?.brevId && harUnntakLoepende) {
    return (
      <Alert variant="info">
        Det er unntak som gjelder for samme periode som den siste vurderingen av aktivitet. Hvis unntaket er gjeldende
        må innhold i brevet tilpasses.
      </Alert>
    )
  } else {
    return null
  }
}
