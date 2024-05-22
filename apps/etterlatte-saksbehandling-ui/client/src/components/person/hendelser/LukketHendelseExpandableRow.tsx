import React from 'react'
import { Table } from '@navikt/ds-react'
import { HendelseBeskrivelse } from '~components/person/hendelser/HendelseBeskrivelse'
import { SakType } from '~shared/types/sak'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import { grunnlagsendringsTittel } from '~components/person/hendelser/utils'
import { formaterStringDato } from '~utils/formattering'

interface Props {
  sakType: SakType
  hendelse: Grunnlagsendringshendelse
}

export const LukketHendelseExpandableRow = ({ sakType, hendelse }: Props) => {
  return (
    <Table.ExpandableRow content={<HendelseBeskrivelse hendelse={hendelse} sakType={sakType} />} expandOnRowClick>
      <Table.DataCell>{grunnlagsendringsTittel[hendelse.samsvarMellomKildeOgGrunnlag.type]}</Table.DataCell>
      <Table.DataCell>{formaterStringDato(hendelse.opprettet)}</Table.DataCell>
    </Table.ExpandableRow>
  )
}
