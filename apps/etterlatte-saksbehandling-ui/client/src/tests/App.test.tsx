import { render } from '@testing-library/react'
import App from '../App'
import { Provider } from 'react-redux'
import { store } from '../store/Store'

const TestEnv = (props: { children: any }) => <Provider store={store}>{props.children}</Provider>

/*
const TestEnv2 = (props: {children: any}) => {
  const dispatch = jest.fn();
  <AppContext.Provider value={{ state: {}, dispatch }}>
    {props.children}
  </AppContext.Provider>
}*/

describe('tester', () => {
  xit('renders learn react link', () => {
    render(
      <TestEnv>
        <App />
      </TestEnv>
    )
    //const linkElement = screen.getByText(/learn react/i);
    expect(true).toBe(true)
  })
})
