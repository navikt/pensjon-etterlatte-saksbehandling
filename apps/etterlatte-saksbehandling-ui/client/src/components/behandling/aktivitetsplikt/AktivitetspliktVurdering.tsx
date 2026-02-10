import { Box, Heading, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import { hentAktivitspliktVurderingForBehandling } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useDispatch } from 'react-redux'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { AktivitetsgradOgUnntakTabellBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/AktivitetsgradOgUnntakTabellBehandling'
import {
  setVurderingBehandling,
  useAktivitetspliktBehandlingState,
} from '~store/reducers/AktivitetspliktBehandlingReducer'
import { VurderAktivitetspliktWrapperBehandling } from '~components/behandling/aktivitetsplikt/VurderAktivitetspliktWrapperBehandling'
import { isBefore, subMonths } from 'date-fns'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { HarBrukerVarigUnntak } from '~components/behandling/aktivitetsplikt/unntak/HarBrukerVarigUnntak'
import { InformasjonUnntakOppfoelging } from '~components/aktivitetsplikt/vurdering/InformasjonUnntakOppfoelging'

export const vurderingHarInnhold = (vurdering: IAktivitetspliktVurderingNyDto): boolean => {
  return !!vurdering.unntak.length || !!vurdering.aktivitet.length
}

export const finnVarigUnntak = (vurdering: IAktivitetspliktVurderingNyDto): IAktivitetspliktUnntak | undefined => {
  return vurdering.unntak.find(
    (unntak) => unntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
  )
}

export const AktivitetspliktVurdering = ({
  behandling,
  setManglerAktivitetspliktVurdering,
  doedsdato,
}: {
  behandling: IDetaljertBehandling
  setManglerAktivitetspliktVurdering: (manglerVurdering: boolean) => void
  doedsdato: Date
}) => {
  const [hentetVurdering, hentVurdering] = useApiCall(hentAktivitspliktVurderingForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const dispatch = useDispatch()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const vurdering = useAktivitetspliktBehandlingState()
  useEffect(() => {
    hentVurdering({ sakId: behandling.sakId, behandlingId: behandling.id }, (result) => {
      dispatch(setVurderingBehandling(result))
    })
  }, [behandling.id])
  useEffect(() => {
    setManglerAktivitetspliktVurdering(!vurderingHarInnhold(vurdering))
  }, [vurdering])

  if (isPending(hentetVurdering)) {
    return <Spinner label="Henter aktivitetspliktsvurdering" />
  }

  const typeVurdering6eller12MndVurdering = typeVurderingFraDoedsdato(doedsdato)
  return (
    <Box maxWidth="120rem" paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
      <VStack gap="space-6">
        <VStack gap="space-2">
          <Heading size="medium" level="2">
            Vurdering av aktivitetsplikt
          </Heading>
          <HjemmelLenke
            tittel="Folketrygdloven § 17-7"
            lenke="https://lovdata.no/pro/#document/NL/lov/1997-02-28-19/%C2%A717-7"
          />
          {!!vurdering && (
            <HarBrukerVarigUnntak behandling={behandling} doedsdato={doedsdato} redigerbar={redigerbar} />
          )}
        </VStack>

        {vurdering &&
          !finnVarigUnntak(vurdering) &&
          (vurdering.aktivitet.length > 0 || vurdering.unntak.length > 0) && (
            <>
              <AktivitetsgradOgUnntakTabellBehandling
                unntak={vurdering.unntak}
                aktiviteter={vurdering.aktivitet}
                behandling={behandling}
                typeVurdering={typeVurdering6eller12MndVurdering}
              />
              {redigerbar && (
                <>
                  <VurderAktivitetspliktWrapperBehandling
                    doedsdato={doedsdato}
                    behandling={behandling}
                    defaultOpen={false}
                  />
                  <InformasjonUnntakOppfoelging vurdering={vurdering} />
                </>
              )}
            </>
          )}
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
