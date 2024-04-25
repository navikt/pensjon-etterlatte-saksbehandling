import { Buildings2Icon, HatSchoolIcon, PencilIcon, PersonIcon, RulerIcon } from '@navikt/aksel-icons'
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
        {aktivitetsTyper.map((aktivitetType) => {
          const aktivitetstypeProps = mapAktivitetstypeProps(aktivitetType)

          return (
            <Timeline.Row key={`row-${aktivitetType}`} label={aktivitetstypeProps.beskrivelse}>
              {aktiviteter
                .filter((aktivitet) => aktivitet.type === aktivitetType)
                .map((aktivitet, i) => (
                  <Timeline.Period
                    key={aktivitetType + i}
                    start={new Date(aktivitet.fom)}
                    end={(aktivitet.tom && new Date(aktivitet.tom)) || addYears(doedsdato, 3)}
                    status={aktivitetstypeProps.status}
                    icon={aktivitetstypeProps.ikon}
                    statusLabel={aktivitetstypeProps.beskrivelse}
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
          )
        })}
      </Timeline>

      <NyAktivitet behandling={behandling} oppdaterAktiviteter={setAktiviteter} />

      {isFailureHandler({
        errorMessage: 'En feil oppsto ved henting av aktiviteter',
        apiResult: hentet,
      })}
    </div>
  )
}

interface AktivitetstypeProps {
  beskrivelse: string
  ikon: JSX.Element
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

export const mapAktivitetstypeProps = (type: AktivitetspliktType): AktivitetstypeProps => {
  switch (type) {
    case AktivitetspliktType.ARBEIDSTAKER:
      return {
        beskrivelse: 'Arbeidstaker',
        ikon: <PersonIcon aria-hidden />,
        status: 'success',
      }
    case AktivitetspliktType.SELVSTENDIG_NAERINGSDRIVENDE:
      return {
        beskrivelse: 'Selvstendig næringsdrivende',
        ikon: <RulerIcon aria-hidden />,
        status: 'info',
      }
    case AktivitetspliktType.ETABLERER_VIRKSOMHET:
      return {
        beskrivelse: 'Etablerer virksomhet',
        ikon: <Buildings2Icon aria-hidden />,
        status: 'danger',
      }
    case AktivitetspliktType.ARBEIDSSOEKER:
      return {
        beskrivelse: 'Arbeidssøker',
        ikon: <PencilIcon aria-hidden />,
        status: 'warning',
      }
    case AktivitetspliktType.UTDANNING:
      return {
        beskrivelse: 'Utdanning',
        ikon: <HatSchoolIcon aria-hidden />,
        status: 'neutral',
      }
  }
}
