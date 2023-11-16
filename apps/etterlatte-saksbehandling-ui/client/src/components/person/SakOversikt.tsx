import { useEffect } from 'react'
import { Behandlingsliste } from './behandlingsliste'
import styled from 'styled-components'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
import { hentBehandlingerForPerson } from '~shared/api/behandling'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyShort, Heading, Link, Tag } from '@navikt/ds-react'
import { SakType } from '~shared/types/sak'
import { formaterEnumTilLesbarString, formaterSakstype } from '~utils/formattering'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE, OpprettKlage } from '~components/person/OpprettKlage'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { KlageListe } from '~components/person/KlageListe'
import { hentSakMedUtenlandstilknytning } from '~shared/api/sak'
import { tagColors } from '~shared/Tags'

export const SakOversikt = ({ fnr }: { fnr: string }) => {
  const skalBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)
  const [behandlingerStatus, hentBehandlinger] = useApiCall(hentBehandlingerForPerson)
  const [sakStatus, hentSak] = useApiCall(hentSakMedUtenlandstilknytning)

  useEffect(() => {
    hentSak(fnr)
    hentBehandlinger(fnr)
  }, [])

  return (
    <GridContainer>
      <MainContent>
        {isPending(behandlingerStatus) && <Spinner visible label="Laster behandlinger ..." />}

        {isFailure(behandlingerStatus) && <Alert variant="error">{JSON.stringify(behandlingerStatus.error)}</Alert>}
        {isFailure(sakStatus) && <Alert variant="error">{JSON.stringify(sakStatus.error)}</Alert>}
        {isSuccess(sakStatus) && (
          <>
            <Heading size="medium" spacing>
              Saknummer {sakStatus.data.id}{' '}
              <Tag variant="success" size="medium">
                {formaterSakstype(sakStatus.data.sakType)}
              </Tag>
              {sakStatus.data.utenlandstilknytning ? (
                <Tag variant={tagColors[sakStatus.data.utenlandstilknytning.type]} size="small">
                  {formaterEnumTilLesbarString(sakStatus.data.utenlandstilknytning.type)}
                </Tag>
              ) : (
                <BodyShort>Du må velge en tilknytning for saken</BodyShort>
              )}
              <FlexRow justify="right">
                <OpprettKlage sakId={sakStatus.data.id} />
                {sakStatus.data.sakType == SakType.BARNEPENSJON && isSuccess(behandlingerStatus) && (
                  <ManueltOpphoerModal
                    sakId={sakStatus.data.id}
                    behandlingliste={behandlingerStatus.data[0].behandlinger}
                  />
                )}
              </FlexRow>
            </Heading>

            <BodyShort spacing>Denne saken tilhører enhet {sakStatus.data.enhet}.</BodyShort>
            <BodyShort spacing>
              <Link href={`/person/${fnr}/sak/${sakStatus.data?.id}/brev`}>
                Du finner brev tilhørende saken her <ExternalLinkIcon />
              </Link>
            </BodyShort>

            <hr />

            {isSuccess(behandlingerStatus) && (
              <Behandlingsliste behandlinger={behandlingerStatus.data[0].behandlinger} sakId={sakStatus.data.id} />
            )}

            {skalBrukeKlage ? <KlageListe sakId={sakStatus.data.id} /> : null}
          </>
        )}
      </MainContent>

      <HendelseSidebar>
        {isPending(behandlingerStatus) && <Spinner visible label="Forbereder hendelser ..." />}

        {isSuccess(behandlingerStatus) && isSuccess(sakStatus) && sakStatus.data?.id && (
          <RelevanteHendelser
            sak={sakStatus.data}
            fnr={fnr}
            behandlingliste={behandlingerStatus.data[0].behandlinger}
          />
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
