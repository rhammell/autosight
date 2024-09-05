import { extendTheme } from '@chakra-ui/react'
import '@fontsource/lato';

const customTheme = extendTheme({
  shadows: {
    outline: 'none'
  },
  fonts: {
    heading: `'Lato', sans-serif`
  },
})

export default customTheme