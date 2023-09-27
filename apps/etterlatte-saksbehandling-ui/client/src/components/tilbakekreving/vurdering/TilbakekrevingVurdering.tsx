import { Button, Heading, Select, Table, TextField } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React, { useState } from 'react'
import {
  TilbakekrevingBeloep,
  TilbakekrevingPeriode,
  TilbakekrevingResultat,
  TilbakekrevingSkyld,
} from '~shared/types/Tilbakekreving'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreTilbakekrevingsperioder } from '~shared/api/tilbakekreving'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'

export function TilbakekrevingVurdering() {
  const navigate = useNavigate()
  const tilbakekreving = useTilbakekreving()
  if (!tilbakekreving) throw new Error('Tilbakekreving finnes ikke i state!')
  const dispatch = useAppDispatch()
  const [lagreStatus, lagre] = useApiCall(lagreTilbakekrevingsperioder)
  const [perioder, setPerioder] = useState<TilbakekrevingPeriode[]>(tilbakekreving.perioder)

  const updateBeloeper = (index: number, oppdatertBeloep: TilbakekrevingBeloep) => {
    const oppdatert = perioder.map((periode, i) =>
      i === index ? { ...periode, ytelsebeloeper: oppdatertBeloep } : periode
    )
    setPerioder(oppdatert)
  }

  const lagrePerioder = () => {
    // TODO validering?
    lagre({ tilbakekrevingsId: tilbakekreving.id, perioder }, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>Kommer</InnholdPadding>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="2" size="medium">
            Utbetalinger
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <Table className="table" zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Måned</Table.HeaderCell>
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
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {perioder.map((periode, index) => {
              const beloeper = periode.ytelsebeloeper
              return (
                <Table.Row key={'beloeperRad' + index}>
                  <Table.DataCell key="maaned">{periode.maaned.toString()}</Table.DataCell>
                  <Table.DataCell key="bruttoUtbetaling">{beloeper.bruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="nyBruttoUtbetaling">{beloeper.nyBruttoUtbetaling}</Table.DataCell>
                  <Table.DataCell key="beregnetFeilutbetaling">
                    <TextField
                      label=""
                      placeholder="Beregnet feilutbetaling"
                      value={beloeper.beregnetFeilutbetaling ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          beregnetFeilutbetaling: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatteprosent">{beloeper.skatteprosent}</Table.DataCell>
                  <Table.DataCell key="bruttoTilbakekreving">
                    <TextField
                      label=""
                      placeholder="Brutto tilbakekreving"
                      value={beloeper.bruttoTilbakekreving ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          bruttoTilbakekreving: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="nettoTilbakekreving">
                    <TextField
                      label=""
                      placeholder="Netto tilbakekreving"
                      value={beloeper.nettoTilbakekreving ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          nettoTilbakekreving: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skatt">
                    <TextField
                      label=""
                      placeholder="Skatt"
                      value={beloeper.skatt ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          skatt: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="skyld">
                    <Select
                      label="Skyld"
                      hideLabel={true}
                      value={beloeper.skyld ?? ''}
                      onChange={(e) => {
                        if (e.target.value === '') return
                        updateBeloeper(index, {
                          ...beloeper,
                          skyld: TilbakekrevingSkyld[e.target.value as TilbakekrevingSkyld],
                        })
                      }}
                    >
                      <option value="">Velg..</option>
                      <option value={TilbakekrevingSkyld.IKKE_FORDELT}>Ikke fordelt</option>
                      <option value={TilbakekrevingSkyld.BRUKER}>Bruker</option>
                      <option value={TilbakekrevingSkyld.NAV}>Nav</option>
                      <option value={TilbakekrevingSkyld.SKYLDDELING}>Skylddeling</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="resultat">
                    <Select
                      label="Resultat"
                      hideLabel={true}
                      value={beloeper.resultat ?? ''}
                      onChange={(e) => {
                        if (e.target.value === '') return
                        updateBeloeper(index, {
                          ...beloeper,
                          resultat: TilbakekrevingResultat[e.target.value as TilbakekrevingResultat],
                        })
                      }}
                    >
                      <option value="">Velg..</option>
                      <option value={TilbakekrevingResultat.FORELDET}>Foreldet</option>
                      <option value={TilbakekrevingResultat.INGEN_TILBAKEKREV}>Ingen tilbakekreving</option>
                      <option value={TilbakekrevingResultat.DELVIS_TILBAKEKREV}>Delvis tilbakekreving</option>
                      <option value={TilbakekrevingResultat.FULL_TILBAKEKREV}>Full tilbakekreving</option>
                    </Select>
                  </Table.DataCell>
                  <Table.DataCell key="tilbakekrevingsprosent">
                    <TextField
                      label=""
                      placeholder="Tilbakekrevingsprosent"
                      value={beloeper.tilbakekrevingsprosent ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={3}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          tilbakekrevingsprosent: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                  <Table.DataCell key="rentetillegg">
                    <TextField
                      label=""
                      placeholder="Rentetillegg"
                      value={beloeper.rentetillegg ?? ''}
                      pattern="[0-9]{11}"
                      maxLength={10}
                      onChange={(e) =>
                        updateBeloeper(index, {
                          ...beloeper,
                          rentetillegg: parseInt(e.target.value),
                        })
                      }
                    />
                  </Table.DataCell>
                </Table.Row>
              )
            })}
          </Table.Body>
        </Table>
        <Button variant="primary" onClick={lagrePerioder} loading={isPending(lagreStatus)}>
          Lagre
        </Button>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${tilbakekreving?.id}/vedtak`)}>
          Gå til vedtak
        </Button>
      </FlexRow>
    </Content>
  )
}
