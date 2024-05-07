import React, { ReactNode } from 'react'
import { Grunnlagsendringshendelse, IBehandlingsammendrag } from '~components/person/typer'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { Alert, Link, Table } from '@navikt/ds-react'
import { HendelseBeskrivelse } from '~components/person/hendelser/HendelseBeskrivelse'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/uhaandtereHendelser/utils'
import { formaterStringDato } from '~utils/formattering'
import { harAapenRevurdering, revurderingKanOpprettes } from '~components/person/hendelser/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { FlexRow } from '~shared/styled'
import { StartRevurderingModal } from '~components/person/hendelser/StartRevurderingModal'
import { LukkHendelseModal } from '~components/person/hendelser/LukkHendelseModal'

interface Props {
  hendelse: Grunnlagsendringshendelse
  sak: ISakMedUtlandstilknytning
  behandlinger: IBehandlingsammendrag[]
  revurderinger: Revurderingaarsak[]
}

export const NyHendelseExpandableRow = ({ hendelse, sak, behandlinger, revurderinger }: Props): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  return (
    <Table.ExpandableRow
      content={
        <>
          <HendelseBeskrivelse sakType={sak.sakType} hendelse={hendelse} />

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

          <FlexRow>
            <LukkHendelseModal hendelse={hendelse} />

            {hendelse.status !== 'TATT_MED_I_BEHANDLING' &&
              stoetterRevurderingAvHendelse(hendelse, revurderinger) &&
              revurderingKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
                <StartRevurderingModal hendelse={hendelse} revurderinger={revurderinger} sakId={sak.id} />
              )}
          </FlexRow>
        </>
      }
    >
      <Table.DataCell>{grunnlagsendringsTittel[hendelse.samsvarMellomKildeOgGrunnlag.type]}</Table.DataCell>
      <Table.DataCell>{formaterStringDato(hendelse.opprettet)}</Table.DataCell>
    </Table.ExpandableRow>
  )
}
