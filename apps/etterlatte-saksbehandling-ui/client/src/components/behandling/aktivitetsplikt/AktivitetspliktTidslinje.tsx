import { PiggybankIcon } from '@navikt/aksel-icons'
import { Timeline } from '@navikt/ds-react'
import { hentAktiviteter } from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formattering'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears } from 'date-fns'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitet } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { NyAktivitet } from '~components/behandling/aktivitetsplikt/NyAktivitet'

export const AktivitetspliktTidslinje = (props: { behandling: IDetaljertBehandling; doedsdato: Date }) => {
  const { behandling, doedsdato } = props
  const [hentet, hent] = useApiCall(hentAktiviteter)
  const seksMndEtterDoedsfall = addMonths(doedsdato, 6)
  const tolvMndEtterDoedsfall = addMonths(doedsdato, 12)

  const [aktiviteter, setAktiviteter] = useState<IAktivitet[]>([])
  const [aktivitetsTyper, setAktivitetsTyper] = useState<AktivitetspliktType[]>([])

  useEffect(() => {
    hent({ behandlingId: behandling.id }, (aktiviteter) => {
      setAktivitetsTyper([...new Set(aktiviteter.map((a) => a.type))])
      setAktiviteter(aktiviteter)
    })
  }, [])

  return (
    <div className="min-w-[800px]" style={{ marginBottom: '50px' }}>
      <Timeline startDate={doedsdato} endDate={addYears(doedsdato, 3)}>
        <Timeline.Pin date={doedsdato}>
          <p>Dødsdato: {formaterDato(doedsdato)}</p>
        </Timeline.Pin>
        <Timeline.Pin date={new Date()}>
          <p>Dagens dato: {formaterDato(new Date())}</p>
        </Timeline.Pin>
        <Timeline.Pin date={seksMndEtterDoedsfall}>
          <p>6 måneder etter dødsfall: {formaterDato(seksMndEtterDoedsfall)}</p>
        </Timeline.Pin>
        <Timeline.Pin date={tolvMndEtterDoedsfall}>
          <p>12 måneder etter dødsfall: {formaterDato(tolvMndEtterDoedsfall)}</p>
        </Timeline.Pin>
        {aktivitetsTyper.map((aktivitetType) => (
          <Timeline.Row key={`row-${aktivitetType}`} label={typeTilTekst(aktivitetType)}>
            {aktiviteter
              .filter((aktivitet) => aktivitet.type === aktivitetType)
              .map((aktivitet, i) => (
                <Timeline.Period
                  key={aktivitetType + i}
                  start={new Date(aktivitet.fom)}
                  end={(aktivitet.tom && new Date(aktivitet.tom)) || addYears(doedsdato, 3)}
                  status="success"
                  icon={<PiggybankIcon aria-hidden />}
                  statusLabel={typeTilTekst(aktivitet.type)}
                >
                  <p>
                    <b>
                      Fra {formaterDato(new Date(aktivitet.fom))}{' '}
                      {aktivitet.tom && `til ${formaterDato(new Date(aktivitet.tom))}`}
                    </b>
                  </p>
                  <p>{aktivitet.beskrivelse}</p>
                  <i>
                    Lagt til {formaterDatoMedTidspunkt(new Date(aktivitet.opprettet.tidspunkt))} av{' '}
                    {aktivitet.opprettet.ident}
                  </i>
                  <br />
                  <i>
                    Sist endret {formaterDatoMedTidspunkt(new Date(aktivitet.endret.tidspunkt))} av{' '}
                    {aktivitet.endret.ident}
                  </i>
                </Timeline.Period>
              ))}
          </Timeline.Row>
        ))}
      </Timeline>

      <NyAktivitet behandling={behandling} oppdaterAktiviteter={setAktiviteter} />

      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av aktiviteter',
        apiResult: hentet,
      })}
    </div>
  )
}

export const typeTilTekst = (type: AktivitetspliktType) => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return 'Arbeidstaker'
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return 'Selvstendig næringsdrivende'
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return 'Etablerer virksomhet'
    case AktivitetspliktType.ARBEIDSSOEKER:
      return 'Arbeidssøker'
    case AktivitetspliktType.UTDANNING:
      return 'Utdanning'
  }
}
