import { Button, Heading, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PencilIcon } from '@navikt/aksel-icons'
import { InformasjonFraBrukerVisning } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerVisning'
import { InformasjonFraBrukerSkjema } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerSkjema'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

interface Props {
  behandling: IDetaljertBehandling
}

export const InformasjonFraBruker = ({ behandling }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const etteroppgjoer = useEtteroppgjoer()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [informasjonFraBrukerSkjemaErAapen, setInformasjonFraBrukerSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !etteroppgjoer.behandling.harMottattNyInformasjon
  )

  return (
    <VStack gap="4">
      <Heading size="large">Informasjon fra bruker</Heading>

      {informasjonFraBrukerSkjemaErAapen && erRedigerbar ? (
        <InformasjonFraBrukerSkjema
          behandling={behandling}
          setInformasjonFraBrukerSkjemaErAapen={setInformasjonFraBrukerSkjemaErAapen}
          erRedigerbar={erRedigerbar}
        />
      ) : (
        <VStack gap="4">
          <InformasjonFraBrukerVisning />
          {erRedigerbar && (
            <div>
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon aria-hidden />}
                onClick={() => setInformasjonFraBrukerSkjemaErAapen(true)}
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
