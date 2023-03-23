import { Heart } from '@navikt/ds-icons'
import { Heading, Table } from '@navikt/ds-react'
import { IFamilieforhold } from '~shared/types/Person'
import { FlexHeader, IconWrapper, TableWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { format } from 'date-fns'
import styled from 'styled-components'
import { IconSize } from '~shared/types/Icon'

export const SivilstatusWrapper = styled.span`
  min-width: 60%;
  max-width: 36rem;
`

type Props = {
  familieforhold: IFamilieforhold
}

export const Sivilstatus: React.FC<Props> = ({ familieforhold }) => {
  const sivilstatus = familieforhold.gjenlevende.opplysning.sivilstatus

  return (
    <SivilstatusWrapper>
      <FlexHeader>
        <IconWrapper>
          <Heart fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size={'small'} level={'3'}>
          Sivilstatus (gjenlevende)
        </Heading>
      </FlexHeader>
      <TableWrapper>
        <Table size={'small'}>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope={'col'}>Status</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Dato</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Partner</Table.HeaderCell>
              <Table.HeaderCell scope={'col'}>Dødsdato</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.DataCell>{sivilstatus}</Table.DataCell>
              <Table.DataCell>{format(new Date(), 'dd.MM.yyyy')}</Table.DataCell>
              <Table.DataCell></Table.DataCell>
              <Table.DataCell></Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </TableWrapper>
    </SivilstatusWrapper>
  )
}
