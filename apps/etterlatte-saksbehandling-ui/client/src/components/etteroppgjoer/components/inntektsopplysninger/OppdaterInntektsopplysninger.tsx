import { EtteroppgjoerForbehandling, EtteroppgjoerForbehandlingStatus } from '~shared/types/EtteroppgjoerForbehandling'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  erInntektsopplysningerOppdaterte,
  hentEtteroppgjoerForbehandling,
  oppdaterInntektsopplysninger,
} from '~shared/api/etteroppgjoer'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { Alert, Button, VStack } from '@navikt/ds-react'
import { useDispatch } from 'react-redux'
import { addDetaljertEtteroppgjoerForbehandling, resetEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

interface Props {
  erRedigerbar: boolean
  forbehandling: EtteroppgjoerForbehandling
}

export const OppdaterInntektsopplysninger = ({ forbehandling, erRedigerbar }: Props) => {
  const [erInntekterOppdaterteResult, erInntekterOppdaterteFetch, erInntekterOppdaterteReset] = useApiCall(
    erInntektsopplysningerOppdaterte
  )
  const [oppdaterInntekterResult, oppdaterInntekterFetch] = useApiCall(oppdaterInntektsopplysninger)
  const hentEtteroppgjoerForbehandlingRequest = useApiCall(hentEtteroppgjoerForbehandling)[1]
  const dispatch = useDispatch()

  const visOppdatertInntektToggle = useFeaturetoggle(FeatureToggle.oppdater_inntekt_forbehandling)
  const erBehandlingenFerdig = [
    EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
    EtteroppgjoerForbehandlingStatus.AVBRUTT,
  ].includes(forbehandling.status)
  const visOppdaterInntekt = visOppdatertInntektToggle && forbehandling.mottattSkatteoppgjoer && !erBehandlingenFerdig

  useEffect(() => {
    if (visOppdaterInntekt) {
      erInntekterOppdaterteReset()
      erInntekterOppdaterteFetch({ forbehandlingId: forbehandling.id })
    }
  }, [visOppdatertInntektToggle])

  function oppdaterForbehandling() {
    dispatch(resetEtteroppgjoer())
    hentEtteroppgjoerForbehandlingRequest(forbehandling.id, (etteroppgjoerForbehandling) => {
      dispatch(addDetaljertEtteroppgjoerForbehandling(etteroppgjoerForbehandling))
    })
  }

  function hentInntekterPaaNytt() {
    oppdaterInntekterFetch({ forbehandlingId: forbehandling.id }, () => {
      oppdaterForbehandling()
    })
  }

  return (
    <>
      {visOppdaterInntekt &&
        mapResult(erInntekterOppdaterteResult, {
          success: (erOppdatert) => (
            <>
              {erOppdatert === false && (
                <VStack gap="space-8">
                  <Alert variant="warning" inline>
                    Inntektsopplysningene er utdaterte.
                  </Alert>
                  {erRedigerbar && (
                    <div>
                      <Button onClick={hentInntekterPaaNytt} loading={isPending(oppdaterInntekterResult)}>
                        Hent inntektsopplysningene på nytt
                      </Button>
                    </div>
                  )}

                  {mapResult(oppdaterInntekterResult, {
                    error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                  })}
                </VStack>
              )}
            </>
          ),
        })}
    </>
  )
}
