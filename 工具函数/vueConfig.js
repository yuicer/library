// vue-resource
const URL = Vue.config.devtools ? '//test.mrpyq.com/api/room' : '/api/room'
Vue.http.options.root = URL
Vue.http.options.emulateJSON = true

var proxyConfig = {
  proxyTable: {
    '/api': {
      target: 'http://www.xxx.com/some/api',
      changeOrigin: true,
      pathRewrite: {
        '^/api': ''
      }
    }
  }
}
