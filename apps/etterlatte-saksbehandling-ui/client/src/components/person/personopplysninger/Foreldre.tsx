import React from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PersonIcon } from '@navikt/aksel-icons'
import { CopyButton, Heading, Table, Tag } from '@navikt/ds-react'
import { SpaceChildren } from '~shared/styled'
import { Personopplysning as PdlPersonopplysning } from '~shared/types/grunnlag'
import { formaterStringDato } from '~utils/formattering'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'

export const Foreldre = ({
  avdoed,
  gjenlevende,
  foreldreansvar,
}: {
  avdoed?: PdlPersonopplysning[]
  gjenlevende?: PdlPersonopplysning[]
  foreldreansvar?: string[]
}) => {
  const harForeldreansvar = (fnr: string): boolean => {
    return !!foreldreansvar && foreldreansvar.includes(fnr)
  }

  return (
    <Personopplysning heading="Foreldre" icon={<PersonIcon height="2rem" width="2rem" />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Bostedsadresse</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Foreldreansvar</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {avdoed && avdoed.length > 0 ? (
            <>
              {avdoed.map((doed: PdlPersonopplysning, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      {`${doed.opplysning.fornavn} ${doed.opplysning.etternavn}`}
                      {!!doed.opplysning.doedsdato && (
                        <Tag variant="error-filled" size="small">
                          Død {formaterStringDato(doed.opplysning.doedsdato)}
                        </Tag>
                      )}
                    </SpaceChildren>
                  </Table.DataCell>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      <CopyButton
                        copyText={doed.opplysning.foedselsnummer}
                        text={doed.opplysning.foedselsnummer}
                        size="small"
                        iconPosition="right"
                      />
                      <AlderTag foedselsdato={doed.opplysning.foedselsdato} />
                    </SpaceChildren>
                  </Table.DataCell>
                  <BostedsadresseDataCell bostedsadresse={doed.opplysning.bostedsadresse} index={0} />
                  <Table.DataCell>-</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
          {gjenlevende && gjenlevende.length >= 0 ? (
            <>
              {gjenlevende.map((levende: PdlPersonopplysning, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      {levende.opplysning.fornavn} {levende.opplysning.etternavn}
                    </SpaceChildren>
                  </Table.DataCell>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      <CopyButton
                        copyText={levende.opplysning.foedselsnummer}
                        text={levende.opplysning.foedselsnummer}
                        size="small"
                        iconPosition="right"
                      />
                      <AlderTag foedselsdato={levende.opplysning.foedselsdato} />
                    </SpaceChildren>
                  </Table.DataCell>
                  {!!levende.opplysning.bostedsadresse ? (
                    <Table.DataCell>
                      <SpaceChildren direction="row">
                        {`${levende.opplysning.bostedsadresse[0].adresseLinje1}, ${levende.opplysning.bostedsadresse[0].postnr} ${!!levende.opplysning.bostedsadresse[0].poststed ? levende.opplysning.bostedsadresse[0].poststed : ''}`}
                      </SpaceChildren>
                    </Table.DataCell>
                  ) : (
                    <Table.DataCell>Ingen bostedsadresse tilgjengelig</Table.DataCell>
                  )}
                  <Table.DataCell>{harForeldreansvar(levende.opplysning.foedselsnummer) ? 'Ja' : 'Nei'}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
