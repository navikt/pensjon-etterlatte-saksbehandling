import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { AktivitetspliktOppgaveVurderingType, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { hentAktivitspliktVurderingForBehandling } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useDispatch } from 'react-redux'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { UnntakTabellBehandling } from '~components/behandling/aktivitetsplikt/unntak/UnntakTabellBehandling'
import { AktivitetsgradTabellBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/AktivitetsgradTabellBehandling'
import {
  setVurderingBehandling,
  useAktivitetspliktBehandlingState,
} from '~store/reducers/AktivitetspliktBehandlingReducer'
import { VurderAktivitetspliktWrapperBehandling } from '~components/behandling/aktivitetsplikt/VurderAktivitetspliktWrapperBehandling'
import { isBefore, subMonths } from 'date-fns'

export const AktivitetspliktVurdering = ({
  behandling,
  resetManglerAktivitetspliktVurdering,
  doedsdato,
}: {
  behandling: IDetaljertBehandling
  resetManglerAktivitetspliktVurdering: () => void
  doedsdato: Date
}) => {
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurderingNyDto>()
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const dispatch = useDispatch()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const updated = useAktivitetspliktBehandlingState()
  useEffect(() => {
    if (!vurdering) {
      hent(
        { sakId: behandling.sakId, behandlingId: behandling.id },
        (result) => {
          dispatch(setVurderingBehandling(result))
          setVurdering(result)
          if (result) resetManglerAktivitetspliktVurdering()
        },
        (error) => {
          if (error.status === 404) {
            //NO-OP
          }
        }
      )
    }
  }, [])

  useEffect(() => {
    setVurdering(updated)
  }, [updated])

  if (isPending(hentet)) {
    return <Spinner label="Henter aktivitetspliktsvurdering" />
  }

  return (
    <Box maxWidth="120rem" paddingBlock="4 0" borderWidth="1 0 0 0">
      <VStack gap="6">
        <VStack gap="2">
          <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
          <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
        </VStack>
        {isSuccess(hentet) && (
          <>
            {vurdering && (
              <>
                <AktivitetsgradTabellBehandling
                  behandling={behandling}
                  aktiviteter={vurdering?.aktivitet}
                  typeVurdering={typeVurderingFraDoedsdato(doedsdato)}
                />
                <UnntakTabellBehandling behandling={behandling} unntak={vurdering?.unntak} />
              </>
            )}
          </>
        )}
        {redigerbar && <VurderAktivitetspliktWrapperBehandling doedsdato={doedsdato} behandling={behandling} />}
      </VStack>
    </Box>
  )
}

export function typeVurderingFraDoedsdato(doedsdato: Date): AktivitetspliktOppgaveVurderingType {
  if (isBefore(doedsdato, subMonths(Date.now(), 10))) {
    return AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER
  } else {
    return AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
  }
}
