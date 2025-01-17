import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
  IOpprettAktivitetspliktAktivitetsgrad,
} from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerAktivitetsgradForOppgave } from '~shared/api/aktivitetsplikt'
import React, { useState } from 'react'
import { JaNei } from '~shared/types/ISvar'
import { RedigerbarAktivtetsgradForm } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/RedigerbarAktivtetsgradForm'

export interface RedigerAktivitetsgrad {
  typeVurdering: AktivitetspliktOppgaveVurderingType
  vurderingAvAktivitet: IOpprettAktivitetspliktAktivitetsgrad
  harUnntak?: JaNei
}

export function maanederForVurdering(typeVurdering: AktivitetspliktOppgaveVurderingType): number {
  if (typeVurdering === AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER) {
    return 6
  } else {
    return 12
  }
}

export function VurderingAktivitetsgradWrapperOppgave(props: {
  aktivitet: IAktivitetspliktAktivitetsgrad
  onAvbryt: () => void
  onSuccess: (data: IAktivitetspliktVurderingNyDto) => void
}) {
  const { aktivitet, onSuccess, onAvbryt } = props
  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const typeVurdering =
    oppgave.type === 'AKTIVITETSPLIKT'
      ? AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
      : AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER

  const [lagreStatus, redigerAktivitetsgrad] = useApiCall(redigerAktivitetsgradForOppgave)

  const [feilmelding, setFeilmelding] = useState('')
  function lagreOgOppdater(formdata: RedigerAktivitetsgrad) {
    setFeilmelding('')
    if (!formdata.vurderingAvAktivitet?.aktivitetsgrad || !formdata.vurderingAvAktivitet.fom) {
      setFeilmelding('Du må fylle ut vurderingen av aktivitetsgraden.')
      return
    }

    redigerAktivitetsgrad(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
        request: {
          id: aktivitet?.id,
          vurdertFra12Mnd: formdata.typeVurdering === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
          skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
          aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
          fom: formdata.vurderingAvAktivitet.fom,
          tom: formdata.vurderingAvAktivitet.tom,
          beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
        },
      },
      onSuccess
    )
  }

  return (
    <RedigerbarAktivtetsgradForm
      aktivitet={aktivitet}
      typeVurdering={typeVurdering}
      lagreOgOppdater={lagreOgOppdater}
      lagreStatus={lagreStatus}
      onAvbryt={onAvbryt}
      feilmelding={feilmelding}
    />
  )
}
