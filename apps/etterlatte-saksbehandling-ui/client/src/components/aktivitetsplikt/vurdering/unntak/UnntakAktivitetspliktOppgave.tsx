import {
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'

import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerAktivitetspliktUnntakForOppgave } from '~shared/api/aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { LagreUnntakForm } from '~components/aktivitetsplikt/vurdering/unntak/LagreUnntakForm'

//TODO: flytte til domain fil
export interface IOpprettAktivitetspliktUnntak {
  id: string | undefined
  unntak: AktivitetspliktUnntakType
  fom: string
  tom?: string
  beskrivelse: string
}

export function VelgOgLagreUnntakAktivitetspliktOppgave(props: {
  unntak: IAktivitetspliktUnntak
  oppdaterStateEtterRedigertUnntak: (data: IAktivitetspliktVurderingNyDto) => void
  onAvbryt: () => void
}) {
  const { oppgave, vurderingType } = useAktivitetspliktOppgaveVurdering()
  const [lagreUnntakStatus, lagreUnntak] = useApiCall(redigerAktivitetspliktUnntakForOppgave)
  const sendInn = (formdata: Partial<IOpprettAktivitetspliktUnntak>) => {
    lagreUnntak(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: formdata.id,
          unntak: formdata.unntak!!,
          fom: formdata.fom!!,
          tom: formdata.tom,
          beskrivelse: formdata.beskrivelse || '',
        },
      },
      (data) => {
        props.oppdaterStateEtterRedigertUnntak(data)
      }
    )
  }

  return (
    <LagreUnntakForm
      lagreUnntakStatus={lagreUnntakStatus}
      sendInn={sendInn}
      unntak={props.unntak}
      vurderingType={vurderingType}
      onAvbryt={props.onAvbryt}
    />
  )
}
