import { FaktiskInntekt } from '~shared/types/EtteroppgjoerForbehandling'
import { FieldErrors, useForm } from 'react-hook-form'
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
  setFastsettFaktiskInntektSkjemaErrors: (errors: FieldErrors<FastsettFaktiskInntektSkjema> | undefined) => void
}

export const FaktiskInntektSkjema = ({
  setFaktiskInntektSkjemaErAapen,
  setFastsettFaktiskInntektSkjemaErrors,
}: Props) => {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)
  const [hentEtteroppgjoerForbehandlingResult, hentEtteroppgjoerForbehandlingFetch] =
    useApiCall(hentEtteroppgjoerForbehandling)

  const { behandling, faktiskInntekt } = useEtteroppgjoer()
  const dispatch = useAppDispatch()

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<FastsettFaktiskInntektSkjema>({
    defaultValues: faktiskInntekt
      ? {
          loennsinntekt: new Intl.NumberFormat('nb').format(faktiskInntekt.loennsinntekt),
          afp: new Intl.NumberFormat('nb').format(faktiskInntekt.afp),
          naeringsinntekt: new Intl.NumberFormat('nb').format(faktiskInntekt.naeringsinntekt),
          utlandsinntekt: new Intl.NumberFormat('nb').format(faktiskInntekt.utlandsinntekt),
          spesifikasjon: faktiskInntekt.spesifikasjon,
        }
      : {
          loennsinntekt: '0',
          afp: '0',
          naeringsinntekt: '0',
          utlandsinntekt: '0',
          spesifikasjon: '',
        },
  })

  const avbryt = () => {
    setFastsettFaktiskInntektSkjemaErrors(errors)
    setFaktiskInntektSkjemaErAapen(false)
  }

  const submitFaktiskInntekt = (faktiskInntekt: FaktiskInntekt) => {
    setFastsettFaktiskInntektSkjemaErrors(undefined)
    lagreFaktiskInntektRequest({ forbehandlingId: behandling.id, faktiskInntekt }, (resultat) => {
      dispatch(addResultatEtteroppgjoer(resultat))
      hentEtteroppgjoerForbehandlingFetch(resultat.forbehandlingId, (etteroppgjoer) => {
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
            name="loennsinntekt"
            control={control}
            label="Lønnsinntekt"
            description="Ekskluder omstillingsstønaden"
          />
          <ControlledInntektTextField name="afp" control={control} label="Avtalefestet pensjon" />
          <ControlledInntektTextField name="naeringsinntekt" control={control} label="Næringsinntekt" />
          <ControlledInntektTextField name="utlandsinntekt" control={control} label="Inntekt fra utland" />
        </VStack>

        <SumAvFaktiskInntekt faktiskInntekt={fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(watch())} />
        <Box maxWidth="fit-content">
          <Textarea
            {...register('spesifikasjon', {
              required: { value: true, message: 'Du må spesifisere inntekten' },
            })}
            label="Spesifikasjon av inntekt"
            description="Beskriv inntekt lagt til grunn og eventuelle beløp som er trukket fra."
            error={errors.spesifikasjon?.message}
          />
        </Box>

        {isFailureHandler({
          apiResult: lagreFaktiskInntektResult,
          errorMessage: 'Kunne ikke fastsette faktisk inntekt',
        })}
        {isFailureHandler({
          apiResult: hentEtteroppgjoerForbehandlingResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button
            onClick={handleSubmit(
              (data) => {
                submitFaktiskInntekt(fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(data))
              },
              () => setFastsettFaktiskInntektSkjemaErrors(errors)
            )}
            size="small"
            loading={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerForbehandlingResult)}
          >
            Fastsett inntekt
          </Button>
          {!!faktiskInntekt && (
            <Button
              type="button"
              variant="secondary"
              size="small"
              disabled={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerForbehandlingResult)}
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
