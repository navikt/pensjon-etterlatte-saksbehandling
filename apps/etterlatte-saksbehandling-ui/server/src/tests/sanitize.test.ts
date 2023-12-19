import { expect } from 'chai'
import { sanitizeUrl } from '../utils/sanitize'

describe('Sanitize', () => {
  it('Sanitize url virker som forventet', () => {
    const skalIkkeEndres = [
      'localhost:8080/behandling/7af41e2d-632a-4ae2-b9ce-41c7e24fa134',
      'localhost:8080/sak/12345',
      'localhost:8080/behandling/7af41e2d-632a-4ae2-b9ce-41c7e24fa134/vilkaarsvurdering/123',
      'localhost:8080/sak/1235/brev/102/',
    ]

    skalIkkeEndres.forEach((url) => {
      expect(sanitizeUrl(url)).to.equal(url)
    })

    expect(sanitizeUrl('localhost:8080/person/25418933200')).to.equal('localhost:8080/person/254189*****')
  })
})
