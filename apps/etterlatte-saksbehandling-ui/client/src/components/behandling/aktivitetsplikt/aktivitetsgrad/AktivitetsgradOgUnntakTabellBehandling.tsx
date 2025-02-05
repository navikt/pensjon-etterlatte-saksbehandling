import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktUnntak,
} from '~shared/types/Aktivitetsplikt'
import React from 'react'
import { RedigerbarAktivitetsgradBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/RedigerbarAktivitetsgradBehandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { RedigerbarUnntakBehandling } from '~components/behandling/aktivitetsplikt/unntak/RedigerbarUnntakBehandling'
import {
  AktivitetsgradOgUnntakTabell,
  erAktivitetsgrad,
} from '~components/aktivitetsplikt/AktivitetsgradOgUnntakTabell'

export function AktivitetsgradOgUnntakTabellBehandling({
  aktiviteter,
  unntak,
  behandling,
  typeVurdering,
}: {
  aktiviteter: IAktivitetspliktAktivitetsgrad[]
  unntak: IAktivitetspliktUnntak[]
  behandling: IDetaljertBehandling
  typeVurdering: AktivitetspliktOppgaveVurderingType
}) {
  return (
    <AktivitetsgradOgUnntakTabell
      aktiviteter={aktiviteter}
      unntak={unntak}
      utvidetVisning={(aktivitetEllerUnntak) =>
        erAktivitetsgrad(aktivitetEllerUnntak) ? (
          <RedigerbarAktivitetsgradBehandling
            aktivitet={aktivitetEllerUnntak}
            behandling={behandling}
            typeVurdering={typeVurdering}
          />
        ) : (
          <RedigerbarUnntakBehandling
            unntak={aktivitetEllerUnntak}
            behandling={behandling}
            typeVurdering={typeVurdering}
          />
        )
      }
    />
  )
}
