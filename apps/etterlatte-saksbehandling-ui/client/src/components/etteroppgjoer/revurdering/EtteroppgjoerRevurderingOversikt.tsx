import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import React, { useEffect, useState } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Alert, BodyShort, Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { EtteroppgjoerRevurderingResultat } from '~components/etteroppgjoer/revurdering/EtteroppgjoerRevurderingResultat'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import {
  INFORMASJON_FRA_BRUKER_ID,
  InformasjonFraBruker,
} from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBruker'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { EtteroppgjoerBehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'

enum EtteroppgjoerFeil {
  MANGLER_ETTEROPPGJOER = 'MANGLER_ETTEROPPGJOER',
  MANGLER_SVAR_NY_INFO = 'MANGLER_SVAR_NY_INFO',
  MANGLER_SVAR_UGUNST = 'MANGLER_SVAR_UGUNST',
  ETTEROPPGJOER_TIL_UGUNST = 'ETTEROPPGJOER_TIL_UGUNST',
}

const feilmeldingerEtteroppgjoer: Record<EtteroppgjoerFeil, string> = {
  [EtteroppgjoerFeil.MANGLER_ETTEROPPGJOER]: 'Har ikke lastet etteroppgjør.',
  [EtteroppgjoerFeil.MANGLER_SVAR_NY_INFO]: 'Obligatorisk å svare på om det har kommet ny informasjon.',
  [EtteroppgjoerFeil.MANGLER_SVAR_UGUNST]:
    'Obligatorisk å svare på om det er til ugunst for bruker hvis det har kommet ny informasjon.',
  [EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST]: 'Endringen skal varsles bruker, revurederingen må avbrytes.',
}

function erRevurderingGyldigAaFerdigstille(
  etteroppgjoer?: EtteroppgjoerBehandling | null
): EtteroppgjoerFeil | undefined {
  // Vi må ha svar på informasjon fra bruker
  if (!etteroppgjoer) {
    return EtteroppgjoerFeil.MANGLER_ETTEROPPGJOER
  }
  if (!etteroppgjoer.harMottattNyInformasjon) {
    return EtteroppgjoerFeil.MANGLER_SVAR_NY_INFO
  }
  if (etteroppgjoer.harMottattNyInformasjon === JaNei.JA && !etteroppgjoer.endringErTilUgunstForBruker) {
    return EtteroppgjoerFeil.MANGLER_SVAR_UGUNST
  }
  if (etteroppgjoer.harMottattNyInformasjon === JaNei.JA && etteroppgjoer.endringErTilUgunstForBruker === JaNei.JA) {
    return EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST
  }
}

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const { next } = useBehandlingRoutes()
  const [visFeilmelding, setVisFeilmelding] = useState(false)
  const [validerSkjema, setValiderSkjema] = useState<() => void>(() => undefined)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const etteroppgjoerForbehandlingId = behandling.relatertBehandlingId
  const etteroppgjoer = useAppSelector((state) => state.etteroppgjoerReducer?.etteroppgjoer)

  const dispatch = useAppDispatch()

  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const feilmeldingEtteroppgjoer = erRevurderingGyldigAaFerdigstille(etteroppgjoer?.behandling)

  function nesteSteg() {
    setVisFeilmelding(false)
    if (!!feilmeldingEtteroppgjoer) {
      validerSkjema()
      if (feilmeldingEtteroppgjoer !== EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST) {
        document.getElementById(INFORMASJON_FRA_BRUKER_ID)?.scrollIntoView({ block: 'center', behavior: 'smooth' })
      }
      setVisFeilmelding(true)
      return
    }
    next()
  }

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId])

  return mapResult(hentEtteroppgjoerResult, {
    pending: <Spinner label="Henter etteroppgjør..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente forbehandling for etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: (etteroppgjoer) => (
      <VStack gap="10" paddingInline="16" paddingBlock="16 4">
        <Heading size="xlarge" level="1">
          Etteroppgjør for {etteroppgjoer.behandling.aar}
        </Heading>
        <BodyShort>
          <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
        </BodyShort>
        <Inntektsopplysninger />

        <InformasjonFraBruker setValiderSkjema={setValiderSkjema} behandling={behandling} />

        {feilmeldingEtteroppgjoer !== EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST && (
          <>
            <FastsettFaktiskInntekt erRedigerbar={!!etteroppgjoer.behandling.harMottattNyInformasjon && erRedigerbar} />
            <ResultatAvForbehandling />
            <EtteroppgjoerRevurderingResultat />
          </>
        )}

        {(visFeilmelding || feilmeldingEtteroppgjoer === EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST) &&
          feilmeldingEtteroppgjoer && (
            <Box maxWidth="42.5rem">
              <Alert
                variant={feilmeldingEtteroppgjoer === EtteroppgjoerFeil.ETTEROPPGJOER_TIL_UGUNST ? 'error' : 'warning'}
              >
                {feilmeldingerEtteroppgjoer[feilmeldingEtteroppgjoer]}
              </Alert>
            </Box>
          )}

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <VStack gap="4" align="center">
              <div>
                <Button type="button" onClick={nesteSteg} variant="primary">
                  Neste side
                </Button>
              </div>
              <AvbrytBehandling />
            </VStack>
          </HStack>
        </Box>
      </VStack>
    ),
  })
}
