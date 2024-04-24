import { PiggybankIcon } from '@navikt/aksel-icons'
import { Timeline } from '@navikt/ds-react'
import { hentAktiviteter } from '~shared/api/aktivitetsplikt'
import { formaterDato } from '~utils/formattering'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths, addYears } from 'date-fns'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { AktivitetspliktType, IAktivitet } from '~shared/types/Aktivitetsplikt'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

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
          <Timeline.Row key={`row-${aktivitetType}`} label={aktivitetType.toString()}>
            {aktiviteter
              .filter((aktivitet) => aktivitet.type === aktivitetType)
              .map((aktivitet, i) => (
                <Timeline.Period
                  key={aktivitetType + i}
                  start={aktivitet.fom}
                  end={aktivitet.tom || addYears(doedsdato, 3)}
                  status="success"
                  icon={<PiggybankIcon aria-hidden />}
                  statusLabel={aktivitet.type.toString()}
                >
                  <p>{aktivitet.beskrivelse}</p>
                </Timeline.Period>
              ))}
          </Timeline.Row>
        ))}
      </Timeline>

      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av aktiviteter',
        apiResult: hentet,
      })}
    </div>
  )
}
