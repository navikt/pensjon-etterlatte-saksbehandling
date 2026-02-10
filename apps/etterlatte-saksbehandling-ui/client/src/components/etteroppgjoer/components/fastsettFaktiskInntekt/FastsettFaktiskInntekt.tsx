import { BodyShort, Button, Heading, Tag, VStack } from '@navikt/ds-react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { maanedNavn } from '~utils/formatering/dato'
import { FaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FaktiskInntektVisning } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektVisning'
import { PencilIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { FieldErrors } from 'react-hook-form'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'

interface Props {
  erRedigerbar: boolean
  setFastsettFaktiskInntektSkjemaErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
}

export const FastsettFaktiskInntekt = ({ erRedigerbar, setFastsettFaktiskInntektSkjemaErrors }: Props) => {
  const { forbehandling, faktiskInntekt } = useEtteroppgjoerForbehandling()

  const [faktiskInntektSkjemaErAapen, setFaktiskInntektSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !faktiskInntekt
  )

  return (
    <VStack gap="space-4">
      <Heading size="large">Fastsett faktisk inntekt</Heading>
      <BodyShort>Fastsett den faktiske inntekten for bruker i den innvilgede perioden.</BodyShort>
      <div>
        <Tag variant="neutral">
          {maanedNavn(forbehandling.innvilgetPeriode.fom)} - {maanedNavn(forbehandling.innvilgetPeriode.tom)}
        </Tag>
      </div>

      {faktiskInntektSkjemaErAapen && erRedigerbar ? (
        <FaktiskInntektSkjema
          setFaktiskInntektSkjemaErAapen={setFaktiskInntektSkjemaErAapen}
          setFastsettFaktiskInntektSkjemaErrors={setFastsettFaktiskInntektSkjemaErrors}
        />
      ) : (
        <VStack gap="space-4">
          <FaktiskInntektVisning />
          {erRedigerbar && (
            <div>
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon aria-hidden />}
                onClick={() => setFaktiskInntektSkjemaErAapen(true)}
              >
                Rediger
              </Button>
            </div>
          )}
        </VStack>
      )}
    </VStack>
  )
}
