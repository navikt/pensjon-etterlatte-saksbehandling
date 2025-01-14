import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { setAktivitetspliktVurdering } from '~store/reducers/AktivitetsplikReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerAktivitetsgradForBehandling, slettAktivitetsgradForBehandling } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { RedigerbarAktivitsgradKnapper } from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/RedigerbarAktivitetsgrad'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import {
  RedigerAktivitetsgrad,
  RedigerbarAktivtetsGradForm,
} from '~components/aktivitetsplikt/vurdering/aktivitetsgrad/VurderingAktivitetsgradForm'

export function RedigerbarAktivitetsgradBehandling({
  aktivitet,
  behandling,
}: {
  aktivitet: IAktivitetspliktAktivitetsgrad
  behandling: IDetaljertBehandling
}) {
  const [redigerer, setRedigerer] = useState<boolean>(false)
  const dispatch = useDispatch()

  const [slettStatus, slettAktivtetsgrad] = useApiCall(slettAktivitetsgradForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  function oppdaterTilstandLagretVurdering(data: IAktivitetspliktVurderingNyDto) {
    setRedigerer(false)
    dispatch(setAktivitetspliktVurdering(data))
  }

  function slettAktivitetsgradIBehandling(aktivitet: IAktivitetspliktAktivitetsgrad) {
    slettAktivtetsgrad(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        aktivitetsgradId: aktivitet.id,
      },
      (data) => {
        dispatch(setAktivitetspliktVurdering(data))
        setRedigerer(false)
      }
    )
  }

  if (redigerer) {
    return (
      <RedigerbarAktivitetsgrad
        onSuccess={oppdaterTilstandLagretVurdering}
        onAvbryt={() => setRedigerer(false)}
        aktivitet={aktivitet}
        behandling={behandling}
      />
    )
  }

  return (
    <RedigerbarAktivitsgradKnapper
      erRedigerbar={redigerbar}
      aktivitet={aktivitet}
      setRedigerer={setRedigerer}
      slettStatus={slettStatus}
      slettAktivitetsgrad={slettAktivitetsgradIBehandling}
    />
  )
}

const RedigerbarAktivitetsgrad = ({
  onAvbryt,
  onSuccess,
  aktivitet,
  behandling,
}: {
  onAvbryt: () => void
  onSuccess: (data: IAktivitetspliktVurderingNyDto) => void
  aktivitet: IAktivitetspliktAktivitetsgrad
  behandling: IDetaljertBehandling
}) => {
  const [feilmelding, setFeilmelding] = useState('')
  const [lagreStatus, redigerAktivitetsgrad] = useApiCall(redigerAktivitetsgradForBehandling)
  function lagreOgOppdater(formdata: RedigerAktivitetsgrad) {
    setFeilmelding('')
    if (!formdata.vurderingAvAktivitet?.aktivitetsgrad || !formdata.vurderingAvAktivitet.fom) {
      setFeilmelding('Du må fylle ut vurderingen av aktivitetsgraden.')
      return
    }

    redigerAktivitetsgrad(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        request: {
          id: aktivitet?.id,
          vurdertFra12Mnd: false,
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
    <RedigerbarAktivtetsGradForm
      aktivitet={aktivitet}
      typeVurdering={AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER}
      lagreOgOppdater={lagreOgOppdater}
      lagreStatus={lagreStatus}
      onAvbryt={onAvbryt}
      feilmelding={feilmelding}
    />
  )
}
