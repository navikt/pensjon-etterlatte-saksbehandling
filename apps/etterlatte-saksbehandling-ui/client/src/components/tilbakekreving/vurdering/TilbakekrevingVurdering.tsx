import { Button, Heading, Select, Table } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React from 'react'

export function TilbakekrevingVurdering() {
  const tilbakekreving = useTilbakekreving()
  const navigate = useNavigate()

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <Innhold>
        <Table className="table" zebraStripes>
          <Table.Header>
            <Table.HeaderCell>Måned</Table.HeaderCell>
            <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
            <Table.HeaderCell>Brutto utbetaling</Table.HeaderCell>
            <Table.HeaderCell>Beregnet ny brutto</Table.HeaderCell>
            <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
            <Table.HeaderCell>Skatteprosent</Table.HeaderCell>
            <Table.HeaderCell>Brutto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Netto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Skatt</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell>Skyld</Table.HeaderCell>
            <Table.HeaderCell>Årsak</Table.HeaderCell>
          </Table.Header>
          <Table.Body>
            {tilbakekreving?.kravgrunnlag.perioder.map((periode) => {
              return (
                <Table.Row key="test">
                  <Table.DataCell key="test">{periode.fra}</Table.DataCell>
                  <Table.DataCell key="test">{periode.beskrivelse}</Table.DataCell>
                  <Table.DataCell key="test">{periode.bruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="test">{periode.beregnetNyBrutto}</Table.DataCell>
                  <Table.DataCell key="test">{periode.beregnetFeilutbetaling}</Table.DataCell>
                  <Table.DataCell key="test">{periode.skatteprosent}</Table.DataCell>
                  <Table.DataCell key="test">{periode.nettoTilbakekreving}</Table.DataCell>
                  <Table.DataCell key="test">{periode.nettoTilbakekreving}</Table.DataCell>
                  <Table.DataCell key="test">{periode.skatt}</Table.DataCell>
                  <Table.DataCell key="test">
                    <Select
                      label="Resultat"
                      hideLabel={true}
                      value=""
                      //onChange={(e) => {}}
                    >
                      <option key="test">Velg..</option>
                      <option key="test">Foreldet</option>
                      <option key="test">Ingen tilbakekreving</option>
                      <option key="test">Delvis tilbakekreving</option>
                      <option key="test">Full tilbakekreving</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="test">
                    <Select
                      label="Skyld"
                      hideLabel={true}
                      value=""
                      //onChange={(e) => {}}
                    >
                      <option key="test">Velg..</option>
                      <option key="test">Ikke fordelt</option>
                      <option key="test">Bruker</option>
                      <option key="test">Nav</option>
                      <option key="test">Skylddeling</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="test">
                    <Select
                      label="Årsak"
                      hideLabel={true}
                      value=""
                      //onChange={(e) => {}}
                    >
                      <option key="test">Velg..</option>
                      <option key="test">Annet</option>
                      <option key="test">Feil regelbruk</option>
                    </Select>
                  </Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
      </Innhold>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Gå til vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
