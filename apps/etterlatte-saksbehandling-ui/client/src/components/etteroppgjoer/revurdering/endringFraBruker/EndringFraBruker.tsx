import { Button, Heading, VStack } from '@navikt/ds-react'
import { useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PencilIcon } from '@navikt/aksel-icons'
import { EndringFraBrukerVisning } from '~components/etteroppgjoer/revurdering/endringFraBruker/EndringFraBrukerVisning'
import { EndringFraBrukerSkjema } from '~components/etteroppgjoer/revurdering/endringFraBruker/EndringFraBrukerSkjema'

export const EndringFraBruker = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [endringFraBrukerSkjemaErAapen, setEndringFraBrukerSkjemaErAapen] = useState<boolean>(false)

  return (
    <VStack gap="4" paddingInline="16" paddingBlock="16 4">
      <Heading size="large">Endring fra bruker</Heading>

      {endringFraBrukerSkjemaErAapen && erRedigerbar ? (
        <EndringFraBrukerVisning />
      ) : (
        <VStack gap="4">
          <EndringFraBrukerSkjema
            behandling={behandling}
            setEndringFraBrukerSkjemaErAapen={setEndringFraBrukerSkjemaErAapen}
            erRedigerbar={erRedigerbar}
          />
          {erRedigerbar && (
            <div>
              <Button size="small" variant="secondary" icon={<PencilIcon aria-hidden />}>
                Rediger
              </Button>
            </div>
          )}
        </VStack>
      )}
    </VStack>
  )
}
