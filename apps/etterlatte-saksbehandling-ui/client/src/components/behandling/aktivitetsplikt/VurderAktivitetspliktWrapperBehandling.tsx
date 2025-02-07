import { IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  opprettAktivitspliktAktivitetsgradOgUnntakForBehandling,
  slettUnntakForBehandling,
} from '~shared/api/aktivitetsplikt'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import { setVurderingBehandling } from '~store/reducers/AktivitetspliktBehandlingReducer'
import { Box, Button } from '@navikt/ds-react'
import { PlusIcon } from '@navikt/aksel-icons'
import {
  NyVurderingAktivitetsgradOgUnntak,
  VurderingAktivitetsgradOgUnntak,
  VurderingKilde,
} from '~components/aktivitetsplikt/vurdering/VurderingAktivitetsgradOgUnntak'
import { typeVurderingFraDoedsdato } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'

export function VurderAktivitetspliktWrapperBehandling(props: {
  doedsdato: Date
  behandling: IDetaljertBehandling
  defaultOpen: boolean
  varigUnntak?: IAktivitetspliktUnntak
}) {
  const { doedsdato, behandling, varigUnntak } = props
  const [lagreStatus, lagreVurdering] = useApiCall(opprettAktivitspliktAktivitetsgradOgUnntakForBehandling)
  const [leggerTilVurdering, setLeggerTilVurdering] = useState(props.defaultOpen)
  const dispatch = useDispatch()

  function oppdaterStateVedLagring(data: IAktivitetspliktVurderingNyDto) {
    dispatch(setVurderingBehandling(data))
    setLeggerTilVurdering(false)
  }

  if (!leggerTilVurdering) {
    return (
      <Box>
        <Button size="small" icon={<PlusIcon aria-hidden />} onClick={() => setLeggerTilVurdering(true)}>
          Legg til ny vurdering
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
      (state) => {
        if (varigUnntak) {
          // Vi må slette eksisterende varig unntak
          slettUnntakForBehandling({
            sakId: behandling.sakId,
            behandlingId: behandling.id,
            unntakId: varigUnntak.id,
          }).finally(() =>
            oppdaterStateVedLagring({
              aktivitet: state.aktivitet,
              unntak: state.unntak.filter((unntak) => unntak.id !== varigUnntak.id),
            })
          )
        } else {
          // Vi har ikke noe eksisterende varig unntak å slette
          oppdaterStateVedLagring(state)
        }
      }
    )
  }

  return (
    <VurderingAktivitetsgradOgUnntak
      lagreStatus={lagreStatus}
      onSubmit={lagreOgOppdater}
      typeVurdering={typeVurderingFraDoedsdato(doedsdato)}
      doedsdato={doedsdato}
      onAvbryt={() => setLeggerTilVurdering(false)}
      vurderingKilde={
        behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING
          ? VurderingKilde.FOERSTEGANGSBEHANDLING
          : VurderingKilde.REVURDERING
      }
    />
  )
}
