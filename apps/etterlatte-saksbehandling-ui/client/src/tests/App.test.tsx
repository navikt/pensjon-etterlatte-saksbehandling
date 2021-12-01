import React from "react";
import { render, screen } from "@testing-library/react";
import App from "../App";
import { AppContext, ContextProvider } from "../store/AppContext";

const TestEnv = (props: {children: any}) => (
  <ContextProvider>
    {props.children}
  </ContextProvider>
);

const TestEnv2 = (props: {children: any}) => {
  const dispatch = jest.fn();
  <AppContext.Provider value={{ state: {}, dispatch }}>
    {props.children}
  </AppContext.Provider>
}

test("renders learn react link", () => {
  render(
    <TestEnv>
      <App />
    </TestEnv>
  );
  const linkElement = screen.getByText(/learn react/i);
  expect(linkElement).toBeInTheDocument();
});
