import React, { ReactNode, useEffect, useState } from 'react'
import { Container } from '~shared/styled'
import { Alert, BodyShort, Heading, Loader, Table, ToggleGroup } from '@navikt/ds-react'
import { ArchiveIcon, BellDotIcon } from '@navikt/aksel-icons'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { Grunnlagsendringshendelse, SakMedBehandlinger, STATUS_IRRELEVANT } from '~components/person/typer'
import Spinner from '~shared/Spinner'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserForSak } from '~shared/api/behandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import UhaandtertHendelse from '~components/person/uhaandtereHendelser/UhaandtertHendelse'
import { harAapenRevurdering } from '~components/person/hendelser/utils'
import { NyHendelseExpandableRow } from '~components/person/hendelser/NyHendelseExpandableRow'
import { hentStoettedeRevurderinger } from '~shared/api/revurdering'

enum HendelseValg {
  NYE = 'NYE',
  ARKIVERTE = 'ARKIVERTE',
}

export const Hendelser = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }): ReactNode => {
  const [hendelseValg, setHendelseValg] = useState<HendelseValg>(HendelseValg.NYE)

  const [hendelserResult, hentHendelser] = useApiCall(hentGrunnlagsendringshendelserForSak)
  const [stoettedeRevurderingerResult, stoettedeRevurderingerFetch] = useApiCall(hentStoettedeRevurderinger)

  const relevanteHendelser = (hendelser: Grunnlagsendringshendelse[]): Grunnlagsendringshendelse[] => {
    return hendelser.filter((h) => h.status !== STATUS_IRRELEVANT)
  }

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentHendelser(sakResult.data.sak.id)

      if (sakResult.data.sak.sakType && sakResult.data.behandlinger) {
        stoettedeRevurderingerFetch({ sakType: sakResult.data.sak.sakType })
      }
    }
  }, [])

  return mapResult(sakResult, {
    pending: <Loader />,
    error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
    success: ({ sak, behandlinger }) => (
      <Container>
        <Heading size="medium" spacing>
          Hendelser
        </Heading>

        <ToggleGroup
          defaultValue={HendelseValg.NYE}
          onChange={(val) => setHendelseValg(val as HendelseValg)}
          size="small"
        >
          <ToggleGroup.Item value={HendelseValg.NYE}>
            <BellDotIcon aria-hidden /> Nye hendelser
          </ToggleGroup.Item>
          <ToggleGroup.Item value={HendelseValg.ARKIVERTE}>
            <ArchiveIcon /> Arkiverte hendelser
          </ToggleGroup.Item>
        </ToggleGroup>

        {mapResult(hendelserResult, {
          pending: <Spinner visible label="Henter hendelser..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente hendelser'}</ApiErrorAlert>,
          success: ({ hendelser }) => (
            <>
              {hendelseValg === HendelseValg.NYE && (
                <>
                  <RevurderingInfo>
                    Nye hendelser kan kreve revurdering. Vurder derfor om hendelsen har konsekvens for ytelsen. Hvis
                    ikke kan du arkivere hendelsen.
                  </RevurderingInfo>

                  {!!relevanteHendelser(hendelser)?.length ? (
                    <HendelserTable>
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell />
                          <Table.HeaderCell scope="col">Hendelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {relevanteHendelser(hendelser).map((hendelse) => (
                          <NyHendelseExpandableRow
                            hendelse={hendelse}
                            sakType={sak.sakType}
                            behandlinger={behandlinger}
                            revurderinger={
                              isSuccess(stoettedeRevurderingerResult) ? stoettedeRevurderingerResult.data : []
                            }
                          />
                        ))}
                      </Table.Body>
                    </HendelserTable>
                  ) : (
                    <Alert variant="info" inline>
                      Ingen nye hendelser
                    </Alert>
                  )}
                </>
              )}
              {hendelseValg === HendelseValg.ARKIVERTE && <p>Arkiverte hendelser table</p>}
            </>
          ),
        })}
      </Container>
    ),
  })
}

const RevurderingInfo = styled(BodyShort)`
  max-width: 26rem;
  margin-top: 1rem;
`

const HendelserTable = styled(Table)`
  max-width: 60rem;
  min-width: 30rem;
`
