import React, { ReactNode, useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Heading, Loader, Table, ToggleGroup } from '@navikt/ds-react'
import { ArchiveIcon, BellDotIcon } from '@navikt/aksel-icons'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { Grunnlagsendringshendelse, GrunnlagsendringStatus, SakMedBehandlinger } from '~components/person/typer'
import Spinner from '~shared/Spinner'
import { SakIkkeFunnet } from '~components/person/sakOgBehandling/SakIkkeFunnet'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserForSak } from '~shared/api/behandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { NyHendelseExpandableRow } from '~components/person/hendelser/NyHendelseExpandableRow'
import { hentStoettedeRevurderinger } from '~shared/api/revurdering'
import { ArkivertHendelseExpandableRow } from '~components/person/hendelser/ArkivertHendelseExpandableRow'

enum HendelseValg {
  NYE = 'NYE',
  ARKIVERTE = 'ARKIVERTE',
}

const erArkivertHendelse = ({ status }: Grunnlagsendringshendelse) =>
  [
    GrunnlagsendringStatus.VURDERT_SOM_IKKE_RELEVANT,
    GrunnlagsendringStatus.HISTORISK,
    GrunnlagsendringStatus.FORKASTET,
  ].includes(status)

export const Hendelser = ({ sakResult, fnr }: { sakResult: Result<SakMedBehandlinger>; fnr: string }): ReactNode => {
  const [hendelseValg, setHendelseValg] = useState<HendelseValg>(HendelseValg.NYE)

  const [hendelserResult, hentHendelser] = useApiCall(hentGrunnlagsendringshendelserForSak)
  const [stoettedeRevurderingerResult, stoettedeRevurderingerFetch] = useApiCall(hentStoettedeRevurderinger)

  const nyeHendelser = (hendelser: Grunnlagsendringshendelse[]): Grunnlagsendringshendelse[] =>
    hendelser.filter((hendelse) => hendelse.status !== GrunnlagsendringStatus.VURDERT_SOM_IKKE_RELEVANT)

  const arkiverteHendelser = (hendelser: Grunnlagsendringshendelse[]): Grunnlagsendringshendelse[] =>
    hendelser.filter(erArkivertHendelse)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentHendelser(sakResult.data.sak.id)

      if (sakResult.data.sak.sakType && sakResult.data.behandlinger) {
        stoettedeRevurderingerFetch({ sakType: sakResult.data.sak.sakType })
      }
    }
  }, [sakResult])

  return mapResult(sakResult, {
    pending: <Loader />,
    error: (error) => <SakIkkeFunnet error={error} fnr={fnr} />,
    success: ({ sak, behandlinger }) => (
      <Box padding="space-8">
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
            <ArchiveIcon aria-hidden /> Arkiverte hendelser
          </ToggleGroup.Item>
        </ToggleGroup>

        {mapResult(hendelserResult, {
          pending: <Spinner label="Henter hendelser..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente hendelser'}</ApiErrorAlert>,
          success: ({ hendelser }) => (
            <>
              {hendelseValg === HendelseValg.NYE && (
                <>
                  <RevurderingInfo>
                    Nye hendelser kan kreve revurdering. Vurder derfor om hendelsen har konsekvens for ytelsen. Hvis
                    ikke kan du arkivere hendelsen.
                  </RevurderingInfo>

                  {!!nyeHendelser(hendelser)?.length ? (
                    <HendelserTable>
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell />
                          <Table.HeaderCell scope="col">Hendelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {nyeHendelser(hendelser).map((hendelse) => (
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
              {hendelseValg === HendelseValg.ARKIVERTE && (
                <>
                  {!!arkiverteHendelser(hendelser)?.length ? (
                    <HendelserTable>
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell />
                          <Table.HeaderCell scope="col">Hendelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {arkiverteHendelser(hendelser).map((hendelse) => (
                          <ArkivertHendelseExpandableRow key={hendelse.id} sakType={sak.sakType} hendelse={hendelse} />
                        ))}
                      </Table.Body>
                    </HendelserTable>
                  ) : (
                    <IngenHendelserAlert variant="info" inline>
                      Ingen arkiverte hendelser
                    </IngenHendelserAlert>
                  )}
                </>
              )}
            </>
          ),
        })}
      </Box>
    ),
  })
}

const RevurderingInfo = styled(BodyShort)`
  max-width: 60rem;
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
