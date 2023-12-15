import styled from 'styled-components'
import { Alert, BodyLong, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevutfallApi } from '~shared/api/behandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { BrevutfallSkjema } from '~components/behandling/brevutfall/BrevutfallSkjema'
import { BrevutfallVisning } from '~components/behandling/brevutfall/BrevutfallVisning'
import Spinner from '~shared/Spinner'
import { MapApiResult } from '~shared/components/MapApiResult'
import { SakType } from '~shared/types/sak'
import { useAppDispatch } from '~store/Store'
import { oppdaterBrevutfallOgEtterbetaling } from '~store/reducers/BehandlingReducer'

export interface IBrevutfallOgEtterbetaling {
  etterbetaling?: IEtterbetaling | null
  brevutfall: IBrevutfall
}

export interface IBrevutfall {
  aldersgruppe?: Aldersgruppe | null
}

export enum Aldersgruppe {
  OVER_18 = 'OVER_18',
  UNDER_18 = 'UNDER_18',
  IKKE_VALGT = 'IKKE_VALGT',
}

export interface IEtterbetaling {
  datoFom?: string | null
  datoTom?: string | null
}

const initialBrevutfallOgEtterbetaling = (saktype: SakType) => {
  switch (saktype) {
    case SakType.BARNEPENSJON:
      return {
        brevutfall: {
          aldersgruppe: Aldersgruppe.IKKE_VALGT,
        },
      }
    case SakType.OMSTILLINGSSTOENAD:
      return {
        brevutfall: {},
      }
  }
}

export const Brevutfall = (props: { behandling: IDetaljertBehandling }) => {
  const behandling = props.behandling
  const dispatch = useAppDispatch()
  const redigerbar = behandlingErRedigerbar(behandling.status)
  const [brevutfallOgEtterbetaling, setBrevutfallOgEtterbetaling] = useState<IBrevutfallOgEtterbetaling>(
    initialBrevutfallOgEtterbetaling(behandling.sakType)
  )
  const [hentBrevutfallResult, hentBrevutfallRequest] = useApiCall(hentBrevutfallApi)
  const [visSkjema, setVisSkjema] = useState(redigerbar)

  const hentBrevutfall = () => {
    hentBrevutfallRequest(behandling.id, (brevutfall: IBrevutfallOgEtterbetaling | null) => {
      if (brevutfall) {
        dispatch(oppdaterBrevutfallOgEtterbetaling(brevutfall))
        setBrevutfallOgEtterbetaling(brevutfall)
        setVisSkjema(false)
      } else {
        setBrevutfallOgEtterbetaling(initialBrevutfallOgEtterbetaling(behandling.sakType))
        if (redigerbar) setVisSkjema(true)
      }
    })
  }

  useEffect(() => {
    hentBrevutfall()
  }, [behandling.id])

  return (
    <BrevutfallContent id="brevutfall">
      <Heading size="medium" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

      <MapApiResult
        result={hentBrevutfallResult}
        mapInitialOrPending={<Spinner visible={true} label="Henter brevutfall .." />}
        mapError={(apiError) => <Alert variant="error">{apiError.detail}</Alert>}
        mapSuccess={() =>
          visSkjema ? (
            <BrevutfallSkjema
              behandling={behandling}
              brevutfallOgEtterbetaling={brevutfallOgEtterbetaling}
              setBrevutfallOgEtterbetaling={setBrevutfallOgEtterbetaling}
              setVisSkjema={setVisSkjema}
              onAvbryt={hentBrevutfall}
            />
          ) : (
            <BrevutfallVisning
              redigerbar={redigerbar}
              brevutfallOgEtterbetaling={brevutfallOgEtterbetaling}
              sakType={behandling.sakType}
              setVisSkjema={setVisSkjema}
            />
          )
        }
      />
    </BrevutfallContent>
  )
}

const BrevutfallContent = styled.div`
  margin-top: 4em;
  margin-bottom: 2em;
  max-width: 500px;
`
