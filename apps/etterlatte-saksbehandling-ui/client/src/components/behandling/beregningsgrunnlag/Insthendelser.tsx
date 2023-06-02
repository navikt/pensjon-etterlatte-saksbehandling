import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserInstitusjonsoppholdforSak } from '~shared/api/behandling'
import React, { useEffect, useState } from 'react'
import { Grunnlagsendringshendelse, InstitusjonsoppholdSamsvar } from '~components/person/typer'
import { Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formattering'
import { isPending } from '@reduxjs/toolkit'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

const Insthendelser = (props: { sakid: number }) => {
  const { sakid } = props
  const [instHendelser, hentInsthendelser] = useApiCall(hentGrunnlagsendringshendelserInstitusjonsoppholdforSak)
  const [hendelser, setHendelser] = useState<Grunnlagsendringshendelse[] | undefined>(undefined)

  useEffect(() => {
    hentInsthendelser(sakid, (hendelser) => {
      setHendelser(hendelser)
    })
  }, [])
  return (
    <>
      <Spinner visible={isPending(instHendelser)} label={'Henter institusjonshendelser for søsken'} />
      {isFailure(instHendelser) && <ApiErrorAlert>Institusjonshendelser kan ikke hentes</ApiErrorAlert>}
      {isSuccess(instHendelser) && (
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
            {hendelser?.map((hendelse) => {
              const inst = hendelse.samsvarMellomKildeOgGrunnlag as InstitusjonsoppholdSamsvar
              return (
                <Table.Row key={hendelse.id}>
                  <Table.HeaderCell scope="row">
                    {inst.oppholdBeriket.startdato ? formaterDato(inst.oppholdBeriket.startdato) : 'Ingen startdato'}
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
      )}
    </>
  )
}

const institusjonstype: { [key: string]: string } = {
  AS: 'Alders- og sykehjem',
  FO: 'Fengsel',
  HS: 'Helseinstitusjon',
}

export default Insthendelser
