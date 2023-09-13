import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserInstitusjonsoppholdforSak } from '~shared/api/behandling'
import React, { useEffect } from 'react'
import { InstitusjonsoppholdSamsvar } from '~components/person/typer'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { isPending } from '@reduxjs/toolkit'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

const Insthendelser = (props: { sakid: number }) => {
  const { sakid } = props
  const [hendelser, hentInsthendelser] = useApiCall(hentGrunnlagsendringshendelserInstitusjonsoppholdforSak)

  useEffect(() => {
    hentInsthendelser(sakid)
  }, [])

  return (
    <>
      {isPending(hendelser) && <Spinner visible label={'Henter institusjonshendelser for søsken'} />}
      {isFailure(hendelser) && <ApiErrorAlert>Institusjonshendelser kan ikke hentes</ApiErrorAlert>}
      {isSuccess(hendelser) && hendelser.data.length > 0 && (
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
            {hendelser.data.map((hendelse) => {
              const inst = hendelse.samsvarMellomKildeOgGrunnlag as InstitusjonsoppholdSamsvar
              return (
                <Table.Row key={hendelse.id}>
                  <Table.HeaderCell scope="row">
                    {inst.oppholdBeriket.startdato
                      ? formaterStringDato(inst.oppholdBeriket.startdato)
                      : 'Ingen startdato'}
                  </Table.HeaderCell>
                  <Table.DataCell>
                    {inst.oppholdBeriket.faktiskSluttdato
                      ? formaterStringDato(inst.oppholdBeriket.faktiskSluttdato)
                      : 'Ingen sluttdato'}
                  </Table.DataCell>
                  <Table.DataCell>
                    {inst.oppholdBeriket.forventetSluttdato
                      ? formaterStringDato(inst.oppholdBeriket.forventetSluttdato)
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
      )}
    </>
  )
}

export const institusjonstype: { [key: string]: string } = {
  AS: 'Alders- og sykehjem',
  FO: 'Fengsel',
  HS: 'Helseinstitusjon',
}

export default Insthendelser
