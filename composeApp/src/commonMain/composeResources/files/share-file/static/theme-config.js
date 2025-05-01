// Material 3 主题配置
tailwind.config = {
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                // Material 3 调色板 - 亮色模式
                primary: {
                    DEFAULT: '#3E6700',
                    light: '#97D945',
                    dark: '#1C3200',
                    container: '#4F8200',
                    'on': '#FFFFFF',
                    'on-container': '#F9FFEA'
                },
                secondary: {
                    DEFAULT: '#4B662A',
                    light: '#B1D188',
                    dark: '#1C3200',
                    container: '#CCEEA1',
                    'on': '#FFFFFF',
                    'on-container': '#516D2F'
                },
                tertiary: {
                    DEFAULT: '#00694E',
                    container: '#008564',
                    'on': '#FFFFFF',
                    'on-container': '#F5FFF8'
                },
                error: {
                    DEFAULT: '#BA1A1A',
                    container: '#FFDAD6',
                    'on': '#FFFFFF',
                    'on-container': '#93000A'
                },
                background: {
                    DEFAULT: '#F7FBEA',
                    'on': '#191D13'
                },
                surface: {
                    DEFAULT: '#F7FBEA',
                    variant: '#DEE6CD',
                    dim: '#D8DCCC',
                    bright: '#F7FBEA',
                    'container-lowest': '#FFFFFF',
                    'container-low': '#F2F5E5',
                    container: '#ECF0DF',
                    'container-high': '#E6EADA',
                    'container-highest': '#E0E4D4',
                    'on': '#191D13',
                    'on-variant': '#424937'
                },
                outline: {
                    DEFAULT: '#727A66',
                    variant: '#C2CAB2'
                },
                // Material 3 调色板 - 暗色模式
                'dark-primary': {
                    DEFAULT: '#97D945',
                    light: '#CFFF95',
                    dark: '#64A104',
                    container: '#64A104',
                    'on': '#1F3700',
                    'on-container': '#192F00'
                },
                'dark-secondary': {
                    DEFAULT: '#B1D188',
                    container: '#365016',
                    'on': '#1F3700',
                    'on-container': '#A3C37B'
                },
                'dark-tertiary': {
                    DEFAULT: '#5DDCB0',
                    container: '#05A47C',
                    'on': '#003828',
                    'on-container': '#002F21'
                },
                'dark-error': {
                    DEFAULT: '#FFB4AB',
                    container: '#93000A',
                    'on': '#690005',
                    'on-container': '#FFDAD6'
                },
                'dark-background': {
                    DEFAULT: '#11150B',
                    'on': '#E0E4D4'
                },
                'dark-surface': {
                    DEFAULT: '#11150B',
                    variant: '#424937',
                    dim: '#11150B',
                    bright: '#363B2F',
                    'container-lowest': '#0B0F07',
                    'container-low': '#191D13',
                    container: '#1D2117',
                    'container-high': '#272B21',
                    'container-highest': '#32362B',
                    'on': '#E0E4D4',
                    'on-variant': '#C2CAB2'
                },
                'dark-outline': {
                    DEFAULT: '#8C947E',
                    variant: '#424937'
                }
            },
            borderRadius: {
                'none': '0px',
                'sm': '4px',
                DEFAULT: '8px',
                'md': '12px',
                'lg': '16px',
                'xl': '28px',
                '2xl': '32px',
                'full': '9999px'
            }
        }
    }
}