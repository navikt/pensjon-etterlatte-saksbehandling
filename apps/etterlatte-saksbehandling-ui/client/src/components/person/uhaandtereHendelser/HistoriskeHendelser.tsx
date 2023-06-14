import { Button, Detail, Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import React, { useEffect, useState } from 'react'
import { ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { formaterDatoMedTidspunkt, formaterStringDato } from '~utils/formattering'
import { JaNei } from '~shared/types/ISvar'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentInstitusjonsoppholdData } from '~shared/api/behandling'
import { VilkaarVurdertInformasjon } from '~components/behandling/vilkaarsvurdering/Vurdering'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
}
const HistoriskeHendelser = (props: Props) => {
  const [aapenhistorikk, setLastetBehandlingliste] = useState<boolean>(false)
  return (
    <HistoriskeHendelserWrapper>
      <Heading spacing size="medium" level="2">
        Tidligere hendelser
      </Heading>
      <div>
        <Button variant="tertiary" onClick={() => setLastetBehandlingliste(!aapenhistorikk)}>
          {aapenhistorikk ? (
            <MarginRightChevron>
              <ChevronUpIcon fontSize="1.5rem" />
            </MarginRightChevron>
          ) : (
            <MarginRightChevron>
              <ChevronDownIcon fontSize="1.5rem" />
            </MarginRightChevron>
          )}
          Vis historikk
        </Button>
        {aapenhistorikk && (
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell scope="col">Sakid</Table.HeaderCell>
                <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                <Table.HeaderCell scope="col">GjelderPerson</Table.HeaderCell>
                <Table.HeaderCell scope="col">Opprettet</Table.HeaderCell>
                <Table.HeaderCell scope="col">Kommentar</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {props.hendelser.map(({ sakId, type, gjelderPerson, id, opprettet, kommentar }) => {
                return (
                  <Table.Row key={id}>
                    <Table.HeaderCell>{sakId}</Table.HeaderCell>
                    <Table.DataCell>{type}</Table.DataCell>
                    <Table.DataCell>{gjelderPerson}</Table.DataCell>
                    <Table.DataCell>{formaterStringDato(opprettet)}</Table.DataCell>
                    <Table.DataCell>
                      {kommentar ? <p>{kommentar}</p> : <p>Ingen kommentar</p>}
                      {type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD && <InstitusjonsoppholdBegrunnelse id={id} />}
                    </Table.DataCell>
                  </Table.Row>
                )
              })}
            </Table.Body>
          </Table>
        )}
      </div>
    </HistoriskeHendelserWrapper>
  )
}

const InstitusjonsoppholdBegrunnelse = ({ id }: { id: string }) => {
  const [begrunnelse, hentInstbegrunnelseData] = useApiCall(hentInstitusjonsoppholdData)

  useEffect(() => {
    hentInstbegrunnelseData(id)
  }, [])

  return (
    <>
      {isSuccess(begrunnelse) && (
        <div>
          <p>
            Er dette en institusjon som kan gi reduksjon av ytelsen? - {begrunnelse.data.kanGiReduksjonAvYtelse}
            {begrunnelse.data.kanGiReduksjonAvYtelseBegrunnelse}
            Er oppholdet forventet å vare lenger enn innleggelsesmåned + tre måneder? -{' '}
            {begrunnelse.data.forventetVarighetMerEnn3Maaneder}
            {begrunnelse.data.forventetVarighetMerEnn3MaanederBegrunnelse}
          </p>
          <VilkaarVurdertInformasjon>
            <Detail>Manuelt av {begrunnelse.data.saksbehandler.ident}</Detail>
            <Detail>
              Dato{' '}
              {begrunnelse.data.saksbehandler.tidspunkt
                ? formaterDatoMedTidspunkt(new Date(begrunnelse.data.saksbehandler.tidspunkt))
                : '-'}
            </Detail>
          </VilkaarVurdertInformasjon>
        </div>
      )}
    </>
  )
}

export interface InstitusjonsoppholdMedKilde {
  kanGiReduksjonAvYtelse: JaNei
  kanGiReduksjonAvYtelseBegrunnelse: string
  forventetVarighetMerEnn3Maaneder: JaNei
  forventetVarighetMerEnn3MaanederBegrunnelse: string
  saksbehandler: KildeSaksbehandler
}

const HistoriskeHendelserWrapper = styled.div`
  margin-top: 3rem;
`

const MarginRightChevron = styled.span`
  margin-right: 5px;
`

export default HistoriskeHendelser
