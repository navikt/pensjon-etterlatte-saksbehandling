import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  hentEtteroppgjoerForbehandling,
  lagreEndringErTilUgunstForBruker,
  lagreHarMottattNyInformasjon,
} from '~shared/api/etteroppgjoer'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'

const endringFraBrukerSkjemaDefaultValues = (
  etteroppgjoerForbehandling: EtteroppgjoerForbehandling
): EndringFraBrukerSkjemaVerdier => {
  let endringFraBrukerSkjemaValues: EndringFraBrukerSkjemaVerdier = {
    harMottattNyInformasjon: '',
    endringErTilUgunstForBruker: '',
    beskrivelseAvUgunst: '',
  }

  if (etteroppgjoerForbehandling) {
    if (etteroppgjoerForbehandling.behandling) {
      if (etteroppgjoerForbehandling.behandling.harMottattNyInformasjon === true) {
        endringFraBrukerSkjemaValues = {
          ...endringFraBrukerSkjemaValues,
          harMottattNyInformasjon: 'JA',
        }
      } else if (!!etteroppgjoerForbehandling.behandling.harMottattNyInformasjon) {
        endringFraBrukerSkjemaValues = {
          ...endringFraBrukerSkjemaValues,
          harMottattNyInformasjon: 'NEI',
        }
      }

      if (etteroppgjoerForbehandling.behandling.endringErTilUgunstForBruker === true) {
        endringFraBrukerSkjemaValues = {
          ...endringFraBrukerSkjemaValues,
          endringErTilUgunstForBruker: 'JA',
        }
      } else if (!!etteroppgjoerForbehandling.behandling.endringErTilUgunstForBruker) {
        endringFraBrukerSkjemaValues = {
          ...endringFraBrukerSkjemaValues,
          endringErTilUgunstForBruker: 'NEI',
        }
      }

      if (!!etteroppgjoerForbehandling.behandling.beskrivelseAvUgunst) {
        endringFraBrukerSkjemaValues = {
          ...endringFraBrukerSkjemaValues,
          beskrivelseAvUgunst: etteroppgjoerForbehandling.behandling.beskrivelseAvUgunst,
        }
      }
    }
  }

  return endringFraBrukerSkjemaValues
}

interface Props {
  behandling: IDetaljertBehandling
  setEndringFraBrukerSkjemaErAapen: (erAapen: boolean) => void
  erRedigerbar: boolean
}

interface EndringFraBrukerSkjemaVerdier {
  harMottattNyInformasjon: 'JA' | 'NEI' | ''
  endringErTilUgunstForBruker: 'JA' | 'NEI' | ''
  beskrivelseAvUgunst: string
}

export const EndringFraBrukerSkjema = ({ behandling, setEndringFraBrukerSkjemaErAapen, erRedigerbar }: Props) => {
  const etteroppgjoer = useEtteroppgjoer()

  const dispatch = useAppDispatch()

  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)
  const [harMottattNyInformasjonResult, harMottattNyInformasjonRequest] = useApiCall(lagreHarMottattNyInformasjon)
  const [endringErTilUgunstForBrukerResult, endringErTilUgunstForBrukerRequest] = useApiCall(
    lagreEndringErTilUgunstForBruker
  )

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<EndringFraBrukerSkjemaVerdier>({
    defaultValues: endringFraBrukerSkjemaDefaultValues(etteroppgjoer),
  })

  const submitEndringFraBruker = (data: EndringFraBrukerSkjemaVerdier) => {
    const harMottattNyInformasjon: boolean = data.harMottattNyInformasjon === 'JA'

    if (harMottattNyInformasjon) {
      harMottattNyInformasjonRequest(
        { forbehandlingId: behandling.relatertBehandlingId!, harMottattNyInformasjon },
        () => {
          endringErTilUgunstForBrukerRequest(
            {
              forbehandlingId: behandling.relatertBehandlingId!,
              endringErTilUgunstForBruker: data.endringErTilUgunstForBruker === 'JA',
              beskrivelseAvUgunst: data.beskrivelseAvUgunst,
            },
            () => {
              hentEtteroppgjoerRequest(behandling.relatertBehandlingId!, (etteroppgjoer) => {
                dispatch(addEtteroppgjoer(etteroppgjoer))
                setEndringFraBrukerSkjemaErAapen(false)
              })
            }
          )
        }
      )
    } else {
      harMottattNyInformasjonRequest(
        { forbehandlingId: behandling.relatertBehandlingId!, harMottattNyInformasjon },
        () => {
          hentEtteroppgjoerRequest(behandling.relatertBehandlingId!, (etteroppgjoer) => {
            dispatch(addEtteroppgjoer(etteroppgjoer))
            setEndringFraBrukerSkjemaErAapen(false)
          })
        }
      )
    }
  }

  return (
    <form>
      <VStack gap="4">
        <ControlledRadioGruppe
          name="harMottattNyInformasjon"
          control={control}
          legend="Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?"
          errorVedTomInput="Du må ta stilling til om bruker gitt ny informasjon"
          readOnly={!erRedigerbar}
          radios={
            <>
              <Radio value="JA">Ja</Radio>
              <Radio value="NEI">Nei</Radio>
            </>
          }
        />

        {watch('harMottattNyInformasjon') === 'JA' && (
          <>
            <ControlledRadioGruppe
              name="endringErTilUgunstForBruker"
              control={control}
              legend="Er endringen til ugunst for bruker?"
              errorVedTomInput="Du må ta stilling til om endringen er til ugunst for bruker"
              readOnly={!erRedigerbar}
              radios={
                <>
                  <Radio value="JA">Ja</Radio>
                  <Radio value="NEI">Nei</Radio>
                </>
              }
            />
            {watch('endringErTilUgunstForBruker') === 'JA' && (
              <Box maxWidth="30rem">
                <Textarea
                  {...register('beskrivelseAvUgunst', {
                    required: {
                      value: true,
                      message: 'Du må beskrive hvorfor endringen kommer til ugunst',
                    },
                  })}
                  label="Beskriv hvorfor det er til ugunst"
                  readOnly={!erRedigerbar}
                  error={errors.beskrivelseAvUgunst?.message}
                />
              </Box>
            )}
          </>
        )}

        {isFailureHandler({
          apiResult: harMottattNyInformasjonResult,
          errorMessage: 'Kunne ikke lagre om det er mottatt ny informasjon',
        })}

        {isFailureHandler({
          apiResult: endringErTilUgunstForBrukerResult,
          errorMessage: 'Kunne ikke lagre om det er til ugunst for bruker',
        })}

        {isFailureHandler({
          apiResult: hentEtteroppgjoerResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button size="small" onClick={handleSubmit(submitEndringFraBruker)}>
            Lagre
          </Button>

          {!!etteroppgjoer.behandling.harMottattNyInformasjon && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(harMottattNyInformasjonResult) || isPending(endringErTilUgunstForBrukerResult)}
              onClick={() => setEndringFraBrukerSkjemaErAapen(false)}
            >
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
