import React, { ReactNode } from 'react'
import { Grunnlagsendringshendelse, IBehandlingsammendrag } from '~components/person/typer'
import { ISakMedUtlandstilknytning } from '~shared/types/sak'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { Alert, HStack, Link, Table, VStack } from '@navikt/ds-react'
import { HendelseBeskrivelse } from '~components/person/hendelser/HendelseBeskrivelse'
import { grunnlagsendringsTittel, stoetterRevurderingAvHendelse } from '~components/person/hendelser/utils'
import { formaterDato } from '~utils/formatering/dato'
import { harAapenRevurdering, revurderingKanOpprettes } from '~components/person/hendelser/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ArkiverHendelseModal } from '~components/person/hendelser/ArkiverHendelseModal'
import { useSearchParams } from 'react-router-dom'
import { OpprettRevurderingModal } from '~components/person/OpprettRevurderingModal'

interface Props {
  hendelse: Grunnlagsendringshendelse
  sak: ISakMedUtlandstilknytning
  behandlinger: IBehandlingsammendrag[]
  revurderinger: Revurderingaarsak[]
}

export const NyHendelseExpandableRow = ({ hendelse, sak, behandlinger, revurderinger }: Props): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [search] = useSearchParams()

  return (
    <Table.ExpandableRow
      defaultOpen={search.get('referanse') === hendelse.id}
      expandOnRowClick
      content={
        <VStack gap="4">
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

          <HStack gap="4">
            <ArkiverHendelseModal hendelse={hendelse} />

            {hendelse.status !== 'TATT_MED_I_BEHANDLING' &&
              stoetterRevurderingAvHendelse(hendelse, revurderinger) &&
              revurderingKanOpprettes(behandlinger, sak.enhet, innloggetSaksbehandler.enheter) && (
                <OpprettRevurderingModal sakId={sak.id} sakType={sak.sakType} hendelseId={hendelse.id} />
              )}
          </HStack>
        </VStack>
      }
    >
      <Table.DataCell>{grunnlagsendringsTittel[hendelse.samsvarMellomKildeOgGrunnlag.type]}</Table.DataCell>
      <Table.DataCell>{formaterDato(hendelse.opprettet)}</Table.DataCell>
    </Table.ExpandableRow>
  )
}
