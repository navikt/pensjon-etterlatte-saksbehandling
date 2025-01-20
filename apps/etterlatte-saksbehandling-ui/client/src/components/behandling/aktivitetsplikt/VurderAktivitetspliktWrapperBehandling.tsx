import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettAktivitspliktAktivitetsgradOgUnntakForBehandling } from '~shared/api/aktivitetsplikt'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { AktivitetspliktOppgaveVurderingType, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { setVurderingBehandling } from '~store/reducers/AktivitetspliktBehandlingReducer'
import { Box, Button } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import {
  NyVurderingAktivitetsgradOgUnntak,
  VurderingAktivitetsgradOgUnntak,
} from '~components/aktivitetsplikt/vurdering/VurderingAktivitetsgradOgUnntak'
import { typeVurderingFraDoedsdato } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'

export function VurderAktivitetspliktWrapperBehandling(props: { doedsdato: Date; behandling: IDetaljertBehandling }) {
  const { doedsdato, behandling } = props
  const [lagreStatus, lagreVurdering] = useApiCall(opprettAktivitspliktAktivitetsgradOgUnntakForBehandling)
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(false)
  const dispatch = useDispatch()

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setVurderingBehandling(data))
    setLeggerTilVurdering(false)
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button size="small" icon={<PlusIcon aria-hidden />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering av aktivitetsplikt
        </Button>
      </Box>
    )
  }

  function lagreOgOppdater(formdata: NyVurderingAktivitetsgradOgUnntak) {
    lagreVurdering(
      {
        sakId: behandling.sakId,
        behandlingId: behandling.id,
        request: {
          aktivitetsgrad: {
            id: undefined,
            vurdertFra12Mnd: typeVurderingFraDoedsdato(doedsdato) === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER,
            skjoennsmessigVurdering: formdata.vurderingAvAktivitet.skjoennsmessigVurdering,
            aktivitetsgrad: formdata.vurderingAvAktivitet.aktivitetsgrad,
            fom: formdata.vurderingAvAktivitet.fom,
            beskrivelse: formdata.vurderingAvAktivitet.beskrivelse || '',
          },
          unntak: formdata.unntak,
        },
      },
      oppdaterStateVedLagring
    )
  }

  return (
    <VurderingAktivitetsgradOgUnntak
      lagreStatus={lagreStatus}
      onSubmit={lagreOgOppdater}
      typeVurdering={typeVurderingFraDoedsdato(doedsdato)}
      doedsdato={doedsdato}
      onAvbryt={() => {}}
    />
  )
}
