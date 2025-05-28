import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { IEndringFraBruker } from '~shared/types/EtteroppgjoerForbehandling'
import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreEndringFraBruker } from '~shared/api/etteroppgjoer'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { JaNei } from '~shared/types/ISvar'

interface Props {
  behandling: IDetaljertBehandling
  setEndringFraBrukerSkjemaErAapen: (erAapen: boolean) => void
  erRedigerbar: boolean
}

export const EndringFraBrukerSkjema = ({ behandling, setEndringFraBrukerSkjemaErAapen, erRedigerbar }: Props) => {
  const etteroppgjoer = useEtteroppgjoer()

  const dispatch = useAppDispatch()

  const [endringFraBrukerResult, endringFraBrukerRequest] = useApiCall(lagreEndringFraBruker)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<IEndringFraBruker>({
    defaultValues: {
      harMottattNyInformasjon: etteroppgjoer.behandling.harMottattNyInformasjon,
      endringErTilUgunstForBruker: etteroppgjoer.behandling.endringErTilUgunstForBruker,
      beskrivelseAvUgunst: etteroppgjoer.behandling.beskrivelseAvUgunst,
    },
  })

  const submitEndringFraBruker = (data: IEndringFraBruker) => {
    endringFraBrukerRequest({ forbehandlingId: behandling.relatertBehandlingId!, endringFraBruker: data }, () => {
      hentEtteroppgjoerRequest(behandling.relatertBehandlingId!, (etteroppgjoer) => {
        dispatch(addEtteroppgjoer(etteroppgjoer))
        setEndringFraBrukerSkjemaErAapen(false)
      })
    })
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

        {watch('harMottattNyInformasjon') === JaNei.JA && (
          <>
            <ControlledRadioGruppe
              name="endringErTilUgunstForBruker"
              control={control}
              legend="Er endringen til ugunst for bruker?"
              errorVedTomInput="Du må ta stilling til om endringen er til ugunst for bruker"
              readOnly={!erRedigerbar}
              radios={
                <>
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </>
              }
            />
            {watch('endringErTilUgunstForBruker') === JaNei.JA && (
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
          apiResult: endringFraBrukerResult,
          errorMessage: 'Kunne ikke lagre oppgitt svar om endring fra bruker',
        })}

        {isFailureHandler({
          apiResult: hentEtteroppgjoerResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button
            size="small"
            loading={isPending(endringFraBrukerResult) || isPending(hentEtteroppgjoerResult)}
            onClick={handleSubmit(submitEndringFraBruker)}
          >
            Lagre
          </Button>

          {!!etteroppgjoer.behandling.harMottattNyInformasjon && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(endringFraBrukerResult) || isPending(hentEtteroppgjoerResult)}
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
