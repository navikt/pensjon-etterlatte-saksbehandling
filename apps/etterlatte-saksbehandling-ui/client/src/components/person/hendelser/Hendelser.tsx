import React, { ReactNode, useEffect, useState } from 'react'
import { Container } from '~shared/styled'
import { Alert, BodyShort, Heading, Loader, Table, ToggleGroup } from '@navikt/ds-react'
import { ArchiveIcon, BellDotIcon } from '@navikt/aksel-icons'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import {
  Grunnlagsendringshendelse,
  HISTORISK_REVURDERING,
  SakMedBehandlinger,
  STATUS_IRRELEVANT,
} from '~components/person/typer'
import Spinner from '~shared/Spinner'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserForSak } from '~shared/api/behandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { NyHendelseExpandableRow } from '~components/person/hendelser/NyHendelseExpandableRow'
import { hentStoettedeRevurderinger } from '~shared/api/revurdering'
import { LukketHendelseExpandableRow } from '~components/person/hendelser/LukketHendelseExpandableRow'

enum HendelseValg {
  NYE = 'NYE',
  LUKKEDE = 'LUKKEDE',
}

export const Hendelser = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }): ReactNode => {
  const [hendelseValg, setHendelseValg] = useState<HendelseValg>(HendelseValg.NYE)

  const [hendelserResult, hentHendelser] = useApiCall(hentGrunnlagsendringshendelserForSak)
  const [stoettedeRevurderingerResult, stoettedeRevurderingerFetch] = useApiCall(hentStoettedeRevurderinger)

  const relevanteHendelser = (hendelser: Grunnlagsendringshendelse[]): Grunnlagsendringshendelse[] => {
    return hendelser.filter((hendelse) => hendelse.status !== STATUS_IRRELEVANT)
  }

  const lukkedeHendelser = (hendelser: Grunnlagsendringshendelse[]): Grunnlagsendringshendelse[] => {
    return hendelser.filter((hendelse) => [STATUS_IRRELEVANT, HISTORISK_REVURDERING].includes(hendelse.status))
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
          <ToggleGroup.Item value={HendelseValg.LUKKEDE}>
            <ArchiveIcon aria-hidden /> Lukkede hendelser
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
                    ikke kan du lukke hendelsen.
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
                            key={hendelse.id}
                            hendelse={hendelse}
                            sak={sak}
                            behandlinger={behandlinger}
                            revurderinger={
                              isSuccess(stoettedeRevurderingerResult) ? stoettedeRevurderingerResult.data : []
                            }
                          />
                        ))}
                      </Table.Body>
                    </HendelserTable>
                  ) : (
                    <IngenHendelserAlert variant="info" inline>
                      Ingen nye hendelser
                    </IngenHendelserAlert>
                  )}
                </>
              )}
              {hendelseValg === HendelseValg.LUKKEDE && (
                <>
                  {!!lukkedeHendelser(hendelser)?.length ? (
                    <HendelserTable>
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell />
                          <Table.HeaderCell scope="col">Hendelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {lukkedeHendelser(hendelser).map((hendelse) => (
                          <LukketHendelseExpandableRow key={hendelse.id} sakType={sak.sakType} hendelse={hendelse} />
                        ))}
                      </Table.Body>
                    </HendelserTable>
                  ) : (
                    <IngenHendelserAlert variant="info" inline>
                      Ingen lukkede hendelser
                    </IngenHendelserAlert>
                  )}
                </>
              )}
            </>
          ),
        })}
      </Container>
    ),
  })
}

const RevurderingInfo = styled(BodyShort)`
  max-width: 32rem;
  margin-top: 1rem;
`

const HendelserTable = styled(Table)`
  margin-top: 1rem;
  max-width: 60rem;
  min-width: 30rem;
`

const IngenHendelserAlert = styled(Alert)`
  margin-top: 1rem;
`
