import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserInstitusjonsoppholdForSak } from '~shared/api/behandling'
import React, { useEffect } from 'react'
import { InstitusjonsoppholdSamsvar } from '~components/person/typer'
import { Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { mapAllApiResult } from '~shared/api/apiUtils'
import { institusjonstype } from '~shared/types/Institusjonsopphold'

const Insthendelser = (props: { sakid: number }) => {
  const { sakid } = props
  const [hendelser, hentInsthendelser] = useApiCall(hentGrunnlagsendringshendelserInstitusjonsoppholdForSak)

  useEffect(() => {
    hentInsthendelser(sakid)
  }, [])

  return mapAllApiResult(
    hendelser,
    <Spinner visible label="Henter institusjonshendelser for søsken" />,
    null,
    () => <ApiErrorAlert>Institusjonshendelser kan ikke hentes</ApiErrorAlert>,
    (hendelserarr) => {
      return (
        <>
          {hendelserarr.length > 0 ? (
            <div style={{ marginBottom: '4rem' }}>
              <h3>Hendelser registrert i inst2</h3>
              <Table>
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell scope="col">Inn</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Ut</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Varighet</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {hendelserarr.map((hendelse) => {
                    const inst = hendelse.samsvarMellomKildeOgGrunnlag as InstitusjonsoppholdSamsvar
                    return (
                      <Table.Row key={hendelse.id}>
                        <Table.HeaderCell scope="row">
                          {inst.oppholdBeriket.startdato
                            ? formaterDato(inst.oppholdBeriket.startdato)
                            : 'Ingen startdato'}
                        </Table.HeaderCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.faktiskSluttdato
                            ? formaterDato(inst.oppholdBeriket.faktiskSluttdato)
                            : 'Ingen sluttdato'}
                        </Table.DataCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.forventetSluttdato
                            ? formaterDato(inst.oppholdBeriket.forventetSluttdato)
                            : 'Ingen forventet sluttdato'}
                        </Table.DataCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.institusjonsType
                            ? institusjonstype[inst.oppholdBeriket.institusjonsType]
                            : `Ukjent type ${inst.oppholdBeriket.institusjonsType}`}
                        </Table.DataCell>
                        <Table.DataCell>{inst.oppholdBeriket.institusjonsnavn}</Table.DataCell>
                      </Table.Row>
                    )
                  })}
                </Table.Body>
              </Table>
            </div>
          ) : null}
        </>
      )
    }
  )
}

export default Insthendelser
