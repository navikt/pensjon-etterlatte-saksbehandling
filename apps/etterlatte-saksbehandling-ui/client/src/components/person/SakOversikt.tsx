import { Behandlingsliste } from './Behandlingsliste'
import styled from 'styled-components'
import { FlexRow, GridContainer } from '~shared/styled'
import Spinner from '~shared/Spinner'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { Alert, BodyShort, Heading, HStack, Tag } from '@navikt/ds-react'
import { formaterEnumTilLesbarString, formaterSakstype } from '~utils/formattering'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE, OpprettKlage } from '~components/person/OpprettKlage'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { KlageListe } from '~components/person/KlageListe'
import { tagColors } from '~shared/Tags'
import { SakMedBehandlinger } from '~components/person/typer'
import { mapApiResult, Result } from '~shared/api/apiUtils'

export const SakOversikt = ({ sakStatus }: { sakStatus: Result<SakMedBehandlinger>; fnr: string }) => {
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  return (
    <GridContainer>
      {mapApiResult(
        sakStatus,
        <Spinner visible={true} label="Henter sak og behandlinger" />,
        (error) => (
          <Alert variant="error">{JSON.stringify(error)}</Alert>
        ),
        (sakOgBehandlinger) => (
          <>
            <MainContent>
              <Heading size="medium" spacing>
                <HStack gap="2">
                  Saknummer {sakOgBehandlinger.sak.id}{' '}
                  <Tag variant="success" size="medium">
                    {formaterSakstype(sakOgBehandlinger.sak.sakType)}
                  </Tag>
                  {sakOgBehandlinger.sak.utlandstilknytning && (
                    <Tag variant={tagColors[sakOgBehandlinger.sak.utlandstilknytning.type]} size="medium">
                      {formaterEnumTilLesbarString(sakOgBehandlinger.sak.utlandstilknytning.type)}
                    </Tag>
                  )}
                </HStack>

                <FlexRow justify="right">
                  <OpprettKlage sakId={sakOgBehandlinger.sak.id} />
                </FlexRow>
              </Heading>

              <BodyShort spacing>Denne saken tilhører enhet {sakOgBehandlinger.sak.enhet}.</BodyShort>

              <hr />
              <Behandlingsliste behandlinger={sakOgBehandlinger.behandlinger} sakId={sakOgBehandlinger.sak.id} />

              {kanBrukeKlage ? <KlageListe sakId={sakOgBehandlinger.sak.id} /> : null}
            </MainContent>
            <HendelseSidebar>
              <RelevanteHendelser sak={sakOgBehandlinger.sak} behandlingliste={sakOgBehandlinger.behandlinger} />
            </HendelseSidebar>
          </>
        )
      )}
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
