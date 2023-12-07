import styled from 'styled-components'
import { Alert, BodyLong, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevutfall } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { BrevutfallSkjema } from '~components/behandling/brevutfall/BrevutfallSkjema'
import { BrevutfallVisning } from '~components/behandling/brevutfall/BrevutfallVisning'
import { isFailure, isPendingOrInitial, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

export interface Brevutfall {
  etterbetaling?: Etterbetaling | null
  aldersgruppe?: Aldersgruppe | null
}

export enum Aldersgruppe {
  OVER_18 = 'OVER_18',
  UNDER_18 = 'UNDER_18',
}

export interface Etterbetaling {
  fom?: Date | null
  tom?: Date | null
}

export const Brevutfall = (props: { behandling: IDetaljertBehandling }) => {
  const behandling = props.behandling
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const [brevutfall, setBrevutfall] = useState<Brevutfall>({})
  const [hentBrevutfallResultat, hentBrevutfallRequest] = useApiCall(hentBrevutfall)
  const [visSkjema, setVisSkjema] = useState(redigerbar)

  useEffect(() => {
    hentBrevutfallRequest(behandling.id, (brevutfall: Brevutfall | null) => {
      if (brevutfall) {
        setBrevutfall(brevutfall)
        setVisSkjema(false)
      } else {
        setVisSkjema(true)
      }
    })
  }, [])

  return (
    <BrevutfallContent>
      <Heading size="medium" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

      {isSuccess(hentBrevutfallResultat) && (
        <VStack gap="8">
          {visSkjema ? (
            <BrevutfallSkjema
              behandling={behandling}
              brevutfall={brevutfall}
              setBrevutfall={setBrevutfall}
              setVisSkjema={setVisSkjema}
            />
          ) : (
            <BrevutfallVisning redigerbar={redigerbar} brevutfall={brevutfall} setVisSkjema={setVisSkjema} />
          )}
        </VStack>
      )}
      {isPendingOrInitial(hentBrevutfallResultat) && <Spinner visible={true} label="Henter brevutfall" />}
      {isFailure(hentBrevutfallResultat) && (
        <HStack>
          <Alert variant="error">{hentBrevutfallResultat.error.detail}</Alert>
        </HStack>
      )}
    </BrevutfallContent>
  )
}

const BrevutfallContent = styled.div`
  margin-top: 4em;
  margin-bottom: 2em;
`
