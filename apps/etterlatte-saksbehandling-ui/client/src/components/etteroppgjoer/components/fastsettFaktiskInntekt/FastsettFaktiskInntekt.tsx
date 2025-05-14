import { BodyShort, Button, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { maanedNavn } from '~utils/formatering/dato'
import { FaktiskInntektSkjema } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FaktiskInntektVisning } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FaktiskInntektVisning'
import { PencilIcon } from '@navikt/aksel-icons'

interface Props {
  erRedigerbar: boolean
  faktiskInntektSkjemaErAapen: boolean
  setFaktiskInntektSkjemaErAapen: (erAapen: boolean) => void
  setFastsettInntektSkjemaErSkittent?: (erSkittent: boolean) => void
}

export const FastsettFaktiskInntekt = ({
  erRedigerbar,
  faktiskInntektSkjemaErAapen,
  setFaktiskInntektSkjemaErAapen,
  setFastsettInntektSkjemaErSkittent,
}: Props) => {
  const { behandling } = useEtteroppgjoer()

  return (
    <VStack gap="4">
      <Heading size="large">Fastsett faktisk inntekt</Heading>
      <HStack gap="2" align="center">
        <BodyShort>Fastsett den faktiske inntekten for bruker i den innvilgede perioden.</BodyShort>
      </HStack>
      <div>
        <Tag variant="neutral">
          {maanedNavn(behandling.innvilgetPeriode.fom)} - {maanedNavn(behandling.innvilgetPeriode.tom)}
        </Tag>
      </div>

      {faktiskInntektSkjemaErAapen && erRedigerbar ? (
        <FaktiskInntektSkjema
          setFaktiskInntektSkjemaErAapen={setFaktiskInntektSkjemaErAapen}
          setFastsettInntektSkjemaErSkittent={setFastsettInntektSkjemaErSkittent}
        />
      ) : (
        <VStack gap="4">
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
