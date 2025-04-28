import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { useForm } from 'react-hook-form'
import { addEtteroppgjoer, addResultatEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { BodyShort, Box, Button, Heading, HStack, Tag, Textarea, VStack } from '@navikt/ds-react'
import { maanedNavn } from '~utils/formatering/dato'
import { ControlledInntektTextField } from '~shared/components/textField/ControlledInntektTextField'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer, lagreFaktiskInntekt } from '~shared/api/etteroppgjoer'
import { useAppDispatch } from '~store/Store'

const fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt = ({
  loennsinntekt,
  afp,
  naeringsinntekt,
  utland,
  spesifikasjon,
}: FastsettFaktiskInntektSkjema): FaktiskInntekt => {
  return {
    loennsinntekt: Number(loennsinntekt.replace(/[^0-9.]/g, '')),
    afp: Number(afp.replace(/[^0-9.]/g, '')),
    naeringsinntekt: Number(naeringsinntekt.replace(/[^0-9.]/g, '')),
    utland: Number(utland.replace(/[^0-9.]/g, '')),
    spesifikasjon,
  }
}

interface FastsettFaktiskInntektSkjema {
  loennsinntekt: string
  afp: string
  naeringsinntekt: string
  utland: string
  spesifikasjon: string
}

interface Props {
  setRedigerFaktiskInntekt: (redigerFaktiskInntekt: boolean) => void
}

export const FaktiskInntektSkjema = ({ setRedigerFaktiskInntekt }: Props) => {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)
  const [hentEtteroppgjoerResult, hentEtteroppgjoerFetch] = useApiCall(hentEtteroppgjoer)

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
          utland: new Intl.NumberFormat('nb').format(faktiskInntekt.utland),
          spesifikasjon: faktiskInntekt.spesifikasjon,
        }
      : {
          loennsinntekt: '0',
          afp: '0',
          naeringsinntekt: '0',
          utland: '0',
          spesifikasjon: '',
        },
  })

  const submitFaktiskInntekt = (faktiskInntekt: FaktiskInntekt) => {
    lagreFaktiskInntektRequest({ forbehandlingId: behandling.id, faktiskInntekt }, (resultat) => {
      dispatch(addResultatEtteroppgjoer(resultat))
      hentEtteroppgjoerFetch(behandling.id, (etteroppgjoer) => {
        dispatch(addEtteroppgjoer(etteroppgjoer))
        setRedigerFaktiskInntekt(false)
      })
    })
  }

  return (
    <form
      onSubmit={handleSubmit((data) => submitFaktiskInntekt(fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(data)))}
    >
      <VStack gap="4">
        {/*TODO fiks dobbel visning av tag og heading*/}
        <Heading size="large">Fastsett faktisk inntekt</Heading>
        <HStack gap="2" align="center">
          <BodyShort>Fastsett den faktiske inntekten for bruker i den innvilgede perioden.</BodyShort>
        </HStack>
        <div>
          <Tag variant="neutral">
            {maanedNavn(behandling.innvilgetPeriode.fom)} - {maanedNavn(behandling.innvilgetPeriode.tom)}
          </Tag>
        </div>

        <VStack gap="4" width="fit-content">
          <ControlledInntektTextField
            name="loennsinntekt"
            control={control}
            label="Lønnsinntekt"
            description="Ekskluder omstillingsstønaden"
          />
          <ControlledInntektTextField name="afp" control={control} label="Avtalefestet pensjon" />
          <ControlledInntektTextField name="naeringsinntekt" control={control} label="Næringsinntekt" />
          <ControlledInntektTextField name="utland" control={control} label="Inntekt fra utland" />
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
          apiResult: hentEtteroppgjoerResult,
          errorMessage: 'Kunne ikke hente oppdatert etteroppgjør',
        })}

        <HStack gap="4">
          <Button size="small" loading={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerResult)}>
            Fastsett inntekt
          </Button>
          <Button
            type="button"
            variant="secondary"
            size="small"
            loading={isPending(lagreFaktiskInntektResult) || isPending(hentEtteroppgjoerResult)}
            onClick={() => setRedigerFaktiskInntekt(false)}
          >
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
