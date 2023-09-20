import { useEffect, useState } from 'react'
import { Behandlingsliste } from './behandlingsliste'
import styled from 'styled-components'
import { IBehandlingListe } from './typer'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
import { hentBehandlingerForPerson } from '~shared/api/behandling'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyShort, Heading, Link, Tag } from '@navikt/ds-react'
import { ISak } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formattering'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import OpprettGenerellBehandling from '~components/person/OpprettGenerellBehandling'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE, OpprettKlage } from '~components/person/OpprettKlage'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { KlageListe } from '~components/person/KlageListe'

export const FEATURE_TOGGLE_KAN_BRUKE_GENERELL_BEHANDLING = 'pensjon-etterlatte.kan-bruke-generell-behandling'
export const SakOversikt = ({ fnr }: { fnr: string }) => {
  const [sak, setSak] = useState<ISak>()
  const skalBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE)
  const [behandlingerStatus, hentBehandlinger] = useApiCall(hentBehandlingerForPerson)
  const kanBrukeGenerllBehandling = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_GENERELL_BEHANDLING, false)

  useEffect(() => {
    hentBehandlinger(fnr, (behandlinger: IBehandlingListe[]) => {
      setSak(behandlinger[0].sak)
    })
  }, [])

  return (
    <GridContainer>
      <MainContent>
        {isPending(behandlingerStatus) && <Spinner visible label={'Laster behandlinger ...'} />}

        {isFailure(behandlingerStatus) && <Alert variant={'error'}>{JSON.stringify(behandlingerStatus.error)}</Alert>}

        {isSuccess(behandlingerStatus) && (
          <>
            <Heading size={'medium'} spacing>
              Saknummer {sak!!.id}{' '}
              <Tag variant={'success'} size={'medium'}>
                {formaterSakstype(sak!!.sakType)}
              </Tag>
              <FlexRow justify={'right'}>
                <OpprettKlage sakId={sak!!.id} />
                <ManueltOpphoerModal sakId={sak!!.id} behandlingliste={behandlingerStatus.data[0].behandlinger} />
                {kanBrukeGenerllBehandling && <OpprettGenerellBehandling sakId={sak!!.id} />}
              </FlexRow>
            </Heading>

            <BodyShort spacing>Denne saken tilhører enhet {sak?.enhet}.</BodyShort>
            <BodyShort spacing>
              <Link href={`/person/${fnr}/sak/${sak?.id}/brev`}>
                Du finner brev tilhørende saken her <ExternalLinkIcon />
              </Link>
            </BodyShort>

            <hr />

            <Behandlingsliste behandlinger={behandlingerStatus.data[0].behandlinger} />
            {sak && skalBrukeKlage ? <KlageListe sakId={sak.id} /> : null}
          </>
        )}
      </MainContent>

      <HendelseSidebar>
        {isPending(behandlingerStatus) && <Spinner visible label={'Forbereder hendelser ...'} />}

        {isSuccess(behandlingerStatus) && sak?.id && (
          <RelevanteHendelser sak={sak} fnr={fnr} behandlingliste={behandlingerStatus.data[0].behandlinger} />
        )}
      </HendelseSidebar>
    </GridContainer>
  )
}

const MainContent = styled.div`
  flex: 1 0 auto;
  margin: 3em 1em;
`

const HendelseSidebar = styled.div`
  min-width: 40rem;
  border-left: 1px solid gray;
  padding: 3em 2rem;
  margin: 0 1em;
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    padding: 0.6em;
  }
`
