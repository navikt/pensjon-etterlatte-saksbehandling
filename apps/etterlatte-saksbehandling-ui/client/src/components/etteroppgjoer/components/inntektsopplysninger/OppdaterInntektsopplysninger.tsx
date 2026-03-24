import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { erInntektsopplysningerOppdaterte, oppdaterInntektsopplysninger } from '~shared/api/etteroppgjoer'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { Alert, Button } from '@navikt/ds-react'
import { useDispatch } from 'react-redux'
import { resetEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

interface Props {
  erRedigerbar: boolean
  forbehandling: EtteroppgjoerForbehandling
}

export const OppdaterInntektsopplysninger = ({ forbehandling, erRedigerbar }: Props) => {
  const [erInntekterOppdaterteResult, erInntekterOppdaterteFetch, erInntekterOppdaterteReset] = useApiCall(
    erInntektsopplysningerOppdaterte
  )
  const [oppdaterInntekterResult, oppdaterInntekterFetch] = useApiCall(oppdaterInntektsopplysninger)
  const dispatch = useDispatch()

  useEffect(() => {
    erInntekterOppdaterteReset()
    if (forbehandling.mottattSkatteoppgjoer && erRedigerbar) {
      // TODO: stemmer sjekk mottattSkatteoppgjoer her?
      erInntekterOppdaterteFetch({ forbehandlingId: forbehandling.id })
    }
  }, [])

  console.log(erInntekterOppdaterteResult)

  function hentInntekterPaaNytt() {
    oppdaterInntekterFetch({ forbehandlingId: forbehandling.id }, () => {
      dispatch(resetEtteroppgjoer())
    })
  }

  return (
    <>
      {mapResult(erInntekterOppdaterteResult, {
        success: (erOppdatert) => (
          <>
            {erOppdatert === false && (
              <>
                <Alert variant="warning">
                  Inntektsopplysningene fra Skatt eller A-inntekt er utdaterte.{' '}
                  <Button onClick={hentInntekterPaaNytt} loading={isPending(oppdaterInntekterResult)}>
                    Hent inntektene på nytt
                  </Button>
                </Alert>

                {mapResult(oppdaterInntekterResult, {
                  pending: <Spinner label="Henter inntekter på nytt..." />,
                  error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                  success: () => (
                    <Alert variant="success" inline>
                      Oppdaterte inntekter
                    </Alert>
                  ),
                })}
              </>
            )}
          </>
        ),
      })}
    </>
  )
}
