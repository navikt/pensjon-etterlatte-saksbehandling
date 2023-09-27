import { Button, Heading, Select, Table, TextField } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
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
      <InnholdPadding>
        <Table className="table" zebraStripes>
          <Table.Header>
            <Table.HeaderCell>Måned</Table.HeaderCell>
            <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
            <Table.HeaderCell>Brutto utbetaling</Table.HeaderCell>
            <Table.HeaderCell>Ny brutto utbetaling</Table.HeaderCell>
            <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
            <Table.HeaderCell>Skatteprosent</Table.HeaderCell>
            <Table.HeaderCell>Brutto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Netto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Skatt</Table.HeaderCell>
            <Table.HeaderCell>Skyld</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell>Tilbakekrevingsprosent</Table.HeaderCell>
            <Table.HeaderCell>Rentetillegg</Table.HeaderCell>
          </Table.Header>
          <Table.Body>
            {tilbakekreving?.utbetalinger.map((utbetaling) => {
              return (
                <Table.Row key="test">
                  <Table.DataCell key="maaned">{utbetaling.maaned.toString()}</Table.DataCell>
                  <Table.DataCell key="beskrivelse">{utbetaling.type}</Table.DataCell>
                  <Table.DataCell key="bruttoUtbetaling">{utbetaling.bruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="nyBruttoUtbetaling">{utbetaling.nyBruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="beregnetFeilutbetaling">{utbetaling.beregnetFeilutbetaling}</Table.DataCell>
                  <Table.DataCell key="skatteprosent">{utbetaling.skatteprosent}</Table.DataCell>
                  <Table.DataCell key="bruttoTilbakekreving">{utbetaling.bruttoTilbakekreving}</Table.DataCell>
                  <Table.DataCell key="nettoTilbakekreving">{utbetaling.nettoTilbakekreving}</Table.DataCell>
                  <Table.DataCell key="skatt">{utbetaling.skatt}</Table.DataCell>
                  <Table.DataCell key="skyld">
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
                  <Table.DataCell key="resultat">
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
                  <Table.DataCell key="tilbakekrevingsprosent">
                    <TextField
                      label=""
                      placeholder="Tilbakekrevingsprosent"
                      value={0}
                      pattern="[0-9]{11}"
                      maxLength={3}
                      onChange={() => console.log('TODO')}
                    />
                  </Table.DataCell>
                  <Table.DataCell key="rentetillegg">
                    <TextField
                      label=""
                      placeholder="Rentetillegg"
                      value={0}
                      pattern="[0-9]{11}"
                      maxLength={3}
                      onChange={() => console.log('TODO')}
                    />
                  </Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Gå til vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
