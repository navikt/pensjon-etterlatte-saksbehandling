import { BodyShort, Button, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { ControlledInntektTextField } from '~shared/components/textField/ControlledInntektTextField'
import { useForm } from 'react-hook-form'
import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/SumAvFaktiskInntekt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreFaktiskInntekt } from '~shared/api/etteroppgjoer'
import { isPending } from '~shared/api/apiUtils'

const fastsettFaktiskInntektSkjemaValuesTilFaktiskInntekt = ({
  loennsinntekt,
  afp,
  naeringsinntekt,
  utland,
}: FastsettFaktiskInntektSkjema): FaktiskInntekt => {
  return {
    loennsinntekt: Number(loennsinntekt.replace(/[^0-9.]/g, '')),
    afp: Number(afp.replace(/[^0-9.]/g, '')),
    naeringsinntekt: Number(naeringsinntekt.replace(/[^0-9.]/g, '')),
    utland: Number(utland.replace(/[^0-9.]/g, '')),
  }
}

interface FastsettFaktiskInntektSkjema {
  loennsinntekt: string
  afp: string
  naeringsinntekt: string
  utland: string
}

export const FastsettFaktiskInntekt = ({ forbehandlingId }: { forbehandlingId: string }) => {
  const [lagreFaktiskInntektResult, lagreFaktiskInntektRequest] = useApiCall(lagreFaktiskInntekt)

  const { control, watch, handleSubmit } = useForm<FastsettFaktiskInntektSkjema>({
    defaultValues: {
      loennsinntekt: '0',
      afp: '0',
      naeringsinntekt: '0',
      utland: '0',
    },
  })

  const submitFaktiskInntekt = (faktiskInntekt: FaktiskInntekt) => {
    lagreFaktiskInntektRequest({ forbehandlingId, faktiskInntekt }, () => {
      //  TODO: her må det gjøre noe snacks for å indikere om at det er lagret, og oppdatere Redux state for videre visning av resultat på forbehandling
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
          {/* TODO: skal denne være dynamisk? Eller er den alltid "april - desember"? */}
          <Tag variant="neutral">April - Desember</Tag>
        </HStack>

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

        <div>
          <Button loading={isPending(lagreFaktiskInntektResult)}>Fastsett inntekt</Button>
        </div>
      </VStack>
    </form>
  )
}
