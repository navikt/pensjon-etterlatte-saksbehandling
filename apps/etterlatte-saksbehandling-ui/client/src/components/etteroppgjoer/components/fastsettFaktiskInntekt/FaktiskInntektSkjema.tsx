import { EtteroppgjoerOversiktSkjemaer, FaktiskInntekt } from '~shared/types/EtteroppgjoerForbehandling'
import { useFormContext } from 'react-hook-form'
import { addEtteroppgjoer, addResultatEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { Box, Button, HStack, Textarea, VStack } from '@navikt/ds-react'
import { ControlledInntektTextField } from '~shared/components/textField/ControlledInntektTextField'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreFaktiskInntekt } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'
import { resetAvkorting } from '~store/reducers/BehandlingReducer'

const fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt = ({
  loennsinntekt,
  afp,
  naeringsinntekt,
  utlandsinntekt,
  spesifikasjon,
}: FastsettFaktiskInntektSkjema): FaktiskInntekt => {
  return {
    loennsinntekt: Number(loennsinntekt.replace(/[^0-9.]/g, '')),
    afp: Number(afp.replace(/[^0-9.]/g, '')),
    naeringsinntekt: Number(naeringsinntekt.replace(/[^0-9.]/g, '')),
    utlandsinntekt: Number(utlandsinntekt.replace(/[^0-9.]/g, '')),
    spesifikasjon,
  }
}

export interface FastsettFaktiskInntektSkjema {
  loennsinntekt: string
  afp: string
  naeringsinntekt: string
  utlandsinntekt: string
  spesifikasjon: string
}

interface Props {
  setFaktiskInntektSkjemaErAapen: (erAapen: boolean) => void
}

export const FaktiskInntektSkjema = ({ setFaktiskInntektSkjemaErAapen }: Props) => {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerFetch] = useApiCall(hentEtteroppgjoerForbehandling)

  const { behandling, faktiskInntekt } = useEtteroppgjoer()
  const dispatch = useAppDispatch()

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useFormContext<EtteroppgjoerOversiktSkjemaer>()

  const submitFaktiskInntekt = (faktiskInntekt: FaktiskInntekt) => {
    lagreFaktiskInntektRequest({ forbehandlingId: behandling.id, faktiskInntekt }, (resultat) => {
      dispatch(addResultatEtteroppgjoer(resultat))
      hentEtteroppgjoerFetch(resultat.forbehandlingId, (etteroppgjoer) => {
        dispatch(addEtteroppgjoer(etteroppgjoer))
        dispatch(resetAvkorting())
        setFaktiskInntektSkjemaErAapen(false)
      })
    })
  }

  return (
    <form>
      <VStack gap="4">
        <VStack gap="4" width="fit-content">
          <ControlledInntektTextField
            name="faktiskInntekt.loennsinntekt"
            control={control}
            label="Lønnsinntekt"
            description="Ekskluder omstillingsstønaden"
          />
          <ControlledInntektTextField name="faktiskInntekt.afp" control={control} label="Avtalefestet pensjon" />
          <ControlledInntektTextField name="faktiskInntekt.naeringsinntekt" control={control} label="Næringsinntekt" />
          <ControlledInntektTextField
            name="faktiskInntekt.utlandsinntekt"
            control={control}
            label="Inntekt fra utland"
          />
        </VStack>

        <SumAvFaktiskInntekt
          faktiskInntekt={fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(watch('faktiskInntekt'))}
        />
        <Box maxWidth="fit-content">
          <Textarea
            {...register('faktiskInntekt.spesifikasjon', {
              required: { value: true, message: 'Du må spesifisere inntekten' },
            })}
            label="Spesifikasjon av inntekt"
            description="Beskriv inntekt lagt til grunn og eventuelle beløp som er trukket fra."
            error={errors.faktiskInntekt?.spesifikasjon?.message}
          />
        </Box>

        {isFailureHandler({
          apiResult: lagreFaktiskInntektResult,
          errorMessage: 'Kunne ikke fastsette faktisk inntekt',
        })}
        {isFailureHandler({
          apiResult: hentEtteroppgjoerResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button
            onClick={handleSubmit((data) => {
              submitFaktiskInntekt(fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(data.faktiskInntekt))
            })}
            size="small"
            loading={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerResult)}
          >
            Fastsett inntekt
          </Button>
          {!!faktiskInntekt && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerResult)}
              onClick={() => setFaktiskInntektSkjemaErAapen(false)}
            >
              Avbryt
            </Button>
          )}
        </HStack>
      </VStack>
    </form>
  )
}
