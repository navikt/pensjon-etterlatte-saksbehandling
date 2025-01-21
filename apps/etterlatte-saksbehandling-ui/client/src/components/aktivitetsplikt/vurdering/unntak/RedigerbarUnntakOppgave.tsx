import { IAktivitetspliktUnntak, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktUnntakForOppgave } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { VelgOgLagreUnntakAktivitetspliktOppgave } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgave'
import { UnntakRedigeringsKnapper } from '~components/aktivitetsplikt/vurdering/unntak/UnntakRedigeringsKnapper'

export function RedigerbarUnntakOppgave(props: { unntak: IAktivitetspliktUnntak }) {
  const dispatch = useDispatch()
  const { unntak } = props
  const [redigerer, setRedigerer] = useState(false)

  const { oppgave } = useAktivitetspliktOppgaveVurdering()
  const [slettUnntakStatus, slettSpesifiktUnntak, resetSlettStatus] = useApiCall(slettAktivitetspliktUnntakForOppgave)
  const redigerbar = erOppgaveRedigerbar(oppgave.status)

  function slettUnntak(unntak: IAktivitetspliktUnntak) {
    slettSpesifiktUnntak(
      {
        oppgaveId: oppgave.id,
        sakId: unntak.sakId,
        unntakId: unntak.id,
      },
      (data) => {
        dispatch(setAktivitetspliktVurdering(data))
        setRedigerer(false)
      }
    )
  }

  function oppdaterStateEtterRedigertUnntak(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setAktivitetspliktVurdering(data))
    setRedigerer(false)
    resetSlettStatus()
  }

  if (redigerer) {
    return (
      <VelgOgLagreUnntakAktivitetspliktOppgave
        oppdaterStateEtterRedigertUnntak={oppdaterStateEtterRedigertUnntak}
        onAvbryt={() => setRedigerer(false)}
        unntak={unntak}
      />
    )
  }

  return (
    <UnntakRedigeringsKnapper
      redigerbar={redigerbar}
      unntak={unntak}
      slettUnntakStatus={slettUnntakStatus}
      setRedigerer={setRedigerer}
      slettUnntak={slettUnntak}
    />
  )
}
