import React, { ReactNode } from 'react'
import { Grunnlagsendringshendelse, IBehandlingsammendrag } from '~components/person/typer'
import { SakType } from '~shared/types/sak'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { Alert, Link, Table } from '@navikt/ds-react'
import { HendelseBeskrivelse } from '~components/person/hendelser/HendelseBeskrivelse'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/uhaandtereHendelser/utils'
import { formaterStringDato } from '~utils/formattering'
import { harAapenRevurdering } from '~components/person/hendelser/utils'

interface Props {
  hendelse: Grunnlagsendringshendelse
  sakType: SakType
  behandlinger: IBehandlingsammendrag[]
  revurderinger: Revurderingaarsak[]
}

export const NyHendelseExpandableRow = ({ hendelse, sakType, behandlinger, revurderinger }: Props): ReactNode => {
  return (
    <Table.ExpandableRow
      content={
        <>
          <HendelseBeskrivelse sakType={sakType} hendelse={hendelse} />

          {hendelse.status === 'TATT_MED_I_BEHANDLING' ? (
            <Alert variant="info" inline>
              Denne hendelsen har en revurdering knyttet til seg.{' '}
              <Link href={`/behandling/${hendelse.behandlingId}/revurderingsoversikt`}>Gå til revurdering</Link>
            </Alert>
          ) : harAapenRevurdering(behandlinger) ? (
            <Alert variant="info" inline>
              Denne saken har en åpen revurdering, denne må behandles før en ny kan startes.
            </Alert>
          ) : (
            !stoetterRevurderingAvHendelse(hendelse, revurderinger) && (
              <Alert variant="info" inline>
                Automatisk revurdering støttes ikke for denne hendelsen
              </Alert>
            )
          )}
        </>
      }
    >
      <Table.DataCell>{grunnlagsendringsTittel[hendelse.samsvarMellomKildeOgGrunnlag.type]}</Table.DataCell>
      <Table.DataCell>{formaterStringDato(hendelse.opprettet)}</Table.DataCell>
    </Table.ExpandableRow>
  )
}
