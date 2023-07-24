import { HeartIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { IFamilieforhold, IPdlPerson } from '~shared/types/Person'
import { FlexHeader, IconWrapper, TableWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { format } from 'date-fns'
import styled from 'styled-components'
import { IconSize } from '~shared/types/Icon'
import { DatoFormat } from '~utils/formattering'

export const SivilstandWrapper = styled.span`
  min-width: 60%;
  max-width: 36rem;
`

type Props = {
  familieforhold: IFamilieforhold
  avdoed: IPdlPerson
}

export const Sivilstand = ({ familieforhold, avdoed }: Props) => {
  const sivilstand = familieforhold.gjenlevende.opplysning.sivilstand

  return (
    <SivilstandWrapper>
      <FlexHeader>
        <IconWrapper>
          <HeartIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size={'small'} level={'3'}>
          Sivilstand (gjenlevende)
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
            {sivilstand ? (
              sivilstand?.map((ss, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>{ss.sivilstatus}</Table.DataCell>
                  <Table.DataCell>
                    {ss.gyldigFraOgMed
                      ? format(new Date(ss.gyldigFraOgMed), DatoFormat.DAG_MAANED_AAR)
                      : ' Mangler dato'}
                  </Table.DataCell>
                  <Table.DataCell>
                    {ss.relatertVedSiviltilstand === avdoed.foedselsnummer
                      ? `${avdoed.fornavn} ${avdoed.etternavn}`
                      : 'Annen person'}
                  </Table.DataCell>
                  <Table.DataCell>
                    {ss.relatertVedSiviltilstand === avdoed.foedselsnummer
                      ? format(new Date(avdoed.doedsdato), DatoFormat.DAG_MAANED_AAR)
                      : ''}
                  </Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell aria-colspan={4} colSpan={4} align={'center'}>
                  Mangler data om sivilstand
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </TableWrapper>
    </SivilstandWrapper>
  )
}
