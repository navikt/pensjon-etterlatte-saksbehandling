import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { FieldErrors, useForm } from 'react-hook-form'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'
import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreInformasjonFraBruker } from '~shared/api/etteroppgjoer'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { JaNei } from '~shared/types/ISvar'
import { Dispatch, SetStateAction } from 'react'

interface Props {
  behandling: IDetaljertBehandling
  setInformasjonFraBrukerSkjemaErAapen: (erAapen: boolean) => void
  erRedigerbar: boolean
  setInformasjonFraBrukerSkjemaErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
  setValideringFeilmedling: Dispatch<SetStateAction<string>>
}

export const InformasjonFraBrukerSkjema = ({
  behandling,
  setInformasjonFraBrukerSkjemaErAapen,
  erRedigerbar,
  setInformasjonFraBrukerSkjemaErrors,
  setValideringFeilmedling,
}: Props) => {
  const etteroppgjoer = useEtteroppgjoer()

  const dispatch = useAppDispatch()

  const [informasjonFraBrukerResult, informasjonFraBrukerRequest] = useApiCall(lagreInformasjonFraBruker)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<IInformasjonFraBruker>({
    defaultValues: {
      harMottattNyInformasjon: etteroppgjoer.behandling.harMottattNyInformasjon,
      endringErTilUgunstForBruker: etteroppgjoer.behandling.endringErTilUgunstForBruker,
      beskrivelseAvUgunst: etteroppgjoer.behandling.beskrivelseAvUgunst,
    },
  })

  const avbryt = () => {
    setInformasjonFraBrukerSkjemaErrors(errors)
    setInformasjonFraBrukerSkjemaErAapen(false)
  }

  const submitEndringFraBruker = (data: IInformasjonFraBruker) => {
    setInformasjonFraBrukerSkjemaErrors(errors)
    setValideringFeilmedling('')
    informasjonFraBrukerRequest({ forbehandlingId: behandling.relatertBehandlingId!, endringFraBruker: data }, () => {
      hentEtteroppgjoerRequest(behandling.relatertBehandlingId!, (etteroppgjoer) => {
        dispatch(addEtteroppgjoer(etteroppgjoer))
        setInformasjonFraBrukerSkjemaErAapen(false)
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
          errorVedTomInput="Du må ta stilling til om bruker har gitt ny informasjon"
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
                      message: 'Du må beskrive hvorfor endringen har kommet til ugunst for bruker',
                    },
                  })}
                  label="Beskriv hvorfor endringen er til ugunst for bruker"
                  readOnly={!erRedigerbar}
                  error={errors.beskrivelseAvUgunst?.message}
                />
              </Box>
            )}
          </>
        )}

        {isFailureHandler({
          apiResult: informasjonFraBrukerResult,
          errorMessage: 'Kunne ikke lagre oppgitt svar om endring fra bruker',
        })}

        {isFailureHandler({
          apiResult: hentEtteroppgjoerResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button
            size="small"
            loading={isPending(informasjonFraBrukerResult) || isPending(hentEtteroppgjoerResult)}
            onClick={handleSubmit(submitEndringFraBruker, () => setInformasjonFraBrukerSkjemaErrors(errors))}
          >
            Lagre
          </Button>

          {!!etteroppgjoer.behandling.harMottattNyInformasjon && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(informasjonFraBrukerResult) || isPending(hentEtteroppgjoerResult)}
              onClick={avbryt}
            >
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
