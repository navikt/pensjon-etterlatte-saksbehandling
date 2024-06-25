import styled from 'styled-components'
import { Alert, BodyLong, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevutfallOgEtterbetalingApi } from '~shared/api/behandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { BrevutfallSkjema } from '~components/behandling/brevutfall/BrevutfallSkjema'
import { BrevutfallVisning } from '~components/behandling/brevutfall/BrevutfallVisning'
import Spinner from '~shared/Spinner'
import { MapApiResult } from '~shared/components/MapApiResult'
import { SakType } from '~shared/types/sak'
import { useAppDispatch } from '~store/Store'
import { IBehandlingReducer, updateBrevutfallOgEtterbetaling } from '~store/reducers/BehandlingReducer'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { SkalSendeBrev } from '~components/behandling/brevutfall/SkalSendeBrev'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export interface BrevutfallOgEtterbetaling {
  opphoer?: boolean | null
  etterbetaling?: Etterbetaling | null
  brevutfall: Brevutfall
}

export interface Brevutfall {
  aldersgruppe?: Aldersgruppe | null
  feilutbetaling?: Feilutbetaling | null
}

export enum Aldersgruppe {
  OVER_18 = 'OVER_18',
  UNDER_18 = 'UNDER_18',
  IKKE_VALGT = 'IKKE_VALGT',
}

export interface Feilutbetaling {
  valg: FeilutbetalingValg
  kommentar?: string | null
}

export enum FeilutbetalingValg {
  NEI = 'NEI',
  JA_VARSEL = 'JA_VARSEL',
  JA_INGEN_TK = 'JA_INGEN_TK',
}

export interface Etterbetaling {
  datoFom?: string | null
  datoTom?: string | null
  inneholderKrav: boolean | null
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

export const Brevutfall = (props: { behandling: IBehandlingReducer; resetBrevutfallvalidering: () => void }) => {
  const behandling = props.behandling
  const behandlingErOpphoer = behandling.vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.IKKE_OPPFYLT
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const dispatch = useAppDispatch()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [brevutfallOgEtterbetaling, setBrevutfallOgEtterbetaling] = useState<BrevutfallOgEtterbetaling>(
    initialBrevutfallOgEtterbetaling(behandling.sakType)
  )
  const [hentBrevutfallOgEtterbetalingResult, hentBrevutfallOgEtterbetalingRequest] = useApiCall(
    hentBrevutfallOgEtterbetalingApi
  )
  const [visSkjema, setVisSkjema] = useState(redigerbar)

  const hentBrevutfall = () => {
    hentBrevutfallOgEtterbetalingRequest(behandling.id, (brevutfall: BrevutfallOgEtterbetaling | null) => {
      if (brevutfall) {
        setBrevutfallOgEtterbetaling(brevutfall)
        dispatch(updateBrevutfallOgEtterbetaling(brevutfall))
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
  const erIkkeFoerstegangsbehandling = behandling.behandlingType !== IBehandlingsType.FØRSTEGANGSBEHANDLING

  return behandling.sendeBrev ? (
    <BrevutfallContent id="brevutfall">
      {erIkkeFoerstegangsbehandling && <SkalSendeBrev behandling={behandling} behandlingRedigerbart={redigerbar} />}
      <Heading level="2" size="small" spacing>
        Valg av utfall i brev
      </Heading>
      <BodyLong spacing>
        Her velger du hvilket utfall som gjelder slik at det blir riktig informasjon i brevet.
      </BodyLong>

      <MapApiResult
        result={hentBrevutfallOgEtterbetalingResult}
        mapInitialOrPending={<Spinner visible={true} label="Henter brevutfall .." />}
        mapError={(apiError) => <Alert variant="error">{apiError.detail}</Alert>}
        mapSuccess={() =>
          visSkjema ? (
            <BrevutfallSkjema
              behandlingErOpphoer={behandlingErOpphoer}
              resetBrevutfallvalidering={props.resetBrevutfallvalidering}
              behandling={behandling}
              brevutfallOgEtterbetaling={brevutfallOgEtterbetaling}
              setBrevutfallOgEtterbetaling={setBrevutfallOgEtterbetaling}
              setVisSkjema={setVisSkjema}
              onAvbryt={hentBrevutfall}
            />
          ) : (
            <BrevutfallVisning
              behandlingErOpphoer={behandlingErOpphoer}
              redigerbar={redigerbar}
              brevutfallOgEtterbetaling={brevutfallOgEtterbetaling}
              sakType={behandling.sakType}
              setVisSkjema={setVisSkjema}
            />
          )
        }
      />
    </BrevutfallContent>
  ) : (
    <>
      {erIkkeFoerstegangsbehandling && <SkalSendeBrev behandling={behandling} behandlingRedigerbart={redigerbar} />}
      <InfoAlert variant="info">Det sendes ikke vedtaksbrev for denne behandlingen.</InfoAlert>
    </>
  )
}

const BrevutfallContent = styled.div`
  margin-top: 4rem;
  margin-bottom: 4rem;
  max-width: 500px;
`
const InfoAlert = styled(Alert)`
  margin-top: 2rem;
  margin-bottom: 4rem;
  max-width: fit-content;
`
