import { Familiemedlem, Sivilstand } from '~shared/types/familieOpplysninger'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { HeartIcon } from '@navikt/aksel-icons'
import { lowerCase, startCase } from 'lodash'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import React from 'react'

interface Props {
  sivilstand?: Sivilstand[]
  soeker?: Sivilstand
  avdoede?: Familiemedlem[]
  erAvdoedesSivilstand?: boolean
}

export const SivilstandExpansionCard = ({ sivilstand, avdoede, erAvdoedesSivilstand = false }: Props) => {
  const relatertAvdoed = (relatertVedSiviltilstand: string, avdoede: Familiemedlem[]): Familiemedlem | undefined =>
    avdoede.find((val) => val.foedselsnummer === relatertVedSiviltilstand)

  return (
    <ExpansionCard aria-labelledby="Sivilstand" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <HeartIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">
            {erAvdoedesSivilstand ? 'Avdødes sivilstand' : 'Sivilstand'}
          </ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Status</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
              <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!sivilstand?.length ? (
              sivilstand.map((stand, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>{startCase(lowerCase(stand.sivilstatus))}</Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="space-4">
                      {!!stand.relatertVedSivilstand ? (
                        <>
                          <KopierbarVerdi value={stand.relatertVedSivilstand} iconPosition="right" />
                          {avdoede && avdoede.length >= 0 && (
                            <DoedsdatoTag doedsdato={relatertAvdoed(stand.relatertVedSivilstand, avdoede)?.doedsdato} />
                          )}
                        </>
                      ) : (
                        'Ingen relatert'
                      )}
                    </HStack>
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(stand.gyldigFraOgMed, '-')}</Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={3}>
                  <Heading size="small">Ingen sivilstand</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
