import { useEffect } from 'react'
import { Behandlingsliste } from './Behandlingsliste'
import styled from 'styled-components'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
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
import { hentSakMedBehandlnger } from '~shared/api/sak'
import { tagColors } from '~shared/Tags'

export const SakOversikt = ({ fnr }: { fnr: string }) => {
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)
  const [sakStatus, hentSak] = useApiCall(hentSakMedBehandlnger)

  useEffect(() => {
    hentSak(fnr)
  }, [])

  return (
    <GridContainer>
      <MainContent>
        {isFailure(sakStatus) && <Alert variant="error">{JSON.stringify(sakStatus.error)}</Alert>}
        {isSuccess(sakStatus) && (
          <>
            <Heading size="medium" spacing>
              Saknummer {sakStatus.data.sak.id}{' '}
              <Tag variant="success" size="medium">
                {formaterSakstype(sakStatus.data.sak.sakType)}
              </Tag>
              {sakStatus.data.sak.utenlandstilknytning ? (
                <Tag variant={tagColors[sakStatus.data.sak.utenlandstilknytning.type]} size="small">
                  {formaterEnumTilLesbarString(sakStatus.data.sak.utenlandstilknytning.type)}
                </Tag>
              ) : (
                <BodyShort>Du må velge en tilknytning for saken</BodyShort>
              )}
              <FlexRow justify="right">
                <OpprettKlage sakId={sakStatus.data.sak.id} />
                {sakStatus.data.sak.sakType === SakType.BARNEPENSJON && (
                  <ManueltOpphoerModal sakId={sakStatus.data.sak.id} behandlingliste={sakStatus.data.behandlinger} />
                )}
              </FlexRow>
            </Heading>

            <BodyShort spacing>Denne saken tilhører enhet {sakStatus.data.sak.enhet}.</BodyShort>
            <BodyShort spacing>
              <Link href={`/person/${fnr}/sak/${sakStatus.data.sak.id}/brev`}>
                Du finner brev tilhørende saken her <ExternalLinkIcon />
              </Link>
            </BodyShort>

            <hr />
            <Behandlingsliste behandlinger={sakStatus.data.behandlinger} sakId={sakStatus.data.sak.id} />

            {kanBrukeKlage ? <KlageListe sakId={sakStatus.data.sak.id} /> : null}
          </>
        )}
      </MainContent>

      <HendelseSidebar>
        {isPending(sakStatus) && <Spinner visible label="Forbereder hendelser ..." />}

        {isSuccess(sakStatus) && sakStatus.data.sak.id && (
          <RelevanteHendelser sak={sakStatus.data.sak} fnr={fnr} behandlingliste={sakStatus.data.behandlinger} />
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
