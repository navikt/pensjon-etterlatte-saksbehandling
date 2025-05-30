import { Button, Heading, VStack } from '@navikt/ds-react'
import { Dispatch, SetStateAction, useState } from 'react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PencilIcon } from '@navikt/aksel-icons'
import { InformasjonFraBrukerVisning } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerVisning'
import { InformasjonFraBrukerSkjema } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerSkjema'
import { useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'

export const INFORMASJON_FRA_BRUKER_ID = 'informasjon-fra-bruker'

export const InformasjonFraBruker = ({
  behandling,
  setValiderSkjema,
}: {
  behandling: IDetaljertBehandling
  setValiderSkjema: Dispatch<SetStateAction<() => void>>
}) => {
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
      <Heading id={INFORMASJON_FRA_BRUKER_ID} size="large">
        Informasjon fra bruker
      </Heading>

      {informasjonFraBrukerSkjemaErAapen && erRedigerbar ? (
        <InformasjonFraBrukerSkjema
          behandling={behandling}
          setValiderSkjema={setValiderSkjema}
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
