import { format } from 'date-fns'

export const SoeknadsdatoDate = ({ mottattDato }: { mottattDato: Date }) => {
  return (
    <div>
      <strong>Søknad mottatt: </strong>
      {format(mottattDato, 'dd.MM.yyyy')}
    </div>
  )
}
