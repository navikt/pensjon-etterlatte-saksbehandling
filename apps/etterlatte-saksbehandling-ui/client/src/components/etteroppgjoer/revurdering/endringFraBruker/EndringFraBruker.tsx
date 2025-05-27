import { Button, Heading, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PencilIcon } from '@navikt/aksel-icons'
import { EndringFraBrukerVisning } from '~components/etteroppgjoer/revurdering/endringFraBruker/EndringFraBrukerVisning'
import { EndringFraBrukerSkjema } from '~components/etteroppgjoer/revurdering/endringFraBruker/EndringFraBrukerSkjema'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

export const EndringFraBruker = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const etteroppgjoer = useEtteroppgjoer()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [endringFraBrukerSkjemaErAapen, setEndringFraBrukerSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !etteroppgjoer.behandling.harMottattNyInformasjon
  )

  return (
    <VStack gap="4">
      <Heading size="large">Endring fra bruker</Heading>

      {endringFraBrukerSkjemaErAapen && erRedigerbar ? (
        <EndringFraBrukerSkjema
          behandling={behandling}
          setEndringFraBrukerSkjemaErAapen={setEndringFraBrukerSkjemaErAapen}
          erRedigerbar={erRedigerbar}
        />
      ) : (
        <VStack gap="4">
          <EndringFraBrukerVisning />
          {erRedigerbar && (
            <div>
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon aria-hidden />}
                onClick={() => setEndringFraBrukerSkjemaErAapen(true)}
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
