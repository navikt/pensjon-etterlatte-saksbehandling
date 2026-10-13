import { Button, Heading, VStack } from '@navikt/ds-react'
import { useBehandlingRedigerbarhet } from '~components/behandling/useBehandlingRedigerbarhet'
import { Dispatch, SetStateAction, useState } from 'react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { PencilIcon } from '@navikt/aksel-icons'
import { InformasjonFraBrukerVisning } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerVisning'
import { InformasjonFraBrukerSkjema } from '~components/etteroppgjoer/revurdering/informasjonFraBruker/InformasjonFraBrukerSkjema'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { FieldErrors } from 'react-hook-form'
import { IInformasjonFraBruker } from '~shared/types/EtteroppgjoerForbehandling'

interface Props {
  behandling: IDetaljertBehandling
  setInformasjonFraBrukerSkjemaErrors: (errors: FieldErrors<IInformasjonFraBruker> | undefined) => void
  setValideringFeilmedling: Dispatch<SetStateAction<string>>
}

export const InformasjonFraBruker = ({
  behandling,
  setInformasjonFraBrukerSkjemaErrors,
  setValideringFeilmedling,
}: Props) => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  const { redigerbar } = useBehandlingRedigerbarhet(behandling)

  const [informasjonFraBrukerSkjemaErAapen, setInformasjonFraBrukerSkjemaErAapen] = useState<boolean>(
    redigerbar && !forbehandling.harMottattNyInformasjon
  )

  return (
    <VStack gap="space-16">
      <Heading size="medium" level="2">
        Er det ny informasjon i saken?
      </Heading>
      {informasjonFraBrukerSkjemaErAapen && redigerbar ? (
        <InformasjonFraBrukerSkjema
          behandling={behandling}
          setInformasjonFraBrukerSkjemaErAapen={setInformasjonFraBrukerSkjemaErAapen}
          erRedigerbar={redigerbar}
          setInformasjonFraBrukerSkjemaErrors={setInformasjonFraBrukerSkjemaErrors}
          setValideringFeilmedling={setValideringFeilmedling}
        />
      ) : (
        <VStack gap="space-16">
          <InformasjonFraBrukerVisning />
          {redigerbar && (
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
