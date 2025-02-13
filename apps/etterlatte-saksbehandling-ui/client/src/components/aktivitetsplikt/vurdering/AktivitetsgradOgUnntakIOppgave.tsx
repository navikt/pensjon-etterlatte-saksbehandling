import React from 'react'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { RedigerbarAktivitetsgradOppgave } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/RedigerbarAktivitetsgradOppgave'
import { RedigerbarUnntakOppgave } from '~components/aktivitetsplikt/vurdering/unntak/RedigerbarUnntakOppgave'
import {
  AktivitetsgradOgUnntakTabell,
  erAktivitetsgrad,
} from '~components/aktivitetsplikt/AktivitetsgradOgUnntakTabell'

export function AktivitetsgradOgUnntakIOppgave() {
  const { vurdering } = useAktivitetspliktOppgaveVurdering()

  return (
    <AktivitetsgradOgUnntakTabell
      aktiviteter={vurdering.aktivitet}
      unntak={vurdering.unntak}
      utvidetVisning={(aktivitetEllerUnntak) =>
        erAktivitetsgrad(aktivitetEllerUnntak) ? (
          <RedigerbarAktivitetsgradOppgave aktivitet={aktivitetEllerUnntak} key={aktivitetEllerUnntak.id} />
        ) : (
          <RedigerbarUnntakOppgave unntak={aktivitetEllerUnntak} key={aktivitetEllerUnntak.id} />
        )
      }
    />
  )
}
