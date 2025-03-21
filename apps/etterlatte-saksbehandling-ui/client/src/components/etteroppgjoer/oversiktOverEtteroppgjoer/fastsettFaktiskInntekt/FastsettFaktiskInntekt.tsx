import { BodyShort, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { ControlledInntektTextField } from '~shared/components/textField/ControlledInntektTextField'
import { useForm } from 'react-hook-form'
import { FaktiskInntekt } from '~shared/types/Etteroppgjoer'
import { SumAvFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/SumAvFaktiskInntekt'

interface FastsettFaktiskInntektSkjema {
  loennsinntekt: string
  afp: string
  naeringsinntekt: string
  utland: string
}

export const FastsettFaktiskInntekt = () => {
  const { control, watch } = useForm<FastsettFaktiskInntektSkjema>({
    defaultValues: {
      loennsinntekt: '0',
      afp: '0',
      naeringsinntekt: '0',
      utland: '0',
    },
  })

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

  return (
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
    </VStack>
  )
}
