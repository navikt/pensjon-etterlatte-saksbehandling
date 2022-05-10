import { Content, ContentHeader } from '../../../shared/styled'
import { useContext } from 'react'
import { BodyLong, Button, Cell, ContentContainer, Grid, Table, Tag } from "@navikt/ds-react";
import { AppContext } from "../../../store/AppContext";
import { IPerson, OpplysningsType } from "../../../store/reducers/BehandlingReducer";
import BrevModal from "./brev-modal";
import { Success } from '@navikt/ds-icons'

export const Brev = () => {
  const { state } = useContext(AppContext)

  const gyldigeTyper = [OpplysningsType.innsender, OpplysningsType.soeker_pdl]

  console.log(state.behandlingReducer.grunnlag)

  const opplysninger: IPerson[] = state.behandlingReducer.grunnlag
      .filter(grunnlag => gyldigeTyper.includes(grunnlag.opplysningType))
      .map(grunnlag => grunnlag.opplysning as IPerson)

  return (
      <Content>
        <ContentHeader>
          <h1>Brev</h1>
        </ContentHeader>

        <ContentContainer>
          <Grid>
            <Cell xs={12}>
              <BodyLong>
                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed ante sit amet tellus aliquet
                mattis. Donec blandit, urna ac vulputate tincidunt, lorem massa tempor lectus, nec porttitor velit nunc
                ac ex. Vivamus vel elementum magna. Nullam tristique nisl sit amet ante interdum, vitae tincidunt libero
                placerat. Pellentesque et dolor at felis dapibus cursus viverra ut massa. Nunc ac pharetra est. Donec
                finibus ante ut volutpat blandit. Integer condimentum eros malesuada luctus egestas. Integer sodales
                aliquet nisi non elementum. Nunc congue, nisi in congue dictum, odio est ultrices enim, non venenatis
                nulla diam non purus. Pellentesque dapibus rutrum elementum.
              </BodyLong>
            </Cell>

            <Cell xs={12}>
              <Table>
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell>ID</Table.HeaderCell>
                    <Table.HeaderCell>Filnavn</Table.HeaderCell>
                    <Table.HeaderCell>Mottaker navn</Table.HeaderCell>
                    <Table.HeaderCell>Fødselsnummer</Table.HeaderCell>
                    <Table.HeaderCell>Status</Table.HeaderCell>
                    <Table.HeaderCell>Handlinger</Table.HeaderCell>
                  </Table.Row>
                </Table.Header>

                <Table.Body>
                  {opplysninger.map((opplysning, i) => (
                      <Table.Row key={i}>
                        <Table.DataCell>{i + 1}</Table.DataCell>
                        <Table.DataCell>Vedtak om innvilget barnepensjon</Table.DataCell>
                        <Table.DataCell>
                          {opplysning.fornavn} {opplysning.etternavn}
                        </Table.DataCell>
                        <Table.DataCell>
                          {opplysning.foedselsnummer}
                        </Table.DataCell>
                        <Table.DataCell>
                          <Tag variant="warning" size={'small'}>
                            Ikke sendt
                          </Tag>
                        </Table.DataCell>
                        <Table.DataCell>
                          <BrevModal/>
                        </Table.DataCell>
                      </Table.Row>
                  ))}
                  <Table.Row>
                    <Table.DataCell>{opplysninger.length + 1}</Table.DataCell>
                    <Table.DataCell>Informasjonsbrev</Table.DataCell>
                    <Table.DataCell>
                      TALENTFULL BLYANT
                    </Table.DataCell>
                    <Table.DataCell>
                      12101376212
                    </Table.DataCell>
                    <Table.DataCell>
                      <Tag variant="success" size={'small'}>
                        Sendt &nbsp;<Success/>
                      </Tag>
                    </Table.DataCell>
                    <Table.DataCell>
                      <Button variant={'secondary'} size={'small'}>
                        Vis brev
                      </Button>
                    </Table.DataCell>
                  </Table.Row>
                </Table.Body>
              </Table>
            </Cell>
          </Grid>
        </ContentContainer>
      </Content>
  )
}
