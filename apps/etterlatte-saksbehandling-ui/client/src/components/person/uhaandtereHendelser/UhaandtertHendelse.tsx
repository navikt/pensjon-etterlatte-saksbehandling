import { Grunnlagsendringshendelse } from '~components/person/typer'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import React from 'react'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/uhaandtereHendelser/utils'
import { Alert, Button, Link, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { HendelseBeskrivelse } from '~components/person/hendelser/HendelseBeskrivelse'
import styled from 'styled-components'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { SakType } from '~shared/types/sak'
import { LukkHendelseModal } from '~components/person/hendelser/LukkHendelseModal'

interface Props {
  sakType: SakType
  hendelse: Grunnlagsendringshendelse
  harAapenRevurdering: boolean
  startRevurdering: (hendelse: Grunnlagsendringshendelse) => void
  revurderinger: Array<Revurderingaarsak>
  revurderingKanOpprettes: boolean
}

const UhaandtertHendelse = ({
  sakType,
  hendelse,
  harAapenRevurdering,
  startRevurdering,
  revurderinger,
  revurderingKanOpprettes,
}: Props) => {
  const { samsvarMellomKildeOgGrunnlag, opprettet } = hendelse
  const stoetterRevurdering = stoetterRevurderingAvHendelse(hendelse, revurderinger)
  const { type: samsvarType } = samsvarMellomKildeOgGrunnlag

  const tattMedIBehandling = hendelse.status === 'TATT_MED_I_BEHANDLING'

  return (
    <Table.ExpandableRow
      content={
        <>
          <HendelseBeskrivelse sakType={sakType} hendelse={hendelse} />

          {tattMedIBehandling ? (
            <Alert variant="info" inline>
              Denne hendelsen har en revurdering knyttet til seg.{' '}
              <BlueLink href={`/behandling/${hendelse.behandlingId}/revurderingsoversikt`}>Gå til revurdering</BlueLink>
            </Alert>
          ) : harAapenRevurdering ? (
            <Alert variant="info" inline>
              Denne saken har en åpen revurdering, denne må behandles før en ny kan startes.
            </Alert>
          ) : (
            !stoetterRevurdering && (
              <Alert variant="info" inline>
                Automatisk revurdering støttes ikke for denne hendelsen
              </Alert>
            )
          )}

          <div style={{ minHeight: '3rem', marginTop: '1rem' }}>
            {!tattMedIBehandling && stoetterRevurdering && revurderingKanOpprettes && (
              <Button
                disabled={harAapenRevurdering}
                onClick={() => startRevurdering(hendelse)}
                icon={<ArrowsCirclepathIcon />}
              >
                Start revurdering
              </Button>
            )}

            <LukkHendelseModal hendelse={hendelse} />
          </div>
        </>
      }
    >
      <Table.DataCell>{grunnlagsendringsTittel[samsvarType]}</Table.DataCell>
      <Table.DataCell>{formaterStringDato(opprettet)}</Table.DataCell>
      <Table.DataCell></Table.DataCell>
    </Table.ExpandableRow>
  )
}

const BlueLink = styled(Link)`
  color: #0067c5 !important;
`

export default UhaandtertHendelse
