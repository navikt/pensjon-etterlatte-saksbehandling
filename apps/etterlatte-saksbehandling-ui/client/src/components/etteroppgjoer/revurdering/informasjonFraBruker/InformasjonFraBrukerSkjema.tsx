import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { useFormContext } from 'react-hook-form'
import { EtteroppgjoerOversiktSkjemaer } from '~shared/types/EtteroppgjoerForbehandling'
import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreInformasjonFraBruker } from '~shared/api/etteroppgjoer'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch } from '~store/Store'
import { JaNei } from '~shared/types/ISvar'

interface Props {
  behandling: IDetaljertBehandling
  setInformasjonFraBrukerSkjemaErAapen: (erAapen: boolean) => void
  erRedigerbar: boolean
}

export const InformasjonFraBrukerSkjema = ({
  behandling,
  setInformasjonFraBrukerSkjemaErAapen,
  erRedigerbar,
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
  } = useFormContext<EtteroppgjoerOversiktSkjemaer>()

  const submitEndringFraBruker = (data: EtteroppgjoerOversiktSkjemaer) => {
    informasjonFraBrukerRequest(
      { forbehandlingId: behandling.relatertBehandlingId!, endringFraBruker: data.informasjonFraBruker! },
      () => {
        hentEtteroppgjoerRequest(behandling.relatertBehandlingId!, (etteroppgjoer) => {
          dispatch(addEtteroppgjoer(etteroppgjoer))
          setInformasjonFraBrukerSkjemaErAapen(false)
        })
      }
    )
  }

  return (
    <form>
      <VStack gap="4">
        <ControlledRadioGruppe
          name="informasjonFraBruker.harMottattNyInformasjon"
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

        {watch('informasjonFraBruker.harMottattNyInformasjon') === JaNei.JA && (
          <>
            <ControlledRadioGruppe
              name="informasjonFraBruker.endringErTilUgunstForBruker"
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
            {watch('informasjonFraBruker.endringErTilUgunstForBruker') === JaNei.JA && (
              <Box maxWidth="30rem">
                <Textarea
                  {...register('informasjonFraBruker.beskrivelseAvUgunst', {
                    required: {
                      value: true,
                      message: 'Du må beskrive hvorfor endringen har kommet til ugunst for bruker',
                    },
                  })}
                  label="Beskriv hvorfor endringen er til ugunst for bruker"
                  readOnly={!erRedigerbar}
                  error={errors.informasjonFraBruker?.beskrivelseAvUgunst?.message}
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
            onClick={handleSubmit(submitEndringFraBruker)}
          >
            Lagre
          </Button>

          {!!etteroppgjoer.behandling.harMottattNyInformasjon && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(informasjonFraBrukerResult) || isPending(hentEtteroppgjoerResult)}
              onClick={() => setInformasjonFraBrukerSkjemaErAapen(false)}
            >
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
