import styled from 'styled-components'
import { Alert, BodyLong, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevoppsett } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { BrevoppsettSkjema } from '~components/behandling/brevoppsett/BrevoppsettSkjema'
import { BrevoppsettVisning } from '~components/behandling/brevoppsett/BrevoppsettVisning'
import { isFailure, isPendingOrInitial, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

export interface Brevoppsett {
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

export const Brevoppsett = (props: { behandling: IDetaljertBehandling }) => {
  const behandling = props.behandling
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const [brevoppsett, setBrevoppsett] = useState<Brevoppsett>({})
  const [hentBrevoppsettResultat, hentBrevoppsettRequest] = useApiCall(hentBrevoppsett)
  const [visSkjema, setVisSkjema] = useState(redigerbar)

  useEffect(() => {
    hentBrevoppsettRequest(behandling.id, (brevoppsett: Brevoppsett | null) => {
      if (brevoppsett) {
        setBrevoppsett(brevoppsett)
        setVisSkjema(false)
      } else {
        setVisSkjema(true)
      }
    })
  }, [])

  return (
    <BrevoppsettContent>
      <Heading size="medium" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

      {isSuccess(hentBrevoppsettResultat) && (
        <VStack gap="8">
          {visSkjema ? (
            <BrevoppsettSkjema
              behandling={behandling}
              brevoppsett={brevoppsett}
              setBrevoppsett={setBrevoppsett}
              setVisSkjema={setVisSkjema}
            />
          ) : (
            <BrevoppsettVisning redigerbar={redigerbar} brevoppsett={brevoppsett} setVisSkjema={setVisSkjema} />
          )}
        </VStack>
      )}
      {isPendingOrInitial(hentBrevoppsettResultat) && <Spinner visible={true} label="Henter brevoppsett" />}
      {isFailure(hentBrevoppsettResultat) && (
        <HStack>
          <Alert variant="error">{hentBrevoppsettResultat.error.detail}</Alert>
        </HStack>
      )}
    </BrevoppsettContent>
  )
}

const BrevoppsettContent = styled.div`
  margin-top: 4em;
  margin-bottom: 2em;
`
