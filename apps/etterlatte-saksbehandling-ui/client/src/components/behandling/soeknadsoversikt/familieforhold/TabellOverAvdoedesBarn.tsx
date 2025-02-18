import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { ChildHairEyesIcon } from '@navikt/aksel-icons'
import React from 'react'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { PdlPersonAktivEllerSisteAdresse } from '~components/behandling/soeknadsoversikt/familieforhold/AktivEllerSisteAdresse'
import { BarnAddressePeriode } from '~components/behandling/soeknadsoversikt/familieforhold/BarnAddressePeriode'
import { Personopplysning } from '~shared/types/grunnlag'

interface Props {
  avdoedesBarn: IPdlPerson[] | undefined
  soeker: Personopplysning | undefined
}

export const TabellOverAvdoedesBarn = ({ avdoedesBarn, soeker }: Props) => {
  const erGjenlevendesBarn = (barn: IPdlPerson) =>
    soeker?.opplysning.familieRelasjon?.barn?.includes(barn.foedselsnummer) ?? false

  return (
    <VStack gap="4">
      <HStack gap="4" justify="start" align="center" wrap={false}>
        <ChildHairEyesIcon fontSize="1.75rem" aria-hidden />
        <Heading size="small" level="3">
          Avdødes barn
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
            <Table.HeaderCell scope="col">Foreldre</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoedesBarn?.length ? (
            avdoedesBarn.map((barn, index) => (
              <Table.Row key={index}>
                <Table.DataCell>{`${formaterNavn(barn)} (${hentAlderForDato(barn.foedselsdato)} år)`}</Table.DataCell>
                <Table.DataCell>
                  <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                </Table.DataCell>
                <Table.DataCell>
                  <PdlPersonAktivEllerSisteAdresse person={barn} />
                </Table.DataCell>
                <Table.DataCell>
                  <BarnAddressePeriode barn={barn} />
                </Table.DataCell>
                <Table.DataCell>{erGjenlevendesBarn(barn) ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5}>
                <Heading size="small">Avøde har ingen barn</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
