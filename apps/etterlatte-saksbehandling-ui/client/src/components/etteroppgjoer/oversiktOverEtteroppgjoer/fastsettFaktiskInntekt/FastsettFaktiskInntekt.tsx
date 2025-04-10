import { BodyShort, Box, Button, Heading, HStack, Tag, Textarea, VStack } from '@navikt/ds-react'
import { ControlledInntektTextField } from '~shared/components/textField/ControlledInntektTextField'
import { useForm } from 'react-hook-form'
import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreFaktiskInntekt } from '~shared/api/etteroppgjoer'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { addResultatEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { maanedNavn } from '~utils/formatering/dato'
import { useAppDispatch } from '~store/Store'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'

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

export const FastsettFaktiskInntekt = ({
  forbehandlingId,
  faktiskInntekt,
}: {
  forbehandlingId: string
  faktiskInntekt?: FaktiskInntekt
}) => {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const { behandling } = useEtteroppgjoer()
  const dispatch = useAppDispatch()

  const erRedigerbar =
    behandling.status !== 'ATTESTERING' && enhetErSkrivbar(behandling.sak.enhet, innloggetSaksbehandler.skriveEnheter)

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
    lagreFaktiskInntektRequest({ forbehandlingId, faktiskInntekt }, (resultat) => {
      dispatch(addResultatEtteroppgjoer(resultat))
    })
  }

  return (
    <form
      onSubmit={handleSubmit((data) => submitFaktiskInntekt(fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt(data)))}
    >
      <VStack gap="4">
        <Heading size="large">Fastett faktisk inntekt</Heading>
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
            readOnly={!erRedigerbar}
          />
          <ControlledInntektTextField
            name="afp"
            control={control}
            label="Avtalefestet pensjon"
            readOnly={!erRedigerbar}
          />
          <ControlledInntektTextField
            name="naeringsinntekt"
            control={control}
            label="Næringsinntekt"
            readOnly={!erRedigerbar}
          />
          <ControlledInntektTextField
            name="utland"
            control={control}
            label="Inntekt fra utland"
            readOnly={!erRedigerbar}
          />
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
            readOnly={!erRedigerbar}
          />
        </Box>

        <div>
          <Button size="small" loading={isPending(lagreFaktiskInntektResult)}>
            Fastsett inntekt
          </Button>
        </div>

        {isFailureHandler({
          apiResult: lagreFaktiskInntektResult,
          errorMessage: 'Kunne ikke fastsette faktisk inntekt',
        })}
      </VStack>
    </form>
  )
}
