import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import { useDispatch } from 'react-redux'
import React, { useState } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerUnntakForBehandling, slettUnntakForBehandling } from '~shared/api/aktivitetsplikt'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { UnntakRedigeringsKnapper } from '~components/aktivitetsplikt/vurdering/unntak/RedigerbarUnntakOppgave'
import {
  IOpprettAktivitetspliktUnntak,
  LagreUnntakForm,
} from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgave'
import { setVurderingBehandling } from '~store/reducers/AktivitetspliktBehandlingReducer'

export function RedigerbarUnntakBehandling({
  unntak,
  behandling,
}: {
  unntak: IAktivitetspliktUnntak
  behandling: IDetaljertBehandling
}) {
  const dispatch = useDispatch()
  const [redigerer, setRedigerer] = useState(false)

  const [slettUnntakStatus, slettSpesifiktUnntak, resetSlettStatus] = useApiCall(slettUnntakForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  function slettUnntak(unntak: IAktivitetspliktUnntak) {
    slettSpesifiktUnntak(
      {
        behandlingId: behandling.id,
        sakId: behandling.sakId,
        unntakId: unntak.id,
      },
      (data) => {
        dispatch(setVurderingBehandling(data))
        setRedigerer(false)
      }
    )
  }

  function oppdaterStateEtterRedigertUnntak(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setVurderingBehandling(data))
    setRedigerer(false)
    resetSlettStatus()
  }

  if (redigerer) {
    return (
      <RedigerbartUnntak
        behandling={behandling}
        onAvbryt={() => setRedigerer(false)}
        unntak={unntak}
        oppdaterStateEtterRedigertUnntak={oppdaterStateEtterRedigertUnntak}
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

const RedigerbartUnntak = (props: {
  unntak: IAktivitetspliktUnntak
  oppdaterStateEtterRedigertUnntak: (data: IAktivitetspliktVurderingNyDto) => void
  onAvbryt: () => void
  behandling: IDetaljertBehandling
}) => {
  const { unntak, oppdaterStateEtterRedigertUnntak, onAvbryt, behandling } = props
  const [lagreUnntakStatus, lagreUnntak] = useApiCall(redigerUnntakForBehandling)
  const sendInn = (formdata: Partial<IOpprettAktivitetspliktUnntak>) => {
    lagreUnntak(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        request: {
          id: formdata.id,
          unntak: formdata.unntak!!,
          fom: formdata.fom!!,
          tom: formdata.tom,
          beskrivelse: formdata.beskrivelse || '',
        },
      },
      (data) => {
        oppdaterStateEtterRedigertUnntak(data)
      }
    )
  }

  return (
    <LagreUnntakForm
      lagreUnntakStatus={lagreUnntakStatus}
      sendInn={sendInn}
      unntak={unntak}
      vurderingType={AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER}
      onAvbryt={onAvbryt}
    />
  )
}
