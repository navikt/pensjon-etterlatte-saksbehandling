import {
  RevurderingInfo,
  RevurderinginfoMedIdOgOpprettet,
  SluttbehandlingUtlandInfo,
} from '~shared/types/RevurderingInfo'
import React, { useState } from 'react'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { formaterDato } from '~utils/formatering/dato'
import { Accordion } from '@navikt/ds-react'
import LandRad from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/LandRad'
import { ILand } from '~utils/kodeverk'

function erSluttbehandling(revurderingsinfo: RevurderingInfo): revurderingsinfo is SluttbehandlingUtlandInfo {
  return revurderingsinfo.type === Revurderingaarsak.SLUTTBEHANDLING_UTLAND
}

export default function HistoriskeSEDer({
  revurderingsinfoliste,
  landListe,
}: {
  revurderingsinfoliste: RevurderinginfoMedIdOgOpprettet[]
  landListe: ILand[]
}) {
  return (
    <Accordion>
      {revurderingsinfoliste.map((revinfo) => {
        if (erSluttbehandling(revinfo.revurderingsinfo)) {
          return (
            <Sluttbehandling
              key={revinfo.id}
              id={revinfo.id}
              opprettetDato={revinfo.opprettetDato}
              sluttbehandlingInfo={revinfo.revurderingsinfo}
              landListe={landListe}
            />
          )
        }
      })}
    </Accordion>
  )
}

function Sluttbehandling({
  sluttbehandlingInfo,
  id,
  opprettetDato,
  landListe,
}: {
  sluttbehandlingInfo: SluttbehandlingUtlandInfo
  id: string
  opprettetDato: string
  landListe: ILand[]
}) {
  const [open, setOpen] = useState<boolean>(true)
  return (
    <Accordion.Item open={open}>
      <Accordion.Header onClick={() => setOpen(() => !open)}>
        Vis sluttbehandling som ble opprettet {formaterDato(opprettetDato)} med id: {id}
      </Accordion.Header>
      {open ? (
        <Accordion.Content>
          {sluttbehandlingInfo.landMedDokumenter.map((landMedDokument, i) => {
            return (
              <div key={i} style={{ marginTop: '3rem' }}>
                <LandRad
                  landListe={landListe}
                  landMedDokumenter={landMedDokument}
                  lesevisning={true}
                  oppdaterLandMedDokumenter={() => {}}
                />
              </div>
            )
          })}
        </Accordion.Content>
      ) : null}
    </Accordion.Item>
  )
}
