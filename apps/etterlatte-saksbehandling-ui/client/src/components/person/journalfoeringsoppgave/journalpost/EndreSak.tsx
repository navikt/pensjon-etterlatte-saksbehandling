import { JournalpostSak, Sakstype } from '~shared/types/Journalpost'
import { ISak, SakType } from '~shared/types/sak'
import { BodyShort, Button, Heading, Table } from '@navikt/ds-react'
import React from 'react'
import { TabsAddIcon, XMarkIcon } from '@navikt/aksel-icons'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { PersonLink } from '~components/person/lenker/PersonLink'

export const temaFraSakstype = (sakstype: SakType): string => {
  switch (sakstype) {
    case SakType.BARNEPENSJON:
      return 'EYB'
    case SakType.OMSTILLINGSSTOENAD:
      return 'EYO'
  }
}

const formaterSakstype = (sakstype: Sakstype): string => {
  switch (sakstype) {
    case Sakstype.FAGSAK:
      return 'Fagsak'
    case Sakstype.GENERELL_SAK:
      return 'Generell'
  }
}

export const EndreSak = ({
  fagsak,
  gjennySak,
  kobleTilSak,
  alternativSak,
}: {
  fagsak?: JournalpostSak
  gjennySak: ISak
  kobleTilSak: (sak: JournalpostSak | undefined) => void
  alternativSak?: ISak
}) => {
  const konverterOgKobleTilSak = (sak: ISak) => () => {
    kobleTilSak({
      sakstype: Sakstype.FAGSAK,
      fagsakId: sak.id.toString(),
      fagsaksystem: 'EY',
      tema: temaFraSakstype(sak.sakType),
    })
  }
  const bytteMellomSakEnabled = useFeaturetoggle(FeatureToggle.bytt_til_annen_sak)

  const sakTilhoererIkkeGjenny = fagsak?.fagsakId !== gjennySak.id.toString()

  return (
    <div>
      <Heading size="small" spacing>
        Sak
      </Heading>
      <BodyShort>
        Viser saksliste for bruker: <PersonLink fnr={gjennySak.ident}>{gjennySak.ident}</PersonLink>
      </BodyShort>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>SakID</Table.HeaderCell>
            <Table.HeaderCell>Sakstype</Table.HeaderCell>
            <Table.HeaderCell>Fagsystem</Table.HeaderCell>
            <Table.HeaderCell>Tema</Table.HeaderCell>
            <Table.HeaderCell />
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!fagsak && (
            <Table.Row style={{ background: 'var(--ac-alert-success-bg, var(--a-surface-success-subtle))' }}>
              <Table.DataCell>{fagsak?.fagsakId || '-'}</Table.DataCell>
              <Table.DataCell>{fagsak?.sakstype ? formaterSakstype(fagsak.sakstype) : '-'}</Table.DataCell>
              <Table.DataCell>{fagsak?.fagsaksystem || '-'}</Table.DataCell>
              <Table.DataCell>{fagsak?.tema || '-'}</Table.DataCell>
              <Table.DataCell>
                <Button
                  data-color="neutral"
                  variant="tertiary"
                  size="small"
                  icon={<XMarkIcon title="Fjern kobling til saken" />}
                  style={{ float: 'right' }}
                  onClick={() => kobleTilSak(undefined)}
                />
              </Table.DataCell>
            </Table.Row>
          )}

          {sakTilhoererIkkeGjenny && (
            <>
              <Table.Row style={{ background: 'var(--ac-alert-warning-bg, var(--a-surface-warning-subtle))' }}>
                <Table.DataCell>{gjennySak.id}</Table.DataCell>
                <Table.DataCell>{formaterSakstype(Sakstype.FAGSAK)}</Table.DataCell>
                <Table.DataCell>EY</Table.DataCell>
                <Table.DataCell>{temaFraSakstype(gjennySak.sakType)}</Table.DataCell>
                <Table.DataCell>
                  <Button
                    data-color="neutral"
                    variant="secondary"
                    size="small"
                    icon={<TabsAddIcon aria-hidden />}
                    iconPosition="right"
                    title="Koble til saken"
                    onClick={konverterOgKobleTilSak(gjennySak)}
                  >
                    Koble til sak
                  </Button>
                </Table.DataCell>
              </Table.Row>
              {bytteMellomSakEnabled && alternativSak && (
                <Table.Row>
                  <Table.DataCell>{alternativSak.id}</Table.DataCell>
                  <Table.DataCell>{formaterSakstype(Sakstype.FAGSAK)}</Table.DataCell>
                  <Table.DataCell>EY</Table.DataCell>
                  <Table.DataCell>{temaFraSakstype(alternativSak.sakType)}</Table.DataCell>
                  <Table.DataCell>
                    <Button
                      data-color="neutral"
                      variant="secondary"
                      size="small"
                      icon={<TabsAddIcon aria-hidden />}
                      iconPosition="right"
                      title="Koble til saken"
                      onClick={konverterOgKobleTilSak(alternativSak)}
                    >
                      Koble til sak
                    </Button>
                  </Table.DataCell>
                </Table.Row>
              )}
            </>
          )}
        </Table.Body>
      </Table>
    </div>
  )
}
