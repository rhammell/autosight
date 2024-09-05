import React from 'react';
import ReactDOM from 'react-dom/client';
import { ChakraProvider } from '@chakra-ui/react'
import App from './components/App';
import customTheme from './theme/customTheme'

import '@fontsource/lato/400.css'

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <ChakraProvider theme={customTheme}>
    <App />
  </ChakraProvider>
);
