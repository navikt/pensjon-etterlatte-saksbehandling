import { BodyShort, Button, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { EtteroppgjoerBehandlingStatus } from '~shared/types/Etteroppgjoer'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { maanedNavn } from '~utils/formatering/dato'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useState } from 'react'
import { FaktiskInntektSkjema } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/FaktiskInntektSkjema'
import { FaktiskInntektVisning } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/FaktiskInntektVisning'
import { PencilIcon } from '@navikt/aksel-icons'

export const FastsettFaktiskInntekt = () => {
  const [redigerFaktiskInntekt, setRedigerFaktiskInntekt] = useState<boolean>(false)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const { behandling } = useEtteroppgjoer()

  const erRedigerbar =
    (behandling.status == EtteroppgjoerBehandlingStatus.OPPRETTET ||
      behandling.status == EtteroppgjoerBehandlingStatus.BEREGNET) &&
    enhetErSkrivbar(behandling.sak.enhet, innloggetSaksbehandler.skriveEnheter)

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

      {redigerFaktiskInntekt && erRedigerbar ? (
        <FaktiskInntektSkjema setRedigerFaktiskInntekt={setRedigerFaktiskInntekt} />
      ) : (
        <VStack gap="4">
          <FaktiskInntektVisning />
          <div>
            <Button
              size="small"
              variant="secondary"
              icon={<PencilIcon aria-hidden />}
              onClick={() => setRedigerFaktiskInntekt(true)}
            >
              Rediger
            </Button>
          </div>
        </VStack>
      )}
    </VStack>
  )
}
