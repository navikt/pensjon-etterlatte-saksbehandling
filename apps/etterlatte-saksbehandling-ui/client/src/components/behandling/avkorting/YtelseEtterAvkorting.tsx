import { IAvkortetYtelse, IAvkorting } from '~shared/types/IAvkorting'
import { Button, Heading, Table, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'
import { formaterStringDato, NOK } from '~utils/formattering'
import { YtelseEtterAvkortingDetaljer } from '~components/behandling/avkorting/YtelseEtterAvkortingDetaljer'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { PencilIcon } from '@navikt/aksel-icons'
import { isPending, Result, useApiCall } from '~shared/hooks/useApiCall'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { lagreManuellRestanse } from '~shared/api/avkorting'

const sorterNyligsteFoerstOgBakover = (a: IAvkortetYtelse, b: IAvkortetYtelse) =>
  new Date(b.fom).getTime() - new Date(a.fom).getTime()

export const YtelseEtterAvkorting = (props: {
  ytelser: IAvkortetYtelse[]
  tidligereYtelser: IAvkortetYtelse[]
  behandling: IBehandlingReducer
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const ytelser = [...props.ytelser].sort(sorterNyligsteFoerstOgBakover)
  const tidligereYtelser = [...props.tidligereYtelser].sort(sorterNyligsteFoerstOgBakover)

  const [redigerRestanse, setRedigerRestanse] = useState<boolean>(false)
  const redigerbar = hentBehandlesFraStatus(props.behandling.status)

  const [manuellRestanseStatus, requestLagreManuellRestanse] = useApiCall(lagreManuellRestanse)
  const submitRestanse = (avkortetYtelseId: string, nyRestanse: number) =>
    requestLagreManuellRestanse(
      {
        behandlingId: props.behandling.id,
        avkortetYtelseId: avkortetYtelseId,
        nyRestanse: nyRestanse,
      },
      (respons) => {
        props.setAvkorting(respons)
      }
    )

  const finnTidligereTidligereYtelseIPeriode = (ytelse: IAvkortetYtelse) => {
    return tidligereYtelser.find(
      (tidligere) => ytelse.fom >= tidligere.fom && (tidligere.tom == null || ytelse.fom < tidligere.tom)
    )
  }

  return (
    <>
      {ytelser.length > 0 && (
        <TableWrapper>
          <Heading spacing size="small" level="2">
            Beregning etter avkorting
          </Heading>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Avkorting</Table.HeaderCell>
                <Table.HeaderCell>Restanse</Table.HeaderCell>
                <Table.HeaderCell>Brutto stønad etter avkorting</Table.HeaderCell>
                {redigerbar && <Table.HeaderCell />}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {ytelser.map((ytelse, key) => {
                const tidligereYtelse = finnTidligereTidligereYtelseIPeriode(ytelse)
                return (
                  <AvkortetYtelseRad
                    key={key}
                    ytelse={ytelse}
                    tidligereYtelse={tidligereYtelse}
                    redigerRestanse={redigerRestanse}
                    submit={submitRestanse}
                    submitResult={manuellRestanseStatus}
                  />
                )
              })}
            </Table.Body>
          </Table>
          {redigerbar && (
            <Table.DataCell>
              <FormKnapper>
                {redigerRestanse ? (
                  <Button
                    size="small"
                    variant="tertiary"
                    onClick={(e) => {
                      e.preventDefault()
                      setRedigerRestanse(false)
                    }}
                  >
                    Avbryt
                  </Button>
                ) : (
                  <Button
                    size="small"
                    variant="secondary"
                    icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
                    onClick={(e) => {
                      e.preventDefault()
                      setRedigerRestanse(true)
                    }}
                  >
                    Rediger
                  </Button>
                )}
              </FormKnapper>
            </Table.DataCell>
          )}
        </TableWrapper>
      )}
    </>
  )
}

const AvkortetYtelseRad = (props: {
  ytelse: IAvkortetYtelse
  tidligereYtelse: IAvkortetYtelse | undefined
  redigerRestanse: boolean
  submit: (avkortetYtelseId: string, restanse: number) => void
  submitResult: Result<IAvkorting>
}) => {
  const { ytelse, tidligereYtelse, redigerRestanse, submit, submitResult } = props

  const [manuellRestanse, setManuellRestanse] = useState<number>(ytelse.restanse)

  const restanseIKroner = (restanse: number) => (restanse < 0 ? `+ ${NOK(restanse * -1)}` : `- ${NOK(restanse)}`)
  return (
    <Table.ExpandableRow shadeOnHover={false} content={<YtelseEtterAvkortingDetaljer ytelse={ytelse} />}>
      <Table.DataCell>
        {formaterStringDato(ytelse.fom)} - {ytelse.tom ? formaterStringDato(ytelse.tom) : ''}
      </Table.DataCell>
      <Table.DataCell>
        <Info
          tekst={NOK(ytelse.avkortingsbeloep)}
          label={''}
          undertekst={tidligereYtelse ? `${NOK(tidligereYtelse.avkortingsbeloep)} (Forrige vedtak)` : ''}
        />
      </Table.DataCell>
      <Table.DataCell>
        {redigerRestanse ? (
          <TextField
            label={''}
            size="small"
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={manuellRestanse}
            onChange={(e) => setManuellRestanse(Number(e.target.value))}
          />
        ) : (
          <Info
            tekst={restanseIKroner(ytelse.restanse)}
            label={''}
            undertekst={tidligereYtelse ? `${restanseIKroner(tidligereYtelse.restanse)} (Forrige vedtak)` : ''}
          />
        )}
      </Table.DataCell>
      <Table.DataCell>
        <ManglerRegelspesifikasjon>
          <Info
            tekst={NOK(ytelse.ytelseEtterAvkorting)}
            label={''}
            undertekst={tidligereYtelse ? `${NOK(tidligereYtelse.ytelseEtterAvkorting)} (Forrige vedtak)` : ''}
          />
        </ManglerRegelspesifikasjon>
      </Table.DataCell>
      {redigerRestanse && (
        <Table.DataCell>
          <Button size="small" loading={isPending(submitResult)} onClick={() => submit(ytelse.id, manuellRestanse)}>
            Lagre
          </Button>
        </Table.DataCell>
      )}
    </Table.ExpandableRow>
  )
}

const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 1000px;

  .table {
    max-width: 1000px;

    .tableCell {
      max-width: 100px;
    }
  }
`

const FormKnapper = styled.div`
  margin-top: 1rem;
  margin-right: 1em;
  gap: 1rem;
`
